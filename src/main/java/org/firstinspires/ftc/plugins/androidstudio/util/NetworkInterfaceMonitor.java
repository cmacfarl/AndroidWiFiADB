package org.firstinspires.ftc.plugins.androidstudio.util;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Map;
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
        void onNetworkInterfacesChanged();
        }

    protected final Callback callback;
    protected final int msPollingInterval = 5000;
    protected final int msSettle = 2000;
    protected final AtomicReference<Thread> thread = new AtomicReference<>(null);
    protected Map<String, NetworkInterface> existing = new ConcurrentHashMap<>();

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
            thread.interrupt();
            }
        }

    protected void poll()
        {
        Map<String, NetworkInterface> current = getUpInterfaces();
        if (!current.keySet().equals(existing.keySet()))
            {
            // Hack: give the interface a chance to settle a bit
            try { Thread.currentThread().sleep(msSettle); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

            // Notify our client
            callback.onNetworkInterfacesChanged();
            }
        existing = current;
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
