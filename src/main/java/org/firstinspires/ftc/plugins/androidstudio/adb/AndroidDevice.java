package org.firstinspires.ftc.plugins.androidstudio.adb;

import org.firstinspires.ftc.plugins.androidstudio.util.MemberwiseCloneable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by bob on 2017-07-06.
 */
public class AndroidDevice extends MemberwiseCloneable<AndroidDevice>
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected String usbSerialNumber;
    protected String wifiDirectName;
    protected String wlanIpAddress;     // the IP address, if any, of the wlan0 address of this fellow
    protected boolean wlanIsRunning;
    protected String ipAddressLastConnected;

    /** current serial numbers we have for this guy and by which he is known in ADB */
    protected List<String> serialNumbers = new ArrayList<>();
    /** does ADB currently know about this guy? */
    protected boolean isConnected = false;
    protected Map<String,String> properties = new ConcurrentHashMap<>();

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    //----------------------------------------------------------------------------------------------
    // Accessing
    //----------------------------------------------------------------------------------------------

    public void clearProperties()
        {
        properties.clear();
        }

    public void addProperty(String key, String value)
        {
        properties.put(key, value);
        }

    //----------------------------------------------------------------------------------------------
    // Commands
    //----------------------------------------------------------------------------------------------

    public void refreshProperties()
        {

        }

    }
