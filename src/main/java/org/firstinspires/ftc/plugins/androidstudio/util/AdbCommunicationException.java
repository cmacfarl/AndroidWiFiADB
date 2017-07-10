package org.firstinspires.ftc.plugins.androidstudio.util;

import java.util.Locale;

/**
 * {@link AdbCommunicationException} wraps all the the various forms of communication failures
 * that can take place with a device
 */
public class AdbCommunicationException extends Exception
    {
    public AdbCommunicationException(String message)
        {
        super(message);
        }
    public AdbCommunicationException(Throwable throwable, String message)
        {
        super(message, throwable);
        }
    public AdbCommunicationException(Throwable throwable, String format, Object...args )
        {
        this(throwable, String.format(Locale.ROOT, format, args));
        }

    }
