package org.firstinspires.ftc.plugins.androidstudio.adb;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.google.gson.Gson;
import com.intellij.openapi.project.Project;
import kotlin.Pair;
import org.firstinspires.ftc.plugins.androidstudio.Configuration;
import org.firstinspires.ftc.plugins.androidstudio.adb.commands.HostAdb;
import org.firstinspires.ftc.plugins.androidstudio.util.EventLog;
import org.firstinspires.ftc.plugins.androidstudio.util.IpUtil;
import org.firstinspires.ftc.plugins.androidstudio.util.NetworkInterfaceMonitor;
import org.firstinspires.ftc.plugins.androidstudio.util.StringUtil;
import org.firstinspires.ftc.plugins.androidstudio.util.ThreadPool;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import static org.firstinspires.ftc.plugins.androidstudio.Configuration.PROP_USB_SERIAL_NUMBER;

/**
 * {@link AndroidDeviceDatabase} centralizes and persists our knowledge of various
 * Android devices
 *
 * References:
 *      https://android.googlesource.com/platform/system/core/+/master/adb/OVERVIEW.TXT
 *      https://android.googlesource.com/platform/system/core/+/master/adb/SERVICES.TXT
 */
@SuppressWarnings("WeakerAccess")
public class AndroidDeviceDatabase
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "AndroidDeviceDatabase";

    protected final HostAdb hostAdb;
    protected final AdbContext adbContext;
    protected final DeviceChangeListener deviceChangeListener = new DeviceChangeListener();
    protected final BridgeChangeListener bridgeChangeListener = new BridgeChangeListener();
    protected final NetworkInterfaceListener networkInterfaceListener = new NetworkInterfaceListener();
    protected final NetworkInterfaceMonitor networkInterfaceMonitor = new NetworkInterfaceMonitor(networkInterfaceListener);

    protected AndroidDebugBridge currentBridge;

    protected final ReentrantLock deviceLock = new ReentrantLock();
    protected final ReentrantLock pendLock = new ReentrantLock();
    protected final ArrayList<Pair<String,Runnable>> pendingOperations = new ArrayList<>();

    /** keyed by USB serial number. 'concurrent' so we can delete while iterating */
    protected final Map<String, AndroidDevice> deviceMap = new ConcurrentHashMap<>();

    /** keyed by (vanilla) serial number. 'concurrent' so we can delete while iterating */
    protected final Map<String, AndroidDeviceHandle> openedDeviceMap = new ConcurrentHashMap<>();

    String usbSerialNumberLastConnected = null;
    InetSocketAddress inetSocketAddressLastConnected = null;


    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public AndroidDeviceDatabase(Project project)
        {
        this.hostAdb = new HostAdb(project);
        this.adbContext = AdbContext.getInstance();
        this.adbContext.addDeviceChangeListener(deviceChangeListener);
        this.adbContext.addBridgeChangeListener(bridgeChangeListener);
        networkInterfaceMonitor.start();
        }

    public void dispose()
        {
        lockDevicesWhile(networkInterfaceMonitor::stop);
        }

    //----------------------------------------------------------------------------------------------
    // Loading and saving
    //----------------------------------------------------------------------------------------------

    /** A hack: not XML native, but an JSON dump living in an XML attribute, but it works. And
     * blimy if we just can't get the default serialization to work with anything complicated.
     * So ***** 'em. */
    public static class PersistentStateExternal
        {
        public String json = null;
        public PersistentStateExternal() { }
        public PersistentStateExternal(PersistentState proto)
            {
            this.json = new Gson().toJson(proto);
            }
        }

    public static class PersistentState
        {
        ArrayList<AndroidDevice.PersistentState> androidDevices = new ArrayList<>();
        String usbSerialNumberLastConnected;
        String inetSocketAddressLastConnected;

        public PersistentState() {}
        public static PersistentState from(PersistentStateExternal persistentStateExternal)
            {
            if (persistentStateExternal.json==null)
                {
                return new PersistentState();
                }
            else
                {
                return new Gson().fromJson(persistentStateExternal.json, PersistentState.class);
                }
            }
        }

    public PersistentStateExternal getPersistentState()
        {
        return lockDevicesWhile(() ->
            {
            PersistentState result = new PersistentState();
            result.inetSocketAddressLastConnected = IpUtil.toString(inetSocketAddressLastConnected);
            result.usbSerialNumberLastConnected = usbSerialNumberLastConnected;
            for (AndroidDevice androidDevice : deviceMap.values())
                {
                result.androidDevices.add(androidDevice.getPersistentState());
                }
            return new PersistentStateExternal(result);
            });
        }

    public void loadPersistentState(PersistentStateExternal persistentStateExternal)
        {
        lockDevicesWhile(() ->
            {
            try
                {
                PersistentState persistentState = PersistentState.from(persistentStateExternal);
                deviceMap.clear();
                openedDeviceMap.clear();

                inetSocketAddressLastConnected = IpUtil.parseInetSocketAddress(persistentState.inetSocketAddressLastConnected);
                usbSerialNumberLastConnected = persistentState.usbSerialNumberLastConnected;

                for (AndroidDevice.PersistentState androidDeviceData : persistentState.androidDevices)
                    {
                    deviceMap.put(androidDeviceData.usbSerialNumber, new AndroidDevice(this, androidDeviceData));
                    }
                debugDump();
                }
            catch (RuntimeException e)
                {
                EventLog.ee(TAG, e,"exception in loadPersistentState: starting afresh");
                // Tolerate errors in reifying (old format?) persistent state
                loadPersistentState(new PersistentStateExternal());
                }
            });
        }

    protected void debugDump()
        {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(byteArrayOutputStream);
        debugDump(0, printStream);
        String result = new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8);
        EventLog.dd(TAG, "state=\n%s", result);
        }

    protected void debugDump(int indent, PrintStream out)
        {
        lockDevicesWhile(() ->
            {
            StringUtil.appendLine(indent, out, "inetSocketAddressLastConnected=%s", IpUtil.toString(inetSocketAddressLastConnected));
            StringUtil.appendLine(indent, out, "usbSerialNumberLastConnected=%s", usbSerialNumberLastConnected);
            StringUtil.appendLine(indent, out, "devices:");
            for (AndroidDevice device : deviceMap.values())
                {
                device.debugDump(indent + 1, out);
                }
            });
        }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    public HostAdb getHostAdb()
        {
        return hostAdb;
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    protected void lockDevicesWhile(Runnable runnable)
        {
        lockDevicesWhile(null, runnable);
        }

    protected void lockDevicesWhile(@Nullable String function, Runnable runnable)
        {
        lockDevicesWhile(function, () ->
            {
            runnable.run();
            return null;
            });
        }

    protected <T> T lockDevicesWhile(Supplier<T> supplier)
        {
        return lockDevicesWhile(null, supplier);
        }

    protected <T> T lockDevicesWhile(@Nullable String function, Supplier<T> supplier)
        {
        if (function != null) EventLog.dd(TAG, "%s...", function);
        try {
            boolean thrown = false;

            deviceLock.lockInterruptibly();
            try {
                return supplier.get();
                }
            catch (Throwable throwable)
                {
                thrown = true;
                throw throwable;
                }
            finally
                {
                boolean pendLockTaken = false;
                try {
                    if (!thrown)
                        {
                        pendLock.lock();
                        pendLockTaken = true;
                        while (!pendingOperations.isEmpty())
                            {
                            Pair<String,Runnable> pair = pendingOperations.remove(0);
                            EventLog.dd(TAG, "running pending op: %s", pair.component1());
                            pair.component2().run();
                            }
                        }
                    }
                finally
                    {
                    deviceLock.unlock();
                    if (pendLockTaken) pendLock.unlock();
                    }
                }
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            throw new RuntimeException("interruption");
            }
        finally
            {
            if (function != null) EventLog.dd(TAG, "...%s", function);
            }
        }

    protected void lockAndRunOrPend(String tag, Runnable runnable)
        {
        pendLock.lock();
        boolean pendLockTaken = true;
        try {
            if (deviceLock.tryLock())
                {
                pendLock.unlock();
                pendLockTaken = false;
                try
                    {
                    runnable.run();
                    }
                finally
                    {
                    deviceLock.unlock();
                    }
                }
            else
                {
                EventLog.dd(TAG, "pending op: %s", tag);
                pendingOperations.add(new Pair<>(tag, runnable));
                }
            }
        finally
            {
            if (pendLockTaken) pendLock.unlock();;
            }
        }


    /** Must be idempotent */
    public AndroidDeviceHandle open(IDevice device)
        {
        AndroidDeviceHandle result = lockDevicesWhile(() ->
            {
            AndroidDevice androidDevice = deviceMap.computeIfAbsent(getUsbSerialNumber(device),
                    (usbSerialNumber) -> new AndroidDevice(AndroidDeviceDatabase.this, usbSerialNumber));
            AndroidDeviceHandle handle = androidDevice.open(device);
            openedDeviceMap.put(device.getSerialNumber(), handle);
            return handle;
            });

        result.getAndroidDevice().refreshTcpipConnectivity();

        return result;
        }

    public void close(IDevice device)
        {
        lockDevicesWhile(() ->
            {
            AndroidDeviceHandle handle = openedDeviceMap.get(device.getSerialNumber());
            if (handle != null)
                {
                EventLog.dd(TAG, "closing(%s)", device.getSerialNumber());
                handle.close();
                openedDeviceMap.remove(device.getSerialNumber());
                }
            });
        }

    protected void closeAll()
        {
        EventLog.dd(TAG, "closeAll()");
        lockDevicesWhile(() ->
            {
            for (AndroidDeviceHandle handle : openedDeviceMap.values())
                {
                close(handle.getDevice());
                }
            });
        }

    //----------------------------------------------------------------------------------------------
    // TCPIP management
    //----------------------------------------------------------------------------------------------

    protected void refreshTcpipConnectivity()
        {
        Collection<AndroidDevice> devices = lockDevicesWhile(deviceMap::values);
        for (AndroidDevice androidDevice : devices)
            {
            androidDevice.refreshTcpipConnectivity();
            }
        }

    protected void reconnectLastTcpipConnected()
        {
        InetSocketAddress inetSocketAddress = this.inetSocketAddressLastConnected;
        EventLog.dd(TAG, "reconnectLastTcpipConnected() addr=%s", inetSocketAddress);
        if (inetSocketAddress != null)
            {
            // 'connect' can take very long time, so use a worker
            ThreadPool.getDefault().execute(() ->
                {
                // Attempt to connect to this most recent fellow. Now, if we successfully connect,
                // then it *may* be the case that it's not the same guy if the IP address in question
                // was somehow reassigned (which can only happen on an infrastructure network).
                //
                // That might be unexpected, but is probably benign. We ignore for now
                //
                getHostAdb().connect(inetSocketAddress);
                });
            }
        }

    public void noteDeviceConnectedTcpip(AndroidDevice androidDevice, InetSocketAddress inetSocketAddress)
        {
        lockDevicesWhile(() ->
            {
            usbSerialNumberLastConnected = androidDevice.getUsbSerialNumber();
            inetSocketAddressLastConnected = inetSocketAddress;
            });
        }

    //----------------------------------------------------------------------------------------------
    // Utility
    //----------------------------------------------------------------------------------------------

    /** Do we have a currently-connected device that lives at the Wifi-Direct group owner address? */
    public boolean isWifiDirectIPAddressConnected()
        {
        return lockDevicesWhile(() -> {
            for (AndroidDeviceHandle handle : openedDeviceMap.values())
                {
                InetSocketAddress inetSocketAddress = handle.getInetSocketAddress();
                if (inetSocketAddress!=null && inetSocketAddress.getAddress().equals(Configuration.WIFI_DIRECT_GROUP_OWNER_ADDRESS))
                    {
                    return true;
                    }
                }
            return false;
            });
        }

    protected String getUsbSerialNumber(IDevice device)
        {
        try {
            return device.getSystemProperty(PROP_USB_SERIAL_NUMBER).get(Configuration.msAdbTimeoutFast, TimeUnit.MILLISECONDS);
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            throw new RuntimeException("interrupt retrieving serial number", e);
            }
        catch (ExecutionException e)
            {
            throw new RuntimeException("exception retrieving serial number", e);
            }
        catch (TimeoutException e)
            {
            throw new RuntimeException("timeout retrieving serial number", e);
            }
        }

    //----------------------------------------------------------------------------------------------
    // Notification
    //----------------------------------------------------------------------------------------------

    protected class NetworkInterfaceListener implements NetworkInterfaceMonitor.Callback
        {
        @Override public void onNetworkInterfacesChanged()
            {
            EventLog.dd(TAG, "network interface list changed: refreshing tcpip connectivity");
            refreshTcpipConnectivity();
            }
        }

    protected class BridgeChangeListener implements AndroidDebugBridge.IDebugBridgeChangeListener
        {
        /** We get called both for creations and disconnects. The situations in which bridge
         * can be null aren't well understood.  */
        @Override public void bridgeChanged(@Nullable AndroidDebugBridge bridge)
            {
            synchronized (deviceLock)
                {
                AndroidDebugBridge oldBridge = currentBridge;
                currentBridge = bridge;
                if (currentBridge != oldBridge)
                    {
                    if (oldBridge != null)
                        {
                        // This may not be necessary: should we see device notifications for these
                        // before we get here?
                        closeAll();
                        }
                    if (currentBridge != null)
                        {
                        reconnectLastTcpipConnected();
                        }
                    }
                }
            }
        }

    protected class DeviceChangeListener implements AndroidDebugBridge.IDeviceChangeListener
        {
        @Override
        public void deviceConnected(IDevice device)
            {
            if (device.isOnline())
                {
                openOrPend(device);
                }
            }

        @Override
        public void deviceDisconnected(IDevice device)
            {
            closeOrPend(device);
            }

        @Override
        public void deviceChanged(IDevice device, int changeMask)
            {
            if (device.isOnline())
                {
                openOrPend(device);
                }
            else
                {
                closeOrPend(device);
                }
            }
        }

    // Be smart to avoid deadlocks
    protected void openOrPend(IDevice device)
        {
        lockAndRunOrPend(String.format(Locale.ROOT,"open(%s)", device.getSerialNumber()), () -> open(device));
        }

    // Be smart to avoid deadlocks
    protected void closeOrPend(IDevice device)
        {
        lockAndRunOrPend(String.format(Locale.ROOT,"close(%s)", device.getSerialNumber()), () -> close(device));
        }
    }
