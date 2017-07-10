package org.firstinspires.ftc.plugins.androidstudio.ui;

import com.android.tools.idea.ddms.adb.AdbService;
import com.android.tools.idea.fd.InstantRunConfiguration;
import com.google.gson.Gson;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.firstinspires.ftc.plugins.androidstudio.Configuration;
import org.firstinspires.ftc.plugins.androidstudio.adb.AndroidDeviceDatabase;
import org.firstinspires.ftc.plugins.androidstudio.util.EventLog;
import org.firstinspires.ftc.plugins.androidstudio.util.ThreadPool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * http://www.jetbrains.org/intellij/sdk/docs/basics/plugin_structure/plugin_components.html
 * http://www.jetbrains.org/intellij/sdk/docs/basics/persisting_state_of_components.html
 */
@SuppressWarnings("WeakerAccess")
@State(name="FtcProjectComponent", storages = { @Storage(Configuration.XML_STATE_FILE_NAME) } )
public class FtcProjectComponentImpl implements FtcProjectComponent, PersistentStateComponent<FtcProjectComponentImpl.PersistentStateExternal>
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "FtcProjectComponent";

    protected final Project project;
    protected       boolean disabledInstantRun;
    protected       AndroidDeviceDatabase database;
    protected       AndroidDeviceDatabase.PersistentState stagedState = null;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public FtcProjectComponentImpl(Project project)
        {
        this.project = project;
        this.disabledInstantRun = false;
        this.database = null;
        this.stagedState = null;
        }

    //----------------------------------------------------------------------------------------------
    // ProjectComponent
    //----------------------------------------------------------------------------------------------

    @Override public void initComponent()
        {
        EventLog.dd(TAG, "initComponent()");
        }

    @Override public void disposeComponent()
        {
        if (database != null)
            {
            database.dispose();
            }
        }

    @Override @NotNull
    public String getComponentName()
        {
        return getClass().getName();
        }

    @Override
    public void projectOpened()
        {
        EventLog.dd(TAG, "projectOpened()");
        database = new AndroidDeviceDatabase(project);
        if (stagedState != null)
            {
            database.loadPersistentState(stagedState);
            stagedState = null;
            }

        startAdbServer();
        }

    protected void onStateLoaded()
        {
        if (!disabledInstantRun)
            {
            disableInstantRun();
            disabledInstantRun = true;
            }
        }

    protected void disableInstantRun()
        {
        // There are two sets of settings. You'd think we'd want the project-centric ones, but
        // the Android Studio UI seems only to manipulate the global one. So we do both, just to
        // be sure.
        EventLog.dd(TAG, "disabling InstantRun");
        InstantRunConfiguration.getInstance().INSTANT_RUN = false;
        InstantRunConfiguration.getInstance(project).INSTANT_RUN = false;
        }

    protected void startAdbServer()
        {
        /** Asynchronously (so as not to block the UI ) start up ADB if it's not already started */
        ThreadPool.getDefault().execute(() ->
            {
            File path = database.getHostAdb().getAdb();
            EventLog.dd(TAG, "fetching/starting ADB: %s", path);
			AdbService.getInstance().getDebugBridge(path);  // ignore returned future: we just want to kick-start the adb server
            });
        }

    @Override
    public void projectClosed()
        {
        EventLog.dd(TAG, "projectClosed()");
        }

    //----------------------------------------------------------------------------------------------
    // PersistentStateComponent
    // http://www.jetbrains.org/intellij/sdk/docs/basics/persisting_state_of_components.html
    // https://intellij-support.jetbrains.com/hc/en-us/community/posts/206794095-Where-is-ApplicationComponent-state-stored-in-
    //----------------------------------------------------------------------------------------------

    /** A hack: not XML native, but an JSON dump living in an XML attribute, but it works. And
     * blimy if we just can't get the default serialization to work with anything complicated.
     * So ***** 'em. */
    public static class PersistentStateExternal
        {
        public String json = null;
        public PersistentStateExternal() { }
        public PersistentStateExternal(PersistentState proto)
            {
            this.json = new Gson().toJson(proto);
            }
        }

    public static class PersistentState
        {
        boolean disabledInstantRun = false;
        AndroidDeviceDatabase.PersistentState databaseState;

        public static PersistentState from(PersistentStateExternal persistentStateExternal)
            {
            if (persistentStateExternal.json==null)
                {
                return new PersistentState();
                }
            else
                {
                return new Gson().fromJson(persistentStateExternal.json, PersistentState.class);
                }
            }
        }

    @Override @Nullable
    public PersistentStateExternal getState()
        {
        PersistentState persistentState = new PersistentState();
        persistentState.disabledInstantRun = disabledInstantRun;
        persistentState.databaseState = database==null
                ? null
                : database.getPersistentState();
        return new PersistentStateExternal(persistentState);
        }

    @Override public void loadState(PersistentStateExternal persistentStateExternal)
        {
        PersistentState persistentState = PersistentState.from(persistentStateExternal);

        disabledInstantRun = persistentState.disabledInstantRun;
        if (database==null)
            {
            stagedState = persistentState.databaseState;
            }
        else
            {
            stagedState = null;
            database.loadPersistentState(persistentState.databaseState);
            }

        onStateLoaded();
        }

    /*@Override*/
    public void noStateLoaded()
        {
        loadState(new PersistentStateExternal());
        }
    }
