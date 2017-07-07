package org.firstinspires.ftc.plugins.androidstudio.adb;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.intellij.openapi.project.Project;
import org.firstinspires.ftc.plugins.androidstudio.adb.commands.GetPropCommand;
import org.firstinspires.ftc.plugins.androidstudio.util.EventLog;
import org.jetbrains.android.sdk.AndroidSdkUtils;

import java.util.Map;

/**
 * Created by bob on 2017-07-04.
 */
@SuppressWarnings("WeakerAccess")
public class AdbForProject
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "AdbForProject";

    protected final Object lock = new Object();
    protected final Project project;
    protected final AdbContext adbContext;
    protected final BridgeChangeListener bridgeChangeListener = new BridgeChangeListener();
    protected final DeviceChangeListener deviceChangeListener = new DeviceChangeListener();

    protected AndroidDebugBridge currentBridge;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public AdbForProject(Project project)
        {
        this.project = project;
        this.currentBridge = null;
        this.adbContext = AdbContext.getInstance();
        this.adbContext.addBridgeChangeListener(bridgeChangeListener);
        this.adbContext.addDeviceChangeListener(deviceChangeListener);
        }

    protected void refreshBridge()
        {
        synchronized (lock)
            {
            currentBridge = null;
            try
                {
                currentBridge = AndroidSdkUtils.getDebugBridge(project);
                if (null == currentBridge)
                    {
                    EventLog.ee(TAG, project, "getDebugBridge() failed");
                    }
                }
            catch (Throwable e)
                {
                EventLog.ee(TAG, project, e, "exception during getDebugBridge()");
                }
            }
        }

    protected class DeviceChangeListener implements AndroidDebugBridge.IDeviceChangeListener
        {
        @Override
        public void deviceConnected(IDevice device)
            {
            GetPropCommand command = new GetPropCommand();
            if (command.execute(device))
                {
                for (Map.Entry<String,String> pair : command.getProperties().entrySet())
                    {
                    EventLog.ii(TAG, "key=%s value=%s", pair.getKey(), pair.getValue());
                    }
                }
            }

        @Override
        public void deviceDisconnected(IDevice device)
            {
            }

        @Override
        public void deviceChanged(IDevice device, int changeMask)
            {
            }
        }


    protected void onNewBridge()
        {
        }

    protected class BridgeChangeListener implements AndroidDebugBridge.IDebugBridgeChangeListener
        {
        /** We get called both for creations and disconnects, and we don't know how to
         * distinguish between the two. So we don't. */
        @Override public void bridgeChanged(AndroidDebugBridge bridge)
            {
            synchronized (lock)
                {
                AndroidDebugBridge oldBridge = currentBridge;
                currentBridge = bridge;
                if (currentBridge != oldBridge)
                    {
                    onNewBridge();
                    }
                }
            }
        }
    }
