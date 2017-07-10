package org.firstinspires.ftc.plugins.androidstudio.adb.commands;

import com.android.ddmlib.IDevice;
import org.firstinspires.ftc.plugins.androidstudio.adb.AndroidDeviceHandle;
import org.firstinspires.ftc.plugins.androidstudio.util.EventLog;
import org.firstinspires.ftc.plugins.androidstudio.util.IpUtil;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link IfConfigCommand} manages the execution of the 'ifconfig' command on the device.
 */
@SuppressWarnings("WeakerAccess")
public class IfConfigCommand extends AdbShellCommand
    {
    public static final String TAG = "IfConfigCommand";

    protected AdbShellCommandResultCollector receiver = new AdbShellCommandResultCollector();

    public boolean execute(IDevice device, String intf)
        {
        return executeShellCommand(device, "ifconfig " + intf, receiver);
        }

    // Example Android responses:
    //
    //  p2p0: ip 192.168.49.1 mask 255.255.255.0 flags [up broadcast running multicast]
    //
    //  p2p0    Link encap:UNSPEC
    //          inet addr:192.168.49.1  Bcast:192.168.49.255  Mask:255.255.255.0
    //          inet6 addr: fe80::e298:61ff:fed7:93/64 Scope: Link
    //          UP BROADCAST RUNNING MULTICAST  MTU:1500  Metric:1
    //          RX packets:326 errors:0 dropped:0 overruns:0 frame:0
    //          TX packets:341 errors:0 dropped:0 overruns:0 carrier:0
    //          collisions:0 txqueuelen:1000
    //          RX bytes:38682 TX bytes:39903
    //
    protected static Pattern patternFindInetAddr = Pattern.compile(String.format("\\s(ip\\s|inet addr:)(?<addr>%s)", AndroidDeviceHandle.patternIpAddress));

    protected static String words = "([a-zA-Z]+\\s+)+";
    protected static Pattern patternFindFlags1 = Pattern.compile(String.format("flags\\s\\[(?<flags>%s)\\]", words));
    protected static Pattern patternFindFlags2 = Pattern.compile(String.format("ink\\s(?<flags>%s)\\s*MTU:", words));

    public @Nullable InetAddress getInetAddress()
        {
        String response = receiver.getResult();
        Matcher matcher = patternFindInetAddr.matcher(response);
        if (matcher.find())
            {
            String addr = matcher.group("addr");
            EventLog.dd(TAG, "addr matched='%s'", addr);
            return IpUtil.parseInetAddress(addr);
            }
        else
            {
            // On a driver station, for example, this may return 'p2p0: Cannot assign requested address'
            EventLog.dd(TAG, "addr not found: response=%s", response);
            }
        return null;
        }

    public List<String> getFlags()
        {
        String response = receiver.getResult();
        Matcher matcher = patternFindFlags1.matcher(response);
        if (!matcher.find())
            {
            matcher = patternFindFlags2.matcher(response);
            if (!matcher.find())
                {
                EventLog.dd(TAG, "flags not found");
                return Collections.emptyList();
                }
            }

        String flags = matcher.group("flags").toLowerCase(Locale.ROOT);
        String[] splits = flags.split("\\s+");
        List<String> result = Arrays.asList(splits);
        EventLog.dd(TAG, "flags='%s'", result);
        return result;
        }

    public boolean isUp()
        {
        return getFlags().contains("up");
        }
    }
