package org.firstinspires.ftc.plugins.androidstudio.adb;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.google.gson.Gson;
import com.intellij.openapi.project.Project;
import org.firstinspires.ftc.plugins.androidstudio.Configuration;
import org.firstinspires.ftc.plugins.androidstudio.adb.commands.HostAdb;
import org.firstinspires.ftc.plugins.androidstudio.util.EventLog;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    protected final ReentrantLock lock = new ReentrantLock();
    protected final HostAdb hostAdb;
    protected final AdbContext adbContext;
    protected final DeviceChangeListener deviceChangeListener = new DeviceChangeListener();
    protected final BridgeChangeListener bridgeChangeListener = new BridgeChangeListener();

    protected AndroidDebugBridge currentBridge;

    /** keyed by USB serial number */
    protected final Map<String, AndroidDevice> deviceMap = new HashMap<>();

    /** keyed by (vanilla) serial number */
    protected final Map<String, AndroidDeviceHandle> openedDeviceMap = new HashMap<>();

    protected final ReentrantLock controlLock = new ReentrantLock();
    protected final ArrayList<Runnable> pendingOperations = new ArrayList<>();

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public AndroidDeviceDatabase(Project project)
        {
        this.hostAdb = new HostAdb(project);
        this.adbContext = AdbContext.getInstance();
        this.adbContext.addDeviceChangeListener(deviceChangeListener);
        this.adbContext.addBridgeChangeListener(bridgeChangeListener);
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
        return lockWhile(() ->
            {
            PersistentState result = new PersistentState();
            for (AndroidDevice androidDevice : deviceMap.values())
                {
                result.androidDevices.add(androidDevice.getPersistentState());
                }
            EventLog.ii(TAG,"getPersistentState() count=%d", result.androidDevices.size());
            return new PersistentStateExternal(result);
            });
        }

    public void loadPersistentState(PersistentStateExternal persistentStateExternal)
        {
        PersistentState persistentState = PersistentState.from(persistentStateExternal);
        EventLog.ii(TAG,"loadPersistentState() count=%d", persistentState.androidDevices.size());
        lockWhile(() ->
            {
            assert deviceMap.isEmpty();
            assert openedDeviceMap.isEmpty();

            for (AndroidDevice.PersistentState androidDeviceData : persistentState.androidDevices)
                {
                deviceMap.put(androidDeviceData.usbSerialNumber, new AndroidDevice(this, androidDeviceData));
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

    protected void lockWhile(Runnable runnable)
        {
        lockWhile(() ->
            {
            runnable.run();
            return null;
            });
        }

    protected <T> T lockWhile(Supplier<T> supplier)
        {
        try {
            boolean thrown = false;

            lock.lockInterruptibly();
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
                boolean controlLockTaken = false;
                try {
                    if (!thrown)
                        {
                        controlLock.lock();
                        controlLockTaken = true;
                        while (!pendingOperations.isEmpty())
                            {
                            pendingOperations.remove(0).run();
                            }
                        }
                    }
                finally
                    {
                    lock.unlock();
                    if (controlLockTaken) controlLock.unlock();
                    }
                }
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            throw new RuntimeException("interruption");
            }
        }

    protected void lockAndRunOrPend(Runnable runnable)
        {
        controlLock.lock();
        if (lock.tryLock())
            {
            controlLock.unlock();
            try {
                runnable.run();
                }
            finally
                {
                lock.unlock();
                }
            }
        else
            {
            pendingOperations.add(runnable);
            controlLock.unlock();
            }
        }


    /** Must be idempotent */
    public AndroidDeviceHandle open(IDevice device)
        {
        return lockWhile(() ->
            {
            AndroidDevice androidDevice = deviceMap.computeIfAbsent(getUsbSerialNumber(device),
                    (usbSerialNumber) -> new AndroidDevice(AndroidDeviceDatabase.this, usbSerialNumber));
            AndroidDeviceHandle handle = androidDevice.open(device);
            openedDeviceMap.put(device.getSerialNumber(), handle);
            return handle;
            });
        }

    public void close(IDevice device)
        {
        lockWhile(() ->
            {
            AndroidDeviceHandle handle = openedDeviceMap.get(device.getSerialNumber());
            if (handle != null)
                {
                handle.close();
                openedDeviceMap.remove(device.getSerialNumber());
                }
            return null;
            });
        }

    public void refreshTcpipConnectivity()
        {
        List<AndroidDevice> androidDevices = new ArrayList<>();
        lockWhile(() ->
            {
            androidDevices.addAll(deviceMap.values());
            });

        for (AndroidDevice androidDevice : androidDevices)
            {
            lockWhile(androidDevice::refreshTcpipConnectivity);
            }
        }

    public void noteDeviceConnectedTcpip(AndroidDevice androidDevice, InetAddress inetAddress)
        {
        noteDeviceConnectedTcpip(androidDevice, new InetSocketAddress(inetAddress, Configuration.ADB_DAEMON_PORT));
        }
    public void noteDeviceConnectedTcpip(AndroidDevice androidDevice, InetSocketAddress inetSocketAddress)
        {
        // TODO
        }

    //----------------------------------------------------------------------------------------------
    // Utility
    //----------------------------------------------------------------------------------------------

    public boolean isWifiDirectIPAddressConnected()
        {
        return lockWhile(() -> {
            for (AndroidDeviceHandle handle : openedDeviceMap.values())
                {
                InetAddress inetAddress = handle.getInetAddress();
                if (inetAddress!=null && inetAddress.equals(Configuration.WIFI_DIRECT_GROUP_OWNER_ADDRESS))
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

    protected class BridgeChangeListener implements AndroidDebugBridge.IDebugBridgeChangeListener
        {
        /** We get called both for creations and disconnects */
        @Override public void bridgeChanged(AndroidDebugBridge bridge)
            {
            synchronized (lock)
                {
                AndroidDebugBridge oldBridge = currentBridge;
                currentBridge = bridge;
                if (currentBridge != oldBridge)
                    {
                    // ....
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
        lockAndRunOrPend(() -> open(device));
        }

    // Be smart to avoid deadlocks
    protected void closeOrPend(IDevice device)
        {
        lockAndRunOrPend(() -> close(device));
        }
    }
