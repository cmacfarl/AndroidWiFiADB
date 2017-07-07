package org.firstinspires.ftc.plugins.androidstudio.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.firstinspires.ftc.plugins.androidstudio.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by bob on 2017-07-07.
 */
@SuppressWarnings("WeakerAccess")
public class ThreadPool
    {
    protected static class ThreadPoolHolder
        {
        public static ExecutorService theInstance = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat(Configuration.PROJECT_NAME + "-%d").build());
        }

    public static Executor getDefault()
        {
        return ThreadPoolHolder.theInstance;
        }
    }
