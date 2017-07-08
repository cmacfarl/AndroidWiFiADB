package org.firstinspires.ftc.plugins.androidstudio.adb;

import com.android.ddmlib.IDevice;
import org.firstinspires.ftc.plugins.androidstudio.Configuration;
import org.firstinspires.ftc.plugins.androidstudio.adb.commands.GetSettingCommand;
import org.firstinspires.ftc.plugins.androidstudio.adb.commands.IfConfigCommand;
import org.firstinspires.ftc.plugins.androidstudio.util.Misc;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

/**
 * {@link AndroidDeviceHandle} represents a live connection to an {@link AndroidDevice}
 */
@SuppressWarnings("WeakerAccess")
public class AndroidDeviceHandle
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "AndroidDeviceHandle";

    public static final Pattern patternIpAddress        = Pattern.compile("[0-9]{1,3}\\.*[0-9]{1,3}\\.*[0-9]{1,3}\\.*[0-9]{1,3}");
    public static final Pattern patternIpAddressAndPort = Pattern.compile("[0-9]{1,3}\\.*[0-9]{1,3}\\.*[0-9]{1,3}\\.*[0-9]{1,3}:[0-9]{1,5}");

    protected final IDevice device;
    protected final AndroidDevice androidDevice;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public AndroidDeviceHandle(IDevice device, AndroidDevice androidDevice)
        {
        this.device = device;
        this.androidDevice = androidDevice;
        }

    public void close()
        {
        androidDevice.close(this);
        }

    public AndroidDevice getAndroidDevice()
        {
        return androidDevice;
        }

    //----------------------------------------------------------------------------------------------
    // High level accessing
    //----------------------------------------------------------------------------------------------

    /*
     * wlan0: Is the device on an infrastructure network?
     *
     * Example connected state:
     *  adb shell getprop | grep -y wlan0
     *      [dhcp.wlan0.dns1]: [75.75.75.75]
     *      [dhcp.wlan0.dns2]: [75.75.76.76]
     *      [dhcp.wlan0.dns3]: []
     *      [dhcp.wlan0.dns4]: []
     *      [dhcp.wlan0.domain]: []
     *      [dhcp.wlan0.gateway]: [192.168.0.1]
     *      [dhcp.wlan0.ipaddress]: [192.168.0.20]
     *      [dhcp.wlan0.leasetime]: [3600]
     *      [dhcp.wlan0.mask]: [255.255.255.0]
     *      [dhcp.wlan0.mtu]: []
     *      [dhcp.wlan0.pid]: [15503]
     *      [dhcp.wlan0.reason]: [BOUND]
     *      [dhcp.wlan0.result]: [ok]
     *      [dhcp.wlan0.server]: [192.168.0.1]
     *      [dhcp.wlan0.vendorInfo]: []
     *      [init.svc.dhcpcd_wlan0]: [running]
     *      [wifi.interface]: [wlan0]
     *
     * Example connected, then disconnected state:
     *      [dhcp.wlan0.dns1]: [75.75.75.75]
     *      [dhcp.wlan0.dns2]: [75.75.76.76]
     *      [dhcp.wlan0.dns3]: []
     *      [dhcp.wlan0.dns4]: []
     *      [dhcp.wlan0.domain]: []
     *      [dhcp.wlan0.gateway]: [192.168.0.1]
     *      [dhcp.wlan0.ipaddress]: [192.168.0.20]
     *      [dhcp.wlan0.leasetime]: [3600]
     *      [dhcp.wlan0.mask]: [255.255.255.0]
     *      [dhcp.wlan0.mtu]: []
     *      [dhcp.wlan0.pid]: [15503]
     *      [dhcp.wlan0.reason]: [BOUND]
     *      [dhcp.wlan0.result]: [failed]
     *      [dhcp.wlan0.server]: [192.168.0.1]
     *      [dhcp.wlan0.vendorInfo]: []
     *      [init.svc.dhcpcd_wlan0]: [stopped]
     *      [wifi.interface]: [wlan0]
     *
     * Example boot, non-connected state:
     *      [wifi.interface]: [wlan0]
     */

    /** Returns whether this device is currently connected to an infrastructure IP network */
    public boolean isWlanRunning()
        {
        return getStringProperty(Configuration.PROP_WLAN_STATUS, "stopped").equals("running");
        }

    /** If this device is currently or was recently connected to an infrastructure
     * IP network, then return the address used on same*/
    public @Nullable InetAddress getWlanAddress()
        {
        try {
            String value = getStringProperty(Configuration.PROP_WLAN_IP_ADDRESS, null);
            return value == null ? null : InetAddress.getByName(value);
            }
        catch (UnknownHostException e)
            {
            return null;
            }
        }

    @SuppressWarnings("ConstantConditions")
    public @NotNull String getUsbSerialNumber()
        {
        return getStringProperty(Configuration.PROP_USB_SERIAL_NUMBER);
        }

    public @NotNull String getSerialNumber()
        {
        return device.getSerialNumber();
        }

    public boolean isEmulator()
        {
        return device.isEmulator();
        }
    public boolean isTcpip()
        {
        return patternIpAddressAndPort.matcher(getSerialNumber()).matches();
        }
    public boolean isUSB()
        {
        return !isTcpip() && !isEmulator();
        }

    public @Nullable InetAddress getInetAddress()
        {
        if (isTcpip())
            {
            return Misc.ipAddress(getSerialNumber());
            }
        return null;
        }

    public boolean isWifiDirectGroupOwner()
        {
        IfConfigCommand command = new IfConfigCommand();
        if (command.execute(device, "p2p0"))
            {
            return command.getInetAddress().equals(Configuration.WIFI_DIRECT_GROUP_OWNER_ADDRESS) && command.isUp();
            }
        throw new RuntimeException("command failed");
        }

    public String getWifiDirectName()
        {
        GetSettingCommand command = new GetSettingCommand(GetSettingCommand.Namespace.GLOBAL, Configuration.SETTING_WIFI_P2P_DEVICE_NAME);
        if (command.execute(device))
            {
            return command.getResult();
            }
        throw new RuntimeException("command failed");
        }

    public String getUserIdentifier()
        {
        String result = getWifiDirectName();
        if (result==null) result = getUsbSerialNumber();
        return result;
        }

    public static boolean isPingable(InetAddress inetAddress)
        {
        try {
            return inetAddress.isReachable(Configuration.msAdbTimeoutFast);
            }
        catch (IOException e)
            {
            return false;
            }
        }

    public boolean isAdbListeningOnTcpip()
        {
        String value = getStringProperty(Configuration.PROP_ADB_TCP_PORT);
        return value != null && Integer.parseInt(value) != 0;
        }
    public boolean isAdbListeningOnTcpipPort(int port)
        {
        return Integer.toString(port).equals(getStringProperty(Configuration.PROP_ADB_TCP_PORT));
        }
    public boolean listenOnTcpip()
        {
        return androidDevice.getDatabase().getHostAdb().tcpip(device, Configuration.ADB_DAEMON_PORT);
        }
    public boolean awaitListeningOnTcpip(long timeout, TimeUnit timeUnit)
        {
        long deadline = nsNow() + timeUnit.toNanos(timeout);
        while (nsNow() <= deadline)
            {
            if (isAdbListeningOnTcpip())
                {
                return true;
                }
            Thread.yield();
            }
        return false;
        }
    protected long nsNow()
        {
        return System.nanoTime();
        }


    //----------------------------------------------------------------------------------------------
    // Low level accessing
    //----------------------------------------------------------------------------------------------

    /** @return null if the property doesn't exist */
    public @Nullable String getStringProperty(String property)
        {
        try
            {
            return device.getSystemProperty(property).get(Configuration.msAdbTimeoutFast, TimeUnit.MILLISECONDS);
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            throw new RuntimeException("interrupt while retrieving property: " + property, e);
            }
        catch (ExecutionException e)
            {
            throw new RuntimeException("exception while retrieving property: " + property, e);
            }
        catch (TimeoutException e)
            {
            throw new RuntimeException("timeout while retrieving property: " + property, e);
            }
        }

    public String getStringProperty(String property, String defaultValue)
        {
        String result = getStringProperty(property);
        return result==null ? defaultValue : result;
        }
    }
