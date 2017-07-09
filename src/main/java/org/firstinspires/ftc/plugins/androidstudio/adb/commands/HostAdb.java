package org.firstinspires.ftc.plugins.androidstudio.adb.commands;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.intellij.openapi.project.Project;
import org.firstinspires.ftc.plugins.androidstudio.Configuration;
import org.jetbrains.android.sdk.AndroidSdkUtils;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Functionality we access by executing the local ADB command rather than trying to cons up
 * our own socket data to do so. Necessary as not all the ADB socket protocol is supported through
 * publicly available function in {@link AndroidDebugBridge}, etc.
 *
 * This is just the easier way to go
 */
@SuppressWarnings("WeakerAccess")
public class HostAdb
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected final File adbExecutable;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public HostAdb(Project project)
        {
        this.adbExecutable = AndroidSdkUtils.getAdb(project).getAbsoluteFile();
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    public boolean tcpip(IDevice device)
        {
        return tcpip(device, Configuration.ADB_DAEMON_PORT);
        }

    /** Note that a successful response indicates only that the listening
     * request has been initiated, not that it has been completed */
    public boolean tcpip(IDevice device, int port)
        {
        String payload = String.format(Locale.ROOT,"tcpip %d", port);
        String result = executeCommand(formCommand(device, payload));

        /* Example executions:

            C:\Users\bob>adb -s 2a28399 tcpip 5555
            restarting in TCP mode port: 5555

            C:\Users\bob>adb -s 192.168.49.1:5555 tcpip 5555
            restarting in TCP mode port: 5555

            C:\Users\bob>adb -s 2a2839x9 tcpip 5555
            error: device '2a2839x9' not found
         */
        return !result.contains("error");   // hope that no serial number has 'error' in it
        }

    public boolean connect(InetAddress inetAddress)
        {
        return connect(new InetSocketAddress(inetAddress, Configuration.ADB_DAEMON_PORT));
        }

    public boolean connect(InetSocketAddress inetSocketAddress)
        {
        String payload = String.format("connect %s", inetSocketAddress.toString());
        String result = executeCommand(formCommand(null, payload));

        /* Example executions:

            C:\Users\bob>adb connect 192.168.49.1:5555
            connected to 192.168.49.1:5555

            C:\Users\bob>adb connect 192.168.49.1:5555
            already connected to 192.168.49.1:5555

            C:\Users\bob>adb connect 192.168.49.3:5555
            unable to connect to 192.168.49.3:5555: cannot connect to 192.168.49.3:5555: A connection attempt failed
            because the connected party did not properly respond after a period of time, or established connection
            failed because connected host has failed to respond. (10060)
         */
        return !result.contains("failed");
        }

    public boolean disconnect(IDevice device)
        {
        String payload = String.format("disconnect %s", device.getSerialNumber());
        String result = executeCommand(formCommand(null, payload));

        /* Example executions:

            C:\Users\bob>adb disconnect 192.168.49.3:5555
            error: no such device '192.168.49.3:5555'

            C:\Users\bob>adb disconnect 192.168.49.1:5555
            disconnected 192.168.49.1:5555

            C:\Users\bob>adb disconnect 2a28399
            error: no such device '2a28399:5555'
         */

        return !result.contains("error");
        }

    //----------------------------------------------------------------------------------------------
    // Utility
    //----------------------------------------------------------------------------------------------

    protected String formCommand(@Nullable IDevice device, String command)
        {
        return adbExecutable.getAbsolutePath()
                + (device != null ? " -s " + device.getSerialNumber() : "")
                + " "
                + command;
        }

    protected String executeCommand(String command)
        {
        StringBuilder result = new StringBuilder();
        try
            {
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();
            //
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            for (String line : reader.lines().collect(Collectors.toList()))
                {
                if (result.length() > 0) result.append("\n");
                result.append(line);
                }
            reader.close();
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            throw new RuntimeException("interrupt executing command: " + command, e);
            }
        catch (IOException e)
            {
            throw new RuntimeException("exception executing command: " + command, e);
            }
        return result.toString();
        }

    }
