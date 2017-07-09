package org.firstinspires.ftc.plugins.androidstudio.adb.commands;

import com.android.ddmlib.IDevice;
import org.firstinspires.ftc.plugins.androidstudio.adb.AndroidDeviceHandle;
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
 * Created by bob on 2017-07-07.
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

    // Example Android response:
    //  p2p0: ip 192.168.49.1 mask 255.255.255.0 flags [up broadcast running multicast]
    //
    protected static Pattern patternFindInetAddr = Pattern.compile(String.format(" ip (%s)", AndroidDeviceHandle.patternIpAddress));
    protected static Pattern patternFindFlags = Pattern.compile("flags \\[([a-zA-Z ]*)\\]");

    public @Nullable InetAddress getInetAddress()
        {
        Matcher matcher = patternFindInetAddr.matcher(receiver.getResult());
        if (matcher.find())
            {
            return IpUtil.parseInetAddress(matcher.group(1));
            }
        else
            {
            // On a driver station, for example, this may return 'p2p0: Cannot assign requested address'
            }
        return null;
        }

    public List<String> getFlags()
        {
        Matcher matcher = patternFindFlags.matcher(receiver.getResult());
        if (matcher.find())
            {
            String[] splits = matcher.group(1).toLowerCase(Locale.ROOT).split(" ");
            return Arrays.asList(splits);
            }
        return Collections.emptyList();
        }

    public boolean isUp()
        {
        return getFlags().contains("up");
        }

    }
