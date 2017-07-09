package org.firstinspires.ftc.plugins.androidstudio.adb;


import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.Client;
import com.android.ddmlib.IDevice;
import org.firstinspires.ftc.plugins.androidstudio.util.EventLog;
import org.firstinspires.ftc.plugins.androidstudio.util.WeakReferenceSet;

/**
 * {@link AdbContext} maintains the statically available context about the state of
 * the debug bridge. This it does using listeners it registers with {@link com.android.ddmlib.AndroidDebugBridge}.
 * These listeners are never unregistered, as there is no good time to do so. Thus, this
 * instance here is never reclaimed. Ourselves, here, we delegate to weakly-held listeners so
 * we don't run afoul of that problem.
 */
@SuppressWarnings("WeakerAccess")
public class AdbContext
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "AdbContext";

    protected static class InstanceHolder
        {
        public static AdbContext theInstance = new AdbContext();
        }
    public static AdbContext getInstance() { return InstanceHolder.theInstance; }

    protected final ClientChangeListener clientChangeListener = new ClientChangeListener();
    protected final DeviceChangeListener deviceChangeListener = new DeviceChangeListener();
    protected final DebugBridgeChangeListener debugBridgeChangeListener = new DebugBridgeChangeListener();

    protected final Object lock = new Object();
    protected final WeakReferenceSet<AndroidDebugBridge.IClientChangeListener> clientChangeListeners = new WeakReferenceSet<>();
    protected final WeakReferenceSet<AndroidDebugBridge.IDeviceChangeListener> deviceChangeListeners = new WeakReferenceSet<>();
    protected final WeakReferenceSet<AndroidDebugBridge.IDebugBridgeChangeListener> bridgeChangeListeners = new WeakReferenceSet<>();
    protected       AndroidDebugBridge currentBridge;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    protected AdbContext()
        {
        currentBridge = null;
        addListeners();
        }

    protected void addListeners()
        {
        AndroidDebugBridge.addClientChangeListener(clientChangeListener);
        AndroidDebugBridge.addDeviceChangeListener(deviceChangeListener);
        AndroidDebugBridge.addDebugBridgeChangeListener(debugBridgeChangeListener);
        }

    protected void removeListeners()
        {
        AndroidDebugBridge.removeDebugBridgeChangeListener(debugBridgeChangeListener);
        AndroidDebugBridge.removeDeviceChangeListener(deviceChangeListener);
        AndroidDebugBridge.removeClientChangeListener(clientChangeListener);
        }

    //----------------------------------------------------------------------------------------------
    // Listeners
    //----------------------------------------------------------------------------------------------

    public void addClientChangeListener(AndroidDebugBridge.IClientChangeListener listener)
        {
        clientChangeListeners.add(listener);
        }
    public void removeClientChangeListener(AndroidDebugBridge.IClientChangeListener listener)
        {
        clientChangeListeners.remove(listener);
        }
    protected class ClientChangeListener implements AndroidDebugBridge.IClientChangeListener
        {
        @Override
        public void clientChanged(Client client, int changeMask)
            {
            EventLog.dd(TAG, "onClientChanged() client=%s mask=0x%08x", client.getClientData().getClientDescription(), changeMask);
            for (AndroidDebugBridge.IClientChangeListener listener : clientChangeListeners)
                {
                listener.clientChanged(client, changeMask);
                }
            }
        }

    public void addDeviceChangeListener(AndroidDebugBridge.IDeviceChangeListener listener)
        {
        deviceChangeListeners.add(listener);
        }
    public void removeDeviceChangeListener(AndroidDebugBridge.IDeviceChangeListener listener)
        {
        deviceChangeListeners.remove(listener);
        }
    protected class DeviceChangeListener implements AndroidDebugBridge.IDeviceChangeListener
        {
        @Override
        public void deviceConnected(IDevice device)
            {
            EventLog.dd(TAG, "deviceConnected() device=%s", device.getSerialNumber());
            for (AndroidDebugBridge.IDeviceChangeListener listener : deviceChangeListeners)
                {
                listener.deviceConnected(device);
                }
            }

        @Override
        public void deviceDisconnected(IDevice device)
            {
            EventLog.dd(TAG, "deviceDisconnected() device=%s", device.getSerialNumber());
            for (AndroidDebugBridge.IDeviceChangeListener listener : deviceChangeListeners)
                {
                listener.deviceDisconnected(device);
                }
            }

        @Override
        public void deviceChanged(IDevice device, int changeMask)
            {
            EventLog.dd(TAG, "deviceChanged() device=%s mask=0x%08x", device.getSerialNumber(), changeMask);
            for (AndroidDebugBridge.IDeviceChangeListener listener : deviceChangeListeners)
                {
                listener.deviceChanged(device, changeMask);
                }
            }
        }

    public void addBridgeChangeListener(AndroidDebugBridge.IDebugBridgeChangeListener listener)
        {
        if (bridgeChangeListeners.add(listener))
            {
            synchronized (lock)
                {
                if (currentBridge != null)
                    {
                    listener.bridgeChanged(currentBridge);
                    }
                }
            }
        }
    public void removeBridgeChangeListener(AndroidDebugBridge.IDebugBridgeChangeListener listener)
        {
        bridgeChangeListeners.remove(listener);
        }
    protected class DebugBridgeChangeListener implements AndroidDebugBridge.IDebugBridgeChangeListener
        {
        @Override
        public void bridgeChanged(AndroidDebugBridge bridge)
            {
            EventLog.dd(TAG, "bridgeChanged() bridge=%s", bridge);
            synchronized (lock)
                {
                currentBridge = bridge;
                }
            for (AndroidDebugBridge.IDebugBridgeChangeListener listener : bridgeChangeListeners)
                {
                listener.bridgeChanged(bridge);
                }
            }
        }
    }

