package org.firstinspires.ftc.plugins.androidstudio;

import com.android.ddmlib.AndroidDebugBridge;
import com.intellij.openapi.project.Project;
import org.jetbrains.android.sdk.AndroidSdkUtils;

/**
 * Created by bob on 2017-07-04.
 */
@SuppressWarnings("WeakerAccess")
public class AdbClient
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "AdbClient";

    final Project project;
    AndroidDebugBridge androidDebugBridge;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public AdbClient(Project project)
        {
        this.project = project;
        try
            {
            this.androidDebugBridge = AndroidSdkUtils.getDebugBridge(project);
            if (null == this.androidDebugBridge)
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
