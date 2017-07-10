package org.firstinspires.ftc.plugins.androidstudio.adb.commands;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.IShellOutputReceiver;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;
import org.firstinspires.ftc.plugins.androidstudio.Configuration;
import org.firstinspires.ftc.plugins.androidstudio.util.AdbCommunicationException;

import java.io.IOException;
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

    protected IDevice device = null;
    protected String executedCommand = "<unexecuted>";

    protected AdbShellCommand(IDevice device)
        {
        this.device = device;
        }

    protected void executeShellCommand(String command, IShellOutputReceiver receiver) throws AdbCommunicationException
        {
        try {
            executedCommand = command;
            device.executeShellCommand(command, receiver, Configuration.msAdbTimeoutSlow, TimeUnit.MILLISECONDS);
            }
        catch (AdbCommandRejectedException|TimeoutException|ShellCommandUnresponsiveException|IOException e)
            {
            throw new AdbCommunicationException(e, "command failed: %s", command);
            }
        }

    protected RuntimeException resultError(String message)
        {
        return resultError("%s", message);
        }
    protected RuntimeException resultError(String format, Object...args)
        {
        String message = String.format(Locale.ROOT, format, args);
        String device = this.device ==null ? "" : String.format(Locale.ROOT, " device=%s", this.device.getSerialNumber());
        String payload = String.format("%s:%s command='%s' %s", this.getClass().getSimpleName(), device, executedCommand, message);
        return new RuntimeException(payload);
        }
    }
