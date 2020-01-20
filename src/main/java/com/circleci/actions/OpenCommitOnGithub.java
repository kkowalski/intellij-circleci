package com.circleci.actions;

import com.circleci.CircleCIDataKeys;
import com.circleci.api.model.Build;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class OpenCommitOnGithub extends AnAction {
    public OpenCommitOnGithub() {
        super("Open Commit on Github");
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        Build build = event.getDataContext().getData(CircleCIDataKeys.listSelectedBuildKey);
        if (build.getVcsType().equals("github") && build.getAllCommitDetails() != null && build.getAllCommitDetails().size() > 0
                && build.getAllCommitDetails().get(0).getCommitUrl() != null) {
            event.getPresentation().setEnabled(true);
        } else {
            event.getPresentation().setEnabled(false);
        }
        super.update(event);
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        Build build = event.getDataContext().getData(CircleCIDataKeys.listSelectedBuildKey);
        BrowserUtil.browse(build.getAllCommitDetails().get(0).getCommitUrl());
    }
}