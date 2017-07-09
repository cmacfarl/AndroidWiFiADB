package org.firstinspires.ftc.plugins.androidstudio.util;

import java.io.PrintStream;
import java.util.Locale;

/**
 * Created by bob on 2017-07-06.
 */
@SuppressWarnings("WeakerAccess")
public class StringUtil
    {
    public static boolean isNullOrEmpty(String string)
        {
        return string==null || string.length()==0;
        }

    public static boolean notNullOrEmpty(String string)
        {
        return !isNullOrEmpty(string);
        }

    public static void appendLine(PrintStream out, String format, Object...args)
        {
        appendLine(0, out, format, args);
        }

    public static void appendLine(int indent, PrintStream out, String format, Object...args)
        {
        appendLine(indent, out, String.format(Locale.ROOT, format, args));
        }

    public static void appendLine(PrintStream out, String line)
        {
        appendLine(0, out, line);
        }

    public static void appendLine(int indent, PrintStream out, String line)
        {
        for (int i=0; i < indent; i++)
            {
            out.append("    ");
            }
        out.print(line);
        out.println();
        }

    }
