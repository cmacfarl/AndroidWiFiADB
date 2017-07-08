package org.firstinspires.ftc.plugins.androidstudio.adb;

import com.android.ddmlib.IDevice;
import org.firstinspires.ftc.plugins.androidstudio.Configuration;
import org.firstinspires.ftc.plugins.androidstudio.util.MemberwiseCloneable;
import org.jetbrains.annotations.NotNull;

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

    protected final Object                  lock = new Object();
    protected final AndroidDeviceDatabase   database;
    protected final String                  usbSerialNumber;

    /** Map of serial number -> device handle. If there's more than one, typically
     * one of them is over USB and the other is over wifi */
    protected final Map<String, AndroidDeviceHandle> handles = new HashMap<>();

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public AndroidDevice(String usbSerialNumber, AndroidDeviceDatabase deviceDatabase)
        {
        this.usbSerialNumber = usbSerialNumber;
        this.database = deviceDatabase;
        }

    // Must be idempotent
    public @NotNull AndroidDeviceHandle open(IDevice device)
        {
        synchronized (lock)
            {
            return handles.computeIfAbsent(device.getSerialNumber(),
                    (serialNumber) -> new AndroidDeviceHandle(device, this));
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
    // Accessing
    //----------------------------------------------------------------------------------------------

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

    public boolean isOpenUsingTcpip()
        {
        return predicateOverHandles(AndroidDeviceHandle::isTcpip);
        }

    public boolean isOpenWithWifiDirectGroupOwner()
        {
        return predicateOverHandles(AndroidDeviceHandle::isWifiDirectGroupOwner);
        }

    public InetAddress getWlanAddress()
        {
        return getDeviceProperty(AndroidDeviceHandle::getWlanAddress);
        }

    public boolean isAdbListeningOnTcpip()
        {
        return getDeviceProperty(AndroidDeviceHandle::isAdbListeningOnTcpip);
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

    public void ensureTcpipConnectivity()
        {
        if (!isOpen())
            return;

        if (isOpenUsingTcpip())
            {

            }
        else
            {
            // ADB doesn't already have a TCPIP connection for him. We'll try to make one if we can.
            //
            // Can we reach him over WifiDirect? If so, use that
            //
            if (!database.isWifiDirectIPAddressConnected()
                    && AndroidDeviceHandle.isPingable(Configuration.WIFI_DIRECT_GROUP_OWNER_ADDRESS)
                    && isOpenWithWifiDirectGroupOwner()
                    && adbListenOnTcpip())
                {

                }
            }
        }

    public boolean adbListenOnTcpip()
        {
        boolean result = false;
        if (isAdbListeningOnTcpip())
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
