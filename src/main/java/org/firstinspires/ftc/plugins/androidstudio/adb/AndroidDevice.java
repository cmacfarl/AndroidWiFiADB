package org.firstinspires.ftc.plugins.androidstudio.adb;

import com.android.ddmlib.IDevice;
import org.firstinspires.ftc.plugins.androidstudio.Configuration;
import org.firstinspires.ftc.plugins.androidstudio.util.EventLog;
import org.firstinspires.ftc.plugins.androidstudio.util.MemberwiseCloneable;
import org.firstinspires.ftc.plugins.androidstudio.util.Misc;
import org.firstinspires.ftc.plugins.androidstudio.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * {@link AndroidDevice} represents the consolidated knowledge we have regarding a particular
 * physical piece of hardware.
 */
@SuppressWarnings("WeakerAccess")
public class AndroidDevice extends MemberwiseCloneable<AndroidDevice>
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "AndroidDevice";

    protected final Object                  lock = new Object();
    protected final String                  usbSerialNumber;
    protected final AndroidDeviceDatabase   database;

    protected       InetAddress             inetAddressLastConnected = null;
    protected       String                  wifiDirectName = null;

    /** Map of serial number -> device handle. If there's more than one, typically
     * one of them is over USB and the other is over wifi */
    protected final Map<String, AndroidDeviceHandle> handles = new HashMap<>();


    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public AndroidDevice(AndroidDeviceDatabase deviceDatabase, String usbSerialNumber)
        {
        EventLog.ii(TAG, "create: %s", usbSerialNumber);
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
        synchronized (lock)
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
            }
        }

    public void close(AndroidDeviceHandle deviceHandle)
        {
        synchronized (lock)
            {
            handles.remove(deviceHandle.getSerialNumber());
            }
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
        synchronized (lock)
            {
            return !handles.isEmpty();
            }
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
        synchronized (lock)
            {
            for (AndroidDeviceHandle handle : handles.values())
                {
                if (predicate.test(handle))
                    {
                    return true;
                    }
                }
            }
        return false;
        }

    protected <T> T getDeviceProperty(Function<AndroidDeviceHandle, T> function)
        {
        synchronized (lock)
            {
            for (AndroidDeviceHandle handle : handles.values())
                {
                T t = function.apply(handle);
                if (t != null)
                    {
                    return t;
                    }
                }
            }
        return null;
        }

    protected <T> T anyHandle(Function<AndroidDeviceHandle, T> function)
        {
        synchronized (lock)
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
            }
        return null;
        }

    protected boolean anyHandleBool(Function<AndroidDeviceHandle, Boolean> function)
        {
        Boolean result = anyHandle(function);
        return result != null && result;
        }

    //----------------------------------------------------------------------------------------------
    // Commands
    //----------------------------------------------------------------------------------------------

    public void refreshTcpipConnectivity()
        {
        if (!isOpen())
            return;

        if (isOpenUsingTcpip())
            {
            // ADB has a TCPIP connection for this guy; we're not going to add one
            }
        else
            {
            // ADB doesn't already have a TCPIP connection for him. We'll try to make one if we can.
            //
            boolean connected = false;

            if (!connected)
                {
                // Can we reach him over WifiDirect? If so, use that
                if (!database.isWifiDirectIPAddressConnected()
                        && isPingable(Configuration.WIFI_DIRECT_GROUP_OWNER_ADDRESS)
                        && isWifiDirectGroupOwner())
                    {
                    if (listenOnTcpip() && connectAdbTcpip(Configuration.WIFI_DIRECT_GROUP_OWNER_ADDRESS))
                        {
                        connected = listenAndConnect(Configuration.WIFI_DIRECT_GROUP_OWNER_ADDRESS);
                        }
                    }
                }

            if (!connected)
                {
                // Is he on some other (infrastructure) wifi network that we can reach him through?
                InetAddress inetAddress = getWlanAddress();
                if (inetAddress != null && isPingable(inetAddress))
                    {
                    if (listenOnTcpip() && connectAdbTcpip(inetAddress))
                        {
                        connected = listenAndConnect(inetAddress);
                        }
                    }
                }

            if (!connected)
                {
                EventLog.notify(TAG, "unable to connect to %s", getUsbSerialNumber());
                }
            }
        }

    protected boolean listenAndConnect(InetAddress inetAddress)
        {
        boolean result = false;
        if (listenOnTcpip() && connectAdbTcpip(inetAddress))
            {
            result = true;
            EventLog.ii(TAG, "connected %s as %s", getUsbSerialNumber(), inetAddress);
            }
        return result;
        }

    public boolean isPingable(InetAddress inetAddress)
        {
        try {
            return inetAddress.isReachable(Configuration.msAdbTimeoutFast);
            }
        catch (IOException e)
            {
            return false;
            }
        }

    public boolean connectAdbTcpip(InetAddress inetAddress)
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
