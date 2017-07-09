package org.firstinspires.ftc.plugins.androidstudio.util;

import org.firstinspires.ftc.plugins.androidstudio.Configuration;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * A simple utility class for ip-related things
 */
public class IpUtil
    {
    public static InetAddress parseInetAddress(String literalAddress)
        {
        try {
            return InetAddress.getByName(literalAddress);
            }
        catch (UnknownHostException e)
            {
            throw new RuntimeException("internal error parsing inetAddress: " + literalAddress, e);
            }
        }

    public static boolean isPingable(InetAddress inetAddress)
        {
        try {
            return inetAddress.isReachable(Configuration.msAdbTimeoutFast);
            }
        catch (IOException|RuntimeException e)
            {
            return false;
            }
        }
    }
