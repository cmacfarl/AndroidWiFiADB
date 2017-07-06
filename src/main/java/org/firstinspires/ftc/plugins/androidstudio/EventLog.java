package org.firstinspires.ftc.plugins.androidstudio;

import com.intellij.openapi.project.Project;

/**
 * {@link EventLog} provides access to a log that's visible inside of Android Studio.
 */
@SuppressWarnings("WeakerAccess")
public class EventLog
    {
    public static void ii(String tag, String message)
        {
        ii(tag, "%s", message);
        }
    public static void ii(String tag, String format, Object...args)
        {
        ii(tag, null, format, args);
        }
    public static void ii(String tag, Project project, String message)
        {
        ii(tag, project, "%s", message);
        }
    public static void ii(String tag, Project project, String format, Object...args)
        {
        String header = String.format("%s%s/E", tag, project==null ? "" : ("(" + project.getName() + ")"));
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
    public static void ee(String tag, Project project, String message)
        {
        ee(tag, project, "%s", message);
        }
    public static void ee(String tag, Project project, String format, Object...args)
        {
        ee(tag, project, null, format, args);
        }

    public static void ee(String tag, Project project, Throwable throwable, String message)
        {
        ee(tag, project, throwable, "%s", message);
        }

    public static void ee(String tag, Project project, Throwable throwable, String format, Object...args)
        {
        String line = formatLine(tag, project, format, args);
        NotificationHelper.error(line);
        if (throwable != null)
            {
            NotificationHelper.error(String.format("exception: %s: %s", throwable.getClass().getSimpleName(), throwable.getMessage()));
            logStackFrames(tag, project, throwable.getStackTrace());
            for (throwable = throwable.getCause(); throwable != null; throwable = throwable.getCause())
                {
                NotificationHelper.error(String.format("caused by: %s: %s", throwable.getClass().getSimpleName(), throwable.getMessage()));
                logStackFrames(tag, project, throwable.getStackTrace());
                }
            }
        }

    private static String formatLine(String tag, Project project, String format, Object...args)
        {
        String header = String.format("%s%s/E", tag, project==null ? "" : ("(" + project.getName() + ")"));
        String message = String.format(format, args);
        return String.format("%s: %s", header, message);
        }

    private static void logStackFrames(String tag, Project project, StackTraceElement[] stackTrace)
        {
        StringBuilder frames = new StringBuilder();
        for (StackTraceElement frame : stackTrace)
            {
            if (frames.length() > 0)
                {
                frames.append("\n");
                }
            frames.append(formatLine(tag, project, "    at %s", frame.toString()));
            }
        NotificationHelper.error(frames.toString());
        }
    }

