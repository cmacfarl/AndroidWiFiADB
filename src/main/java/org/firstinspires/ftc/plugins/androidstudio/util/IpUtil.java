package org.firstinspires.ftc.plugins.androidstudio.util;

import org.firstinspires.ftc.plugins.androidstudio.Configuration;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * A simple utility class for ip-related things
 */
public class IpUtil
    {
    public static InetAddress parseInetAddress(String literalAddress)
        {
        if (literalAddress==null) return null;
        try {
            return InetAddress.getByName(literalAddress);
            }
        catch (UnknownHostException e)
            {
            throw new RuntimeException("internal error parsing inetAddress: " + literalAddress, e);
            }
        }

    public static String toString(InetAddress inetAddress)
        {
        return inetAddress==null
                ? null
                : inetAddress.getHostAddress();
        }

    public static InetSocketAddress parseInetSocketAddress(String literalAddressAndPort)
        {
        if (literalAddressAndPort==null) return null;
        String[] splits = literalAddressAndPort.split(":");
        return new InetSocketAddress(parseInetAddress(splits[0]), Integer.parseInt(splits[1]));
        }

    public static String toString(InetSocketAddress inetSocketAddress)
        {
        return inetSocketAddress==null
                ? null
                : toString(inetSocketAddress.getAddress()) + ":" + inetSocketAddress.getPort();
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
