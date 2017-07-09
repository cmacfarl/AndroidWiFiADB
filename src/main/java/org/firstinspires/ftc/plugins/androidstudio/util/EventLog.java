package org.firstinspires.ftc.plugins.androidstudio.util;

/**
 * {@link EventLog} provides access to a log that's visible inside of Android Studio.
 */
@SuppressWarnings("WeakerAccess")
public class EventLog
    {
    public static void notify(String tag, String message)
        {
        notify(tag, "%s", message);
        }
    public static void notify(String tag, String format, Object...args)
        {
        ii(tag, format, args);
        // TODO: also put in more visible location
        }


    public static void ii(String tag, String message)
        {
        ii(tag, "%s", message);
        }
    public static void ii(String tag, String format, Object...args)
        {
        String header = String.format("%s/I", tag);
        String message = String.format(format, args);
        NotificationHelper.info(String.format("%s: %s", header, message));
        }

    public static void dd(String tag, String message)
        {
        dd(tag, "%s", message);
        }
    public static void dd(String tag, String format, Object...args)
        {
        String header = String.format("%s/D", tag);
        String message = String.format(format, args);
        NotificationHelper.info(String.format("%s: %s", header, message));
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
        NotificationHelper.error(line);
        if (throwable != null)
            {
            NotificationHelper.error(String.format("exception: %s: %s", throwable.getClass().getSimpleName(), throwable.getMessage()));
            logStackFrames(tag, throwable.getStackTrace());
            for (throwable = throwable.getCause(); throwable != null; throwable = throwable.getCause())
                {
                NotificationHelper.error(String.format("caused by: %s: %s", throwable.getClass().getSimpleName(), throwable.getMessage()));
                logStackFrames(tag, throwable.getStackTrace());
                }
            }
        }

    private static String formatLine(String tag, String format, Object...args)
        {
        String header = String.format("%s/E", tag);
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

