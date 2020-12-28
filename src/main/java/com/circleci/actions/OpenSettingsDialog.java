package com.circleci.actions;

import com.circleci.i18n.CircleCIBundle;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Chris Kowalski
 */
public class OpenSettingsDialog extends AnAction {

    public OpenSettingsDialog() {
        super(CircleCIBundle.messagePointer("open.settings.action"),
                AllIcons.General.Settings);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ShowSettingsUtil.getInstance().showSettingsDialog(e.getProject(),"CircleCI");
    }

}
