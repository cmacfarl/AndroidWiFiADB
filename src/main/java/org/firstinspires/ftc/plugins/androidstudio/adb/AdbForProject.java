package org.firstinspires.ftc.plugins.androidstudio.adb;

import com.android.ddmlib.AndroidDebugBridge;
import com.intellij.openapi.project.Project;

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

    protected AndroidDebugBridge currentBridge;
    protected AndroidDeviceDatabase database;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public AdbForProject(Project project)
        {
        this.project = project;
        this.currentBridge = null;
        this.adbContext = AdbContext.getInstance();
        this.database = new AndroidDeviceDatabase(project, adbContext);
        this.adbContext.addBridgeChangeListener(bridgeChangeListener);
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
                    // ....
                    }
                }
            }
        }
    }
