package org.firstinspires.ftc.plugins.androidstudio.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by bob on 2017-07-07.
 */
public class Misc
    {
    public static InetAddress ipAddress(String literalAddress)
        {
        try {
            return InetAddress.getByName(literalAddress);
            }
        catch (UnknownHostException e)
            {
            throw new RuntimeException("internal error", e);
            }
        }
    }
