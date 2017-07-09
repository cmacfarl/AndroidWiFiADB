package org.firstinspires.ftc.plugins.androidstudio.ui;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.firstinspires.ftc.plugins.androidstudio.Configuration;
import org.firstinspires.ftc.plugins.androidstudio.adb.AndroidDeviceDatabase;
import org.firstinspires.ftc.plugins.androidstudio.util.EventLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * http://www.jetbrains.org/intellij/sdk/docs/basics/plugin_structure/plugin_components.html
 * http://www.jetbrains.org/intellij/sdk/docs/basics/persisting_state_of_components.html
 */
@SuppressWarnings("WeakerAccess")
@State(name="FtcProjectComponent", storages = { @Storage(Configuration.XML_STATE_FILE_NAME) } )
public class FtcProjectComponentImpl implements FtcProjectComponent, PersistentStateComponent<AndroidDeviceDatabase.PersistentStateExternal>
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "FtcProjectComponent";

    protected final Project project;
    protected       AndroidDeviceDatabase database;
    protected       AndroidDeviceDatabase.PersistentStateExternal stagedState = null;

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public FtcProjectComponentImpl(Project project)
        {
        this.project = project;
        this.database = null;
        this.stagedState = null;
        }

    //----------------------------------------------------------------------------------------------
    // ProjectComponent
    //----------------------------------------------------------------------------------------------

    @Override public void initComponent()
        {
        EventLog.ii(TAG, "initComponent()");
        }

    @Override public void disposeComponent()
        {
        }

    @Override @NotNull
    public String getComponentName()
        {
        return getClass().getName();
        }

    @Override
    public void projectOpened()
        {
        EventLog.ii(TAG, "projectOpened()");
        database = new AndroidDeviceDatabase(project);
        if (stagedState != null)
            {
            database.loadPersistentState(stagedState);
            stagedState = null;
            }
        }

    @Override
    public void projectClosed()
        {
        EventLog.ii(TAG, "projectClosed()");
        }

    //----------------------------------------------------------------------------------------------
    // PersistentStateComponent
    // http://www.jetbrains.org/intellij/sdk/docs/basics/persisting_state_of_components.html
    // https://intellij-support.jetbrains.com/hc/en-us/community/posts/206794095-Where-is-ApplicationComponent-state-stored-in-
    //----------------------------------------------------------------------------------------------

    @Override @Nullable
    public AndroidDeviceDatabase.PersistentStateExternal getState()
        {
        return database == null ? new AndroidDeviceDatabase.PersistentStateExternal() : database.getPersistentState();
        }

    @Override public void loadState(AndroidDeviceDatabase.PersistentStateExternal persistentState)
        {
        if (database==null)
            {
            stagedState = persistentState;
            }
        else
            {
            stagedState = null;
            database.loadPersistentState(persistentState);
            }
        }

    /*@Override*/
    public void noStateLoaded()
        {
        loadState(new AndroidDeviceDatabase.PersistentStateExternal());
        }
    }
