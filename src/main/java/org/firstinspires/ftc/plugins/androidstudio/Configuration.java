package org.firstinspires.ftc.plugins.androidstudio;

import org.firstinspires.ftc.plugins.androidstudio.util.IpUtil;

import java.net.InetAddress;

/**
 * Constants and strings that configure the plugin.
 */
@SuppressWarnings("WeakerAccess")
public class Configuration
    {
    public static final String PROJECT_NAME       = "Ftc Plugin";
    public static final String LOGGING_GROUP_NAME = "Ftc Plugin (Logging)";
    public static final String ERROR_GROUP_NAME   = "Ftc Plugin (Errors)";

    // For application components, this will be in ~/.AndroidStudio2.3/config/options
    // For project components, it will be in the .idea directory of the project
    public static final String XML_STATE_FILE_NAME = "ftcAndroidStudioPluginState.xml";

    public static final String PROP_USB_SERIAL_NUMBER = "ro.boot.serialno";
    public static final String PROP_BUILD_VERSION     = "ro.build.version.release";
    public static final String PROP_BUILD_API_LEVEL   = "ro.build.version.sdk";
    public static final String PROP_BUILD_CODENAME    = "ro.build.version.codename";
    public static final String PROP_DEBUGGABLE        = "ro.debuggable";
    public static final String PROP_WLAN_STATUS       = "init.svc.dhcpcd_wlan0";
    public static final String PROP_WLAN_IP_ADDRESS   = "dhcp.wlan0.ipaddress";

    /** Non-zero when the device is listening on TCPIP (possibly in addition to USB) */
    public static final String PROP_ADB_TCP_PORT      = "service.adb.tcp.port";

    public static final String SETTING_WIFI_P2P_DEVICE_NAME = "wifi_p2p_device_name";

    public static final InetAddress WIFI_DIRECT_GROUP_OWNER_ADDRESS = IpUtil.parseInetAddress("192.168.49.1");
    public static final int ADB_DAEMON_PORT = 5555;

    public static int msAdbTimeoutSlow = 4000;
    public static int msAdbTimeoutFast = 2000;
    }
