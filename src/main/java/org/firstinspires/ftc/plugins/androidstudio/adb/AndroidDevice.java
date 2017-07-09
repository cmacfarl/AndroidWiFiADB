package org.firstinspires.ftc.plugins.androidstudio.adb;

import com.android.ddmlib.IDevice;
import org.firstinspires.ftc.plugins.androidstudio.Configuration;
import org.firstinspires.ftc.plugins.androidstudio.util.EventLog;
import org.firstinspires.ftc.plugins.androidstudio.util.ReentrantLockOwner;
import org.firstinspires.ftc.plugins.androidstudio.util.IpUtil;
import org.firstinspires.ftc.plugins.androidstudio.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * {@link AndroidDevice} represents the consolidated knowledge we have regarding a particular
 * physical piece of hardware.
 */
@SuppressWarnings("WeakerAccess")
public class AndroidDevice extends ReentrantLockOwner
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "AndroidDevice";

    protected final String usbSerialNumber;
    protected final AndroidDeviceDatabase database;

    /** Map of serial number -> device handle. If there's more than one, typically
     * one of them is over USB and the other is over wifi */
    protected final Map<String, AndroidDeviceHandle>    handles = new ConcurrentHashMap<>();
    protected       InetSocketAddress                   inetSocketAddressLastConnected = null;
    protected       String                              wifiDirectName = null;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public AndroidDevice(AndroidDeviceDatabase deviceDatabase, String usbSerialNumber)
        {
        EventLog.dd(TAG, "create(usb=%s)", usbSerialNumber);
        this.database = deviceDatabase;
        this.usbSerialNumber = usbSerialNumber;
        }

    public AndroidDevice(AndroidDeviceDatabase database, PersistentState persistentState)
        {
        this(database, persistentState.usbSerialNumber);
        loadPersistentState(persistentState);
        }

    // Must be idempotent
    public @NotNull AndroidDeviceHandle open(IDevice device)
        {
        return lockWhile(() ->
            {
            AndroidDeviceHandle result = handles.computeIfAbsent(device.getSerialNumber(),
                    (serialNumber) -> new AndroidDeviceHandle(device, this));

            // Remember the latest name for this fellow
            AndroidDevice.this.updateWifiDirectName(result.getWifiDirectName());

            // If he's wireless (or at least non-usb) then remember that
            if (result.isTcpip())
                {
                inetSocketAddressLastConnected = IpUtil.parseInetSocketAddress(result.getSerialNumber());
                database.noteDeviceConnectedTcpip(this, inetSocketAddressLastConnected);
                }

            return result;
            });
        }

    public void close(AndroidDeviceHandle deviceHandle)
        {
        lockWhile(() -> handles.remove(deviceHandle.getSerialNumber()));
        }

    public void debugDump(int indent, PrintStream out)
        {
        lockWhile(() ->
            {
            StringUtil.appendLine(indent, out, "device=%s inetSocketAddressLastConnected=%s", getDebugDisplayName(), IpUtil.toString(inetSocketAddressLastConnected));
            for (AndroidDeviceHandle handle : handles.values())
                {
                handle.debugDump(indent + 1, out);
                }
            });
        }

    //----------------------------------------------------------------------------------------------
    // Loading and saving
    //----------------------------------------------------------------------------------------------

    public static class PersistentState
        {
        String usbSerialNumber;
        String wifiDirectName;
        String inetSocketAddressLastConnected;

        public PersistentState()
            {
            }
        public PersistentState(AndroidDevice androidDevice)
            {
            this();
            this.usbSerialNumber = androidDevice.usbSerialNumber;
            this.wifiDirectName = androidDevice.wifiDirectName;
            this.inetSocketAddressLastConnected = IpUtil.toString(androidDevice.inetSocketAddressLastConnected);
            }
        }

    public PersistentState getPersistentState()
        {
        return new PersistentState(this);
        }

    public void loadPersistentState(PersistentState persistentState)
        {
        assert this.usbSerialNumber.equals(persistentState.usbSerialNumber);
        this.inetSocketAddressLastConnected = IpUtil.parseInetSocketAddress(persistentState.inetSocketAddressLastConnected);
        updateWifiDirectName(persistentState.wifiDirectName);
        }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    public List<AndroidDeviceHandle> getOpenHandles()
        {
        return lockWhile(() -> new ArrayList<>(handles.values()));
        }

    public String getUsbSerialNumber()
        {
        return usbSerialNumber;
        }

    public AndroidDeviceDatabase getDatabase()
        {
        return database;
        }

    public boolean isOpen()
        {
        return lockWhile(() -> !handles.isEmpty());
        }

    /** Is at least one of our handles already connected using TCPIP? */
    public boolean isOpenUsingTcpip()
        {
        return predicateOverHandles(AndroidDeviceHandle::isTcpip);
        }

    /** Is the device the owner of a wifi direct group? */
    public boolean isWifiDirectGroupOwner()
        {
        return anyHandleBool(AndroidDeviceHandle::isWifiDirectGroupOwner);
        }

    public InetAddress getWlanAddress()
        {
        return getDeviceProperty(AndroidDeviceHandle::getWlanAddress);
        }

    public boolean isListeningOnTcpip()
        {
        return getDeviceProperty(AndroidDeviceHandle::isListeningOnTcpip);
        }

    public String getDisplayName()
        {
        String result = wifiDirectName;
        if (result == null)
            {
            result = usbSerialNumber;
            }
        return result;
        }

    public String getDebugDisplayName()
        {
        return wifiDirectName==null
                ? usbSerialNumber
                : String.format(Locale.ROOT, "%s(%s)", wifiDirectName, usbSerialNumber);
        }

    public void updateWifiDirectName(@Nullable String wifiDirectName)
        {
        if (StringUtil.notNullOrEmpty(wifiDirectName))
            {
            this.wifiDirectName = wifiDirectName;
            }
        }

    //----------------------------------------------------------------------------------------------
    // Mapping over handles
    //----------------------------------------------------------------------------------------------

    protected boolean predicateOverHandles(Predicate<AndroidDeviceHandle> predicate)
        {
        return lockWhile(() ->
            {
            for (AndroidDeviceHandle handle : handles.values())
                {
                if (predicate.test(handle))
                    {
                    return true;
                    }
                }
            return false;
            });
        }

    protected <T> T getDeviceProperty(Function<AndroidDeviceHandle, T> function)
        {
        return lockWhile(() ->
            {
            for (AndroidDeviceHandle handle : handles.values())
                {
                T t = function.apply(handle);
                if (t != null)
                    {
                    return t;
                    }
                }
            return null;
            });
        }

    protected <T> T anyHandle(Function<AndroidDeviceHandle, T> function)
        {
        return lockWhile(() ->
            {
            // For robustness: try USB handles first
            for (AndroidDeviceHandle handle : handles.values())
                {
                if (handle.isUSB())
                    {
                    return function.apply(handle);
                    }
                }
            for (AndroidDeviceHandle handle : handles.values())
                {
                return function.apply(handle);
                }
            return null;
            });
        }

    protected boolean anyHandleBool(Function<AndroidDeviceHandle, Boolean> function)
        {
        Boolean result = anyHandle(function);
        return result != null && result;
        }

    //----------------------------------------------------------------------------------------------
    // Commands
    //----------------------------------------------------------------------------------------------

    /** Called with the database handles lock NOT held. */
    public void refreshTcpipConnectivity()
        {
        boolean needToConnect = lockWhile(() -> isOpen() && !isOpenUsingTcpip());
        if (needToConnect)
            {
            // ADB doesn't already have a TCPIP connection for him. We'll try to make one if we can.
            //
            boolean connected = false;

            if (!connected)
                {
                // Can we reach him over WifiDirect? If so, use that
                boolean tryWifiDirect = lockWhile(() ->
                    !database.isWifiDirectIPAddressConnected()
                        && IpUtil.isPingable(Configuration.WIFI_DIRECT_GROUP_OWNER_ADDRESS)
                        && isWifiDirectGroupOwner()
                    );

                if (tryWifiDirect)
                    {
                    connected = listenAndConnect(Configuration.WIFI_DIRECT_GROUP_OWNER_ADDRESS, Configuration.ADB_DAEMON_PORT);
                    }
                }

            if (!connected)
                {
                // Is he on some other (infrastructure) wifi network that we can reach him through?
                InetAddress inetAddress = getWlanAddress();
                if (inetAddress != null && IpUtil.isPingable(inetAddress))
                    {
                    connected = listenAndConnect(inetAddress, Configuration.ADB_DAEMON_PORT);
                    }
                }

            if (!connected)
                {
                EventLog.notify(TAG, "unable to tcpip-connect to %s", getDebugDisplayName());
                }
            }
        }

    protected boolean listenAndConnect(InetAddress inetAddress, int port)
        {
        boolean result = false;
        InetSocketAddress inetSocketAddress = new InetSocketAddress(inetAddress, port);
        if (listenOnTcpip() && adbConnect(inetSocketAddress))
            {
            result = true;
            EventLog.dd(this, "tcpip-connected to %s at %s", getDebugDisplayName(), IpUtil.toString(inetSocketAddress));
            }
        return result;
    }

    public boolean adbConnect(InetSocketAddress inetSocketAddress)
        {
        return database.getHostAdb().connect(inetSocketAddress);
        }

    public boolean listenOnTcpip()
        {
        boolean result = false;
        if (isListeningOnTcpip())
            {
            result = true;
            }
        else
            {
            boolean listenRequested = anyHandleBool(AndroidDeviceHandle::listenOnTcpip);
            if (listenRequested)
                {
                boolean isListening = anyHandleBool((handle) ->
                        handle.awaitListeningOnTcpip(Configuration.msAdbTimeoutSlow, TimeUnit.MILLISECONDS)
                    );
                if (isListening)
                    {
                    result = true;
                    }
                }
            }
        return result;
        }

    }
