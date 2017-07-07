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

    protected final AdbContext adbContext = AdbContext.getInstance();
    protected final Project project;
    protected final AndroidDebugBridge androidDebugBridge;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public AdbClient(Project project)
        {
        this.project = project;
        //
        AndroidDebugBridge androidDebugBridge = null;
        try
            {
            androidDebugBridge = AndroidSdkUtils.getDebugBridge(project);
            if (null == androidDebugBridge)
                {
                EventLog.ee(TAG, project, "getDebugBridge() failed");
                }
            }
        catch (Throwable e)
            {
            EventLog.ee(TAG, project, e, "exception during getDebugBridge()");
            }
        this.androidDebugBridge = androidDebugBridge;
        }
    }
