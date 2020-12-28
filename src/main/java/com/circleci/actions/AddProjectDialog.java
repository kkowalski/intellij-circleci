package com.circleci.actions;

import com.circleci.ui.CircleCIDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

/**
 * @author Chris Kowalski
 */
public class AddProjectDialog extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final CircleCIDialog dialog = new CircleCIDialog(e.getProject());
        dialog.showAndGet();
    }

}
