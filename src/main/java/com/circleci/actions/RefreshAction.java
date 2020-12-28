package com.circleci.actions;

import com.circleci.ListLoader;
import com.circleci.CircleCIDataKeys;
import com.circleci.LoadRequests;
import com.circleci.i18n.CircleCIBundle;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class RefreshAction extends AnAction {

    public RefreshAction() {
        super(CircleCIBundle.messagePointer("refresh.action"),
                AllIcons.Actions.Refresh);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ListLoader listLoader = e.getDataContext().getData(CircleCIDataKeys.listLoaderKey);
        listLoader.load(LoadRequests.reload());
    }
}
