package org.firstinspires.ftc.plugins.androidstudio.adb.commands;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.intellij.openapi.project.Project;
import org.firstinspires.ftc.plugins.androidstudio.Configuration;
import org.firstinspires.ftc.plugins.androidstudio.util.EventLog;
import org.jetbrains.android.sdk.AndroidSdkUtils;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Functionality we access by executing the local ADB command rather than trying to cons up
 * our own socket data to do so. Necessary as not all the ADB socket protocol is supported through
 * publicly available function in {@link AndroidDebugBridge}, etc.
 *
 * This is just the easier way to go.
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
    // Accessing
    //----------------------------------------------------------------------------------------------

    public File getAdb()
        {
        return adbExecutable;
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
        String result = executeSystemCommand(composeCommand(device, payload));

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
        return connect(inetSocketAddress, 0);
        }

    /** Note: this can take a *very* long time if there's no device reachable at the
     * indicated address */
    public boolean connect(InetSocketAddress inetSocketAddress, int msTimeout)
        {
        String address = String.format("%s:%d", inetSocketAddress.getAddress().getHostAddress(), inetSocketAddress.getPort());
        String result = executeSystemCommand(composeCommand(null, "connect " + address), msTimeout);

        /* Example executions:

            C:\Users\bob>adb connect 192.168.49.1:5555
            connected to 192.168.49.1:5555

            C:\Users\bob>adb connect 192.168.49.1:5555
            already connected to 192.168.49.1:5555

            C:\Users\bob>adb connect 192.168.49.3:5555
            unable to connect to 192.168.49.3:5555: cannot connect to 192.168.49.3:5555: A connection attempt failed
            because the connected party did not properly respond after a period of time, or established connection
            failed because connected host has failed to respond. (10060)

            C:\Users\bob>adb connect /192.168.49.1:5555
            unable to connect to /192.168.49.1:5555: cannot resolve host '/192.168.49.1' and port 5555: No such host is known. (11001)
         */
        return result.contains("connected to " + address);
        }

    public boolean disconnect(IDevice device)
        {
        String payload = String.format("disconnect %s", device.getSerialNumber());
        String result = executeSystemCommand(composeCommand(null, payload));

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

    protected String composeCommand(@Nullable IDevice device, String command)
        {
        return adbExecutable.getAbsolutePath()
                + (device != null ? " -s " + device.getSerialNumber() : "")
                + " "
                + command;
        }

    protected String executeSystemCommand(String command)
        {
        return executeSystemCommand(command, 0);
        }

    protected String executeSystemCommand(String command, int msTimeout)
        {
        StringBuilder result = new StringBuilder();
        try
            {
            EventLog.dd(this, "executing: %s", command);
            Process process = Runtime.getRuntime().exec(command);
            if (msTimeout == 0)
                {
                process.waitFor();
                }
            else
                {
                if (!process.waitFor(msTimeout, TimeUnit.MILLISECONDS))
                    {
                    process.destroy();
                    return "";
                    }
                }
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
