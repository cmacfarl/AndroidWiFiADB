package org.firstinspires.ftc.plugins.androidstudio.adb;

import com.android.ddmlib.IDevice;
import org.firstinspires.ftc.plugins.androidstudio.Configuration;
import org.firstinspires.ftc.plugins.androidstudio.util.EventLog;
import org.firstinspires.ftc.plugins.androidstudio.util.ReentrantLockOwner;
import org.firstinspires.ftc.plugins.androidstudio.util.Misc;
import org.firstinspires.ftc.plugins.androidstudio.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetAddress;
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
    protected       InetAddress                         inetAddressLastConnected = null;
    protected       String                              wifiDirectName = null;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public AndroidDevice(AndroidDeviceDatabase deviceDatabase, String usbSerialNumber)
        {
        EventLog.dd(TAG, "create: %s", usbSerialNumber);
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

            // Remember the latest name for this fellow that we have
            String wifiDirectName = result.getWifiDirectName();
            if (wifiDirectName != null)
                {
                AndroidDevice.this.wifiDirectName = wifiDirectName;
                }

            return result;
            });
        }

    public void close(AndroidDeviceHandle deviceHandle)
        {
        lockWhile(() -> handles.remove(deviceHandle.getSerialNumber()));
        }

    //----------------------------------------------------------------------------------------------
    // Loading and saving
    //----------------------------------------------------------------------------------------------

    public static class PersistentState
        {
        String      usbSerialNumber = "";
        String      wifiDirectName = "";
        String      inetAddressLastConnected = "";

        public PersistentState()
            {
            }
        public PersistentState(AndroidDevice androidDevice)
            {
            this();
            this.usbSerialNumber = androidDevice.usbSerialNumber;
            this.wifiDirectName = androidDevice.wifiDirectName != null ? androidDevice.wifiDirectName : "";
            if (androidDevice.inetAddressLastConnected != null)
                {
                this.inetAddressLastConnected = androidDevice.inetAddressLastConnected.toString();
                }
            }

        public InetAddress getInetAddressLastConnected()
            {
            return Misc.parseInetAddress(inetAddressLastConnected);
            }

        }

    public PersistentState getPersistentState()
        {
        return new PersistentState(this);
        }

    public void loadPersistentState(PersistentState persistentState)
        {
        assert this.usbSerialNumber.equals(persistentState.usbSerialNumber);
        this.inetAddressLastConnected = StringUtils.isNullOrEmpty(persistentState.inetAddressLastConnected)
                ? null
                : Misc.parseInetAddress(persistentState.inetAddressLastConnected);
        this.wifiDirectName = StringUtils.isNullOrEmpty(persistentState.wifiDirectName)
                ? null
                : persistentState.wifiDirectName;
        }

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

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

    //----------------------------------------------------------------------------------------------
    // Mapping over handles
    //----------------------------------------------------------------------------------------------

    protected boolean predicateOverHandles(Predicate<AndroidDeviceHandle> predicate)
        {
        return lockWhile("predicateOverHandles",() ->
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
        return lockWhile("getDeviceProperty", () ->
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
        return lockWhile("anyHandle", () ->
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
        boolean needToConnect = lockWhile("refresh1", () -> isOpen() && !isOpenUsingTcpip());
        if (needToConnect)
            {
            // ADB doesn't already have a TCPIP connection for him. We'll try to make one if we can.
            //
            boolean connected = false;

            if (!connected)
                {
                // Can we reach him over WifiDirect? If so, use that
                EventLog.dd(TAG, "maybe wifi direct");
                boolean tryWifiDirect = lockWhile("refresh2", () ->
                    !database.isWifiDirectIPAddressConnected()
                        && isPingable(Configuration.WIFI_DIRECT_GROUP_OWNER_ADDRESS)
                        && isWifiDirectGroupOwner()
                    );

                if (tryWifiDirect)
                    {
                    EventLog.dd(TAG, "trying wifi direct");
                    connected = listenAndConnect(Configuration.WIFI_DIRECT_GROUP_OWNER_ADDRESS);
                    }
                }

            if (!connected)
                {
                EventLog.dd(TAG, "maybe wlan");
                // Is he on some other (infrastructure) wifi network that we can reach him through?
                InetAddress inetAddress = getWlanAddress();
                if (inetAddress != null && isPingable(inetAddress))
                    {
                    EventLog.dd(TAG, "trying wlan %s", inetAddress);
                    connected = listenAndConnect(inetAddress);
                    }
                }

            if (!connected)
                {
                EventLog.notify(TAG, "unable to tcpip-connect to %s", getUsbSerialNumber());
                }
            }
        }

    protected boolean listenAndConnect(InetAddress inetAddress)
        {
        return trace("listenAndConnect", () ->
            {
            boolean result = false;
            if (listenOnTcpip() && adbConnect(inetAddress))
                {
                result = true;
                EventLog.dd(TAG, "tcpip-connected to %s at %s", getUsbSerialNumber(), inetAddress);
                }
            return result;
            });
        }

    public boolean isPingable(InetAddress inetAddress)
        {
        return trace("isPingable", () ->
            {
            try {
                return inetAddress.isReachable(Configuration.msAdbTimeoutFast);
                }
            catch (IOException|RuntimeException e)
                {
                return false;
                }
            });
        }

    public boolean adbConnect(InetAddress inetAddress)
        {
        boolean result = database.getHostAdb().connect(inetAddress);
        if (result)
            {
            inetAddressLastConnected = inetAddress;
            database.noteDeviceConnectedTcpip(this, inetAddress);
            }
        return result;
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
