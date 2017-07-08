package org.firstinspires.ftc.plugins.androidstudio.adb.commands;

import com.android.ddmlib.MultiLineReceiver;
import org.firstinspires.ftc.plugins.androidstudio.util.StringUtils;

/**
 * Created by bob on 2017-07-07.
 */
@SuppressWarnings("WeakerAccess")
public class AdbShellCommandResultCollector extends MultiLineReceiver
    {
    protected StringBuilder builder = new StringBuilder();
    protected String commandResult = null;

    @Override
    public boolean isCancelled()
        {
        return false;
        }

    @Override
    public void processNewLines(String[] lines)
        {
        for (String line : lines)
            {
            if (StringUtils.isNullOrEmpty(line) || line.startsWith("#") || line.startsWith("$"))
                {
                continue;
                }
            if (builder.length() > 0) builder.append("\n");
            builder.append(line);
            }
        }

    @Override
    public void done()
        {
        this.commandResult = builder.toString();
        }

    public String getResult()
        {
        return commandResult;
        }

    }
