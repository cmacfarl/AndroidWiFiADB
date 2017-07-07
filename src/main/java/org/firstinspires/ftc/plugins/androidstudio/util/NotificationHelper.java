package org.firstinspires.ftc.plugins.androidstudio.util;

import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import org.firstinspires.ftc.plugins.androidstudio.Configuration;

public class NotificationHelper
    {
    private static final NotificationGroup INFO = NotificationGroup.logOnlyGroup(Configuration.LOGGING_GROUP_NAME);
    private static final NotificationGroup ERROR = NotificationGroup.logOnlyGroup(Configuration.ERROR_GROUP_NAME);
    private static final NotificationListener NOOP_LISTENER = (notification, event) ->
        {
        };

    public static void info(String message)
        {
        sendNotification(message, NotificationType.INFORMATION, INFO);
        }

    public static void error(String message)
        {
        sendNotification(message, NotificationType.ERROR, ERROR);
        }

    private static void sendNotification(String message, NotificationType notificationType, NotificationGroup notificationGroup)
        {
        notificationGroup.createNotification(Configuration.PROJECT_NAME, escapeString(message), notificationType, NOOP_LISTENER).notify(null);
        }


    private static String escapeString(String string)
        {
        return string.replaceAll("\n", "\n<br />");
        }
    }
