package org.firstinspires.ftc.plugins.androidstudio.util;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link NetworkInterfaceMonitor} monitors for network interfaces coming and going
 */
@SuppressWarnings("WeakerAccess")
public class NetworkInterfaceMonitor
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public interface Callback
        {
        void onNetworkInterfacesUp();
        void onNetworkInterfacesDown();
        }

    protected final Callback callback;
    protected final int msPollingInterval = 5000;
    protected final AtomicReference<Thread> thread = new AtomicReference<>(null);
    protected Map<String, NetworkInterface> currentInterfaces = new ConcurrentHashMap<>();

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public NetworkInterfaceMonitor(Callback callback)
        {
        this.callback = callback;
        }

    //----------------------------------------------------------------------------------------------
    // Operations
    //----------------------------------------------------------------------------------------------

    public void start()
        {
        EventLog.dd(this, "start()");
        stop();
        CountDownLatch latch = new CountDownLatch(1);
        Thread thread = new Thread(new Runnable()
            {
            @Override public void run()
                {
                latch.countDown();
                try {
                    //noinspection InfiniteLoopStatement
                    while (true)
                        {
                        poll();
                        Thread.sleep(msPollingInterval);
                        }
                    }
                catch (InterruptedException e)
                    {
                    // ignore, fall off end of thread
                    }
                }
            });
        thread.start();
        try {
            latch.await();
            }
        catch (InterruptedException e)
            {
            stop();
            Thread.currentThread().interrupt();
            }
        this.thread.set(thread);
        }

    public void stop()
        {
        Thread thread = this.thread.getAndSet(null);
        if (thread != null)
            {
            EventLog.dd(this, "stop()");
            thread.interrupt();
            }
        }

    protected <T> Set<T> setDifference(Set<T> left, Collection<T> right)
        {
        Set<T> result = new HashSet<>(left);
        result.removeAll(right);
        return result;
        }

    protected void poll()
        {
        Map<String, NetworkInterface> newInterfaces = getUpInterfaces();
        Set<String> newSet = newInterfaces.keySet();
        Set<String> currentSet = currentInterfaces.keySet();

        Set<String> newlyUp = setDifference(newSet, currentSet);
        Set<String> newlyDown = setDifference(currentSet, newSet);

        if (!newlyUp.isEmpty())
            {
            callback.onNetworkInterfacesUp();
            }

        if (!newlyDown.isEmpty())
            {
            callback.onNetworkInterfacesDown();
            }

        currentInterfaces = newInterfaces;
        }

    protected Map<String, NetworkInterface> getUpInterfaces()
        {
        Map<String, NetworkInterface> result = new ConcurrentHashMap<>();
        try
            {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements())
                {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isUp())
                    {
                    result.put(networkInterface.getName(), networkInterface);
                    }
                }
            }
        catch (SocketException e)
            {
            // ignore
            }
        return result;
        }

    }
