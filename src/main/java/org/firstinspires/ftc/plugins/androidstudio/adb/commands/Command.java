package org.firstinspires.ftc.plugins.androidstudio.adb.commands;

import com.android.ddmlib.IDevice;
import com.android.ddmlib.IShellOutputReceiver;
import org.firstinspires.ftc.plugins.androidstudio.util.EventLog;

import java.util.concurrent.TimeUnit;

/**
 * Created by bob on 2017-07-07.
 */
public abstract class Command
    {
    public static final String TAG = "Command";

    protected boolean executeShellCommand(IDevice device, String command, IShellOutputReceiver receiver)
        {
        try {
            device.executeShellCommand(command, receiver, 10, TimeUnit.SECONDS);
            return true;
            }
        catch (Exception e)
            {
            EventLog.ee(TAG, e, "command failed: %s", command);
            }
        return false;
        }
    }
