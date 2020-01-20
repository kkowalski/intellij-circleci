package com.circleci.actions;

import com.circleci.CircleCIDataKeys;
import com.circleci.CircleCISettings;
import com.circleci.api.model.Build;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

/**
 * @author Chris Kowalski
 */
public class OpenWorkflow extends AnAction {
    public OpenWorkflow() {
        super("Open Workflow in Browser");
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        Build build = event.getDataContext().getData(CircleCIDataKeys.listSelectedBuildKey);
        if (build.getWorkflows() != null) {
            event.getPresentation().setEnabled(true);
        } else {
            event.getPresentation().setEnabled(false);
        }
        super.update(event);
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        CircleCISettings settings = CircleCISettings.getInstance();
        Build build = event.getDataContext().getData(CircleCIDataKeys.listSelectedBuildKey);
        BrowserUtil.browse(settings.serverUrl + "/workflow-run/" + build.getWorkflows().getId());
    }
}