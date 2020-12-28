package com.circleci.actions;

import com.circleci.i18n.CircleCIBundle;
import com.circleci.ui.CircleCIDialog;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

/**
 * @author Chris Kowalski
 */
public class AddProjectDialog extends AnAction {

    public AddProjectDialog() {
        super(CircleCIBundle.messagePointer("add.project.action"),
                AllIcons.General.Add);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final CircleCIDialog dialog = new CircleCIDialog(e.getProject());
        dialog.showAndGet();
    }

}
