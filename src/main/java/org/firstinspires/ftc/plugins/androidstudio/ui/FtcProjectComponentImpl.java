package org.firstinspires.ftc.plugins.androidstudio.ui;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.firstinspires.ftc.plugins.androidstudio.adb.AdbForProject;
import org.firstinspires.ftc.plugins.androidstudio.Configuration;
import org.firstinspires.ftc.plugins.androidstudio.util.EventLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @see <a href="http://www.jetbrains.org/intellij/sdk/docs/basics/plugin_structure/plugin_components.html">components</a>
 */
@SuppressWarnings("WeakerAccess")
@State(name="FtcProjectComponent", storages = { @Storage(file = Configuration.XML_STATE_FILE_NAME) } )
public class FtcProjectComponentImpl implements FtcProjectComponent, PersistentStateComponent<FtcProjectComponentImpl.State>
    {
    //----------------------------------------------------------------------------------------------
    // State
    //----------------------------------------------------------------------------------------------

    public static final String TAG = "FtcProjectComponent";

    protected final Project project;
    protected State state;
    protected AdbForProject adbForProject;

    // http://www.jetbrains.org/intellij/sdk/docs/basics/persisting_state_of_components.html
    public static class State
        {
        public String value;
        }

    //----------------------------------------------------------------------------------------------
    // Construction
    //----------------------------------------------------------------------------------------------

    public FtcProjectComponentImpl(Project project)
        {
        this.project = project;
        this.state = new State();
        this.state.value = "barney";
        }

    //----------------------------------------------------------------------------------------------
    // ProjectComponent
    //----------------------------------------------------------------------------------------------

    @Override public void initComponent()
        {
        EventLog.ii(TAG, project, "initComponent()");
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
        EventLog.ii(TAG, project, "projectOpened()");
        this.adbForProject = new AdbForProject(this.project);
        }

    @Override
    public void projectClosed()
        {
        EventLog.ii(TAG, project, "projectClosed()");
        }

    //----------------------------------------------------------------------------------------------
    // PersistentStateComponent
    // http://www.jetbrains.org/intellij/sdk/docs/basics/persisting_state_of_components.html
    // https://intellij-support.jetbrains.com/hc/en-us/community/posts/206794095-Where-is-ApplicationComponent-state-stored-in-
    //----------------------------------------------------------------------------------------------

    @Override @Nullable
    public State getState()
        {
        EventLog.ii(TAG, project, "getState()");
        return state;
        }

    @Override public void loadState(State state)
        {
        EventLog.ii(TAG, project, "loadState() value=%s", state.value);
        this.state = state;
        }
    }
