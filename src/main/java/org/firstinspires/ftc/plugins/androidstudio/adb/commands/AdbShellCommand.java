package org.firstinspires.ftc.plugins.androidstudio.adb.commands;

import com.android.ddmlib.IDevice;
import com.android.ddmlib.IShellOutputReceiver;
import org.firstinspires.ftc.plugins.androidstudio.Configuration;
import org.firstinspires.ftc.plugins.androidstudio.util.EventLog;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * {@link AdbShellCommand} instances represent commands to be executed in the shell on
 * the remote device
 */
@SuppressWarnings("WeakerAccess")
public abstract class AdbShellCommand
    {
    public static final String TAG = "Command";

    protected IDevice executedDevice = null;
    protected String executedCommand = "<unexecuted>";

    protected boolean executeShellCommand(IDevice device, String command, IShellOutputReceiver receiver)
        {
        try {
            executedCommand = command;
            device.executeShellCommand(command, receiver, Configuration.msAdbTimeoutSlow, TimeUnit.MILLISECONDS);
            return true;
            }
        catch (Exception e)
            {
            EventLog.ee(TAG, e, "command failed: %s", command);
            }
        return false;
        }

    protected RuntimeException resultError(String message)
        {
        return resultError("%s", message);
        }
    protected RuntimeException resultError(String format, Object...args)
        {
        String message = String.format(Locale.ROOT, format, args);
        String device = executedDevice==null ? "" : String.format(Locale.ROOT, " device=%s", executedDevice.getSerialNumber());
        String payload = String.format("%s:%s command='%s' %s", this.getClass().getSimpleName(), device, executedCommand, message);
        return new RuntimeException(payload);
        }
    }
