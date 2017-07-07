package org.firstinspires.ftc.plugins.androidstudio.util;

/**
 * Created by bob on 2017-07-06.
 */
public class StringUtils
    {
    public static boolean isNullOrEmpty(String string)
        {
        return string==null || string.length()==0;
        }

    public static boolean notNullOrEmpty(String string)
        {
        return !isNullOrEmpty(string);
        }
    }
