package org.firstinspires.ftc.plugins.androidstudio.adb;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.intellij.openapi.project.Project;
import org.firstinspires.ftc.plugins.androidstudio.Configuration;
import org.firstinspires.ftc.plugins.androidstudio.adb.commands.HostAdb;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
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

    protected final ReentrantLock lock = new ReentrantLock();
    protected final HostAdb hostAdb;
    protected final AdbContext adbContext;
    protected final DeviceChangeListener deviceChangeListener = new DeviceChangeListener();

    /** keyed by USB serial number */
    protected final Map<String, AndroidDevice> deviceMap = new HashMap<>();
    /** keyed by (vanilla) serial number */
    protected final Map<String, AndroidDeviceHandle> openedDeviceMap = new HashMap<>();

    protected final ReentrantLock controlLock = new ReentrantLock();
    protected final ArrayList<Runnable> pendingOperations = new ArrayList<>();

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public AndroidDeviceDatabase(Project project, AdbContext adbContext)
        {
        this.hostAdb = new HostAdb(project);
        this.adbContext = adbContext;
        this.adbContext.addDeviceChangeListener(deviceChangeListener);
        }

    //----------------------------------------------------------------------------------------------
    // Operations
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
                    (usbSerialNumber) -> new AndroidDevice(usbSerialNumber, AndroidDeviceDatabase.this));
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

    public void ensureTcpipConnectivity()
        {
        lockWhile(() ->
            {
            for (AndroidDevice androidDevice : deviceMap.values())
                {
                androidDevice.ensureTcpipConnectivity();
                }
            });
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
