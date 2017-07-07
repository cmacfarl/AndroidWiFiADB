package org.firstinspires.ftc.plugins.androidstudio.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import org.firstinspires.ftc.plugins.androidstudio.util.EventLog;

/**
 * Created by bob on 2017-07-03.
 */
@SuppressWarnings("WeakerAccess")
public class HelloAction extends AnAction
    {
    public static final String TAG = "HelloAction";

    @Override
    public void actionPerformed(AnActionEvent e)
        {
        String message = "Hello from FTC!";
        Messages.showInfoMessage("Hello World!", message);
        EventLog.ii(TAG, message);
        }

    @Override
    public void update(AnActionEvent e)
        {
        super.update(e);
        e.getPresentation().setIcon(AllIcons.Ide.Info_notifications);
        }
    }
