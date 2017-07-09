package org.firstinspires.ftc.plugins.androidstudio.util;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Created by bob on 2017-07-09.
 */
@SuppressWarnings("WeakerAccess")
public class ReentrantLockOwner
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    protected final ReentrantLock lock = new ReentrantLock();

    //----------------------------------------------------------------------------------------------
    // Tracing
    //----------------------------------------------------------------------------------------------

    protected void trace(@Nullable String tag, Runnable runnable)
        {
        trace(tag, () -> {
            runnable.run();
            return null;
            });
        }

    protected <T> T trace(@Nullable String tag, Supplier<T> supplier)
        {
        if (tag != null) EventLog.dd(this, "%s...", tag);
        try {
            return supplier.get();
            }
        finally
            {
            if (tag != null) EventLog.dd(this, "...%s", tag);
            }
        }

    //----------------------------------------------------------------------------------------------
    // Locking
    //----------------------------------------------------------------------------------------------

    protected void lockWhile(Runnable runnable)
        {
        lockWhile(null, runnable);
        }

    protected void lockWhile(@Nullable String tag, Runnable runnable)
        {
        lockWhile(tag, () ->
            {
            runnable.run();
            return null;
            });
        }

    protected <T> T lockWhile(Supplier<T> supplier)
        {
        return lockWhile(null, supplier);
        }

    protected <T> T lockWhile(@Nullable String tag, Supplier<T> supplier)
        {
        return trace(tag, () ->
            {
            try
                {
                lock.lockInterruptibly();
                try
                    {
                    return supplier.get();
                    }
                finally
                    {
                    lock.unlock();
                    }
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                throw new RuntimeException("interruption");
                }
            });
        }
    }
