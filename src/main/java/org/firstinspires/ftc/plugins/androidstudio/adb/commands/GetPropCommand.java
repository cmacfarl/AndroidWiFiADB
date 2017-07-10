package org.firstinspires.ftc.plugins.androidstudio.adb.commands;

import com.android.ddmlib.IDevice;
import com.android.ddmlib.MultiLineReceiver;
import org.firstinspires.ftc.plugins.androidstudio.util.StringUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link GetPropCommand} returns the map of all properties available on the device.
 */
@SuppressWarnings("WeakerAccess")
public class GetPropCommand extends AdbShellCommand
    {
    public static final String TAG = "GetPropCommand";

    protected Map<String, String> properties = new HashMap<>();

    public boolean execute(IDevice device)
        {
        Receiver receiver = new Receiver();
        return executeShellCommand(device, "getprop", receiver);
        }

    public Map<String,String> getProperties()
        {
        return properties;
        }

    protected class Receiver extends MultiLineReceiver
        {
        protected static final String GETPROP_PATTERN = "^\\[([^]]+)\\]\\:\\s*\\[(.*)\\]$";
        protected Pattern pattern = Pattern.compile(GETPROP_PATTERN);

        @Override public boolean isCancelled()
            {
            return false;
            }

        @Override
        public void processNewLines(String[] lines)
            {
            for (String line : lines)
                {
                if (StringUtil.isNullOrEmpty(line) || line.startsWith("#") || line.startsWith("$"))
                    {
                    continue;
                    }

                Matcher matcher = pattern.matcher(line);
                if (matcher.find())
                    {
                    String key = matcher.group(1).trim();
                    String value = matcher.group(2).trim();
                    if (key.length() > 0)
                        {
                        properties.put(key, value);
                        }
                    }
                }
            }

        }
    }
