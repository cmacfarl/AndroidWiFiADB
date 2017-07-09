package org.firstinspires.ftc.plugins.androidstudio.util;

import com.intellij.openapi.diagnostic.Logger;

import java.util.Locale;

/**
 * {@link EventLog} provides access to a log that's visible inside of Android Studio.
 */
@SuppressWarnings("WeakerAccess")
public class EventLog
    {
    protected static Logger logger = Logger.getInstance(EventLog.class);

    public static void notify(String tag, String message)
        {
        notify(tag, "%s", message);
        }
    public static void notify(String tag, String format, Object...args)
        {
        ii(tag, format, args);
        // TODO: also put in more visible location
        }

    protected static String getHeader(String tag, String flag)
        {
        return String.format(Locale.ROOT, "%d %s/%s", Thread.currentThread().getId(), tag, flag);
        }


    public static void ii(String tag, String message)
        {
        ii(tag, "%s", message);
        }
    public static void ii(String tag, String format, Object...args)
        {
        String header = getHeader(tag, "I");
        String message = String.format(format, args);
        String line = String.format("%s: %s", header, message);
        logger.info(line);
        NotificationHelper.info(line);
        }

    public static void dd(Object instance, String message)
        {
        dd(instance, "%s", message);
        }
    public static void dd(Object instance, String format, Object...args)
        {
        dd(instance.getClass(), format, args);
        }
    public static void dd(Class clazz, String message)
        {
        dd(clazz, "%s", message);
        }
    public static void dd(Class clazz, String format, Object...args)
        {
        String header = getHeader(clazz.getSimpleName(), "D");
        String message = String.format(format, args);
        String line = String.format("%s: %s", header, message);
        Logger.getInstance(clazz).info(line);
        NotificationHelper.info(line);
        }
    public static void dd(String tag, String message)
        {
        dd(tag, "%s", message);
        }
    public static void dd(String tag, String format, Object...args)
        {
        String header = getHeader(tag, "D");
        String message = String.format(format, args);
        String line = String.format("%s: %s", header, message);
        logger.info(line);
        NotificationHelper.info(line);
        }


    public static void ee(String tag, String message)
        {
        ee(tag, "%s", message);
        }
    public static void ee(String tag, String format, Object...args)
        {
        ee(tag, null, format, args);
        }

    public static void ee(String tag, Throwable throwable, String message)
        {
        ee(tag, throwable, "%s", message);
        }

    public static void ee(String tag, Throwable throwable, String format, Object...args)
        {
        String line = formatLine(tag, format, args);
        if (throwable != null)
            {
            NotificationHelper.error(line);
            logger.error(line, throwable);
            NotificationHelper.error(String.format("exception: %s: %s", throwable.getClass().getSimpleName(), throwable.getMessage()));
            logStackFrames(tag, throwable.getStackTrace());
            for (throwable = throwable.getCause(); throwable != null; throwable = throwable.getCause())
                {
                NotificationHelper.error(String.format("caused by: %s: %s", throwable.getClass().getSimpleName(), throwable.getMessage()));
                logStackFrames(tag, throwable.getStackTrace());
                }
            }
        else
            {
            NotificationHelper.error(line);
            logger.error(line);
            }
        }

    private static String formatLine(String tag, String format, Object...args)
        {
        String header = getHeader(tag, "E");
        String message = String.format(format, args);
        return String.format("%s: %s", header, message);
        }

    private static void logStackFrames(String tag, StackTraceElement[] stackTrace)
        {
        StringBuilder frames = new StringBuilder();
        for (StackTraceElement frame : stackTrace)
            {
            if (frames.length() > 0)
                {
                frames.append("\n");
                }
            frames.append(formatLine(tag, "    at %s", frame.toString()));
            }
        NotificationHelper.error(frames.toString());
        }
    }

