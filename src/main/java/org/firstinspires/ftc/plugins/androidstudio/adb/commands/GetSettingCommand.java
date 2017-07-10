package org.firstinspires.ftc.plugins.androidstudio.adb.commands;

import com.android.ddmlib.IDevice;

import java.util.Locale;

/**
 * {@link GetSettingCommand} retrieves a setting (system, secure, or global) from the device.
 */
@SuppressWarnings("WeakerAccess")
public class GetSettingCommand extends AdbShellCommand
    {
    public enum Namespace { SYSTEM, SECURE, GLOBAL };

    protected Namespace namespace;
    protected String setting;
    protected AdbShellCommandResultCollector receiver = new AdbShellCommandResultCollector();

    public GetSettingCommand(Namespace namespace, String setting)
        {
        this.namespace = namespace;
        this.setting = setting;
        }

    public boolean execute(IDevice device)
        {
        return executeShellCommand(device, String.format(Locale.ROOT, "settings get %s %s", namespace, setting), receiver);
        }

    public String getResult()
        {
        return receiver.getResult();
        }
    }
