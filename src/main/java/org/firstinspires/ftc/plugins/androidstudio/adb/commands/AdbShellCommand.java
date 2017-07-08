package org.firstinspires.ftc.plugins.androidstudio.adb.commands;

import com.android.ddmlib.IDevice;
import com.android.ddmlib.IShellOutputReceiver;
import org.firstinspires.ftc.plugins.androidstudio.Configuration;
import org.firstinspires.ftc.plugins.androidstudio.util.EventLog;

import java.util.concurrent.TimeUnit;

/**
 * {@link AdbShellCommand} instances represent commands to be executed in the shell on
 * the remote device
 */
@SuppressWarnings("WeakerAccess")
public abstract class AdbShellCommand
    {
    public static final String TAG = "Command";

    protected boolean executeShellCommand(IDevice device, String command, IShellOutputReceiver receiver)
        {
        try {
            device.executeShellCommand(command, receiver, Configuration.msAdbTimeoutSlow, TimeUnit.MILLISECONDS);
            return true;
            }
        catch (Exception e)
            {
            EventLog.ee(TAG, e, "command failed: %s", command);
            }
        return false;
        }
    }
