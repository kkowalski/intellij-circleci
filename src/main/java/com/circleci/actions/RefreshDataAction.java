package com.circleci.actions;

import com.circleci.BuildListLoader;
import com.circleci.CircleCIDataKeys;
import com.circleci.LoadRequests;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class RefreshDataAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        BuildListLoader listLoader = e.getDataContext().getData(CircleCIDataKeys.listLoaderKey);
        listLoader.load(LoadRequests.refresh());
    }
}
