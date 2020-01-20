package com.circleci.actions;

import com.circleci.CircleCIDataKeys;
import com.circleci.api.model.Build;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

/**
 * @author Chris Kowalski
 */
public class OpenBuild extends AnAction {
    public OpenBuild() {
        super("Open Build in Browser");
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        Build build = event.getDataContext().getData(CircleCIDataKeys.listSelectedBuildKey);
        BrowserUtil.browse(build.getUrl());
    }
}