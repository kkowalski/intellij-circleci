package com.circleci.actions;

import com.circleci.CircleCIDataKeys;
import com.circleci.api.Requests;
import com.circleci.api.model.Build;
import com.intellij.concurrency.JobScheduler;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author Chris Kowalski
 */
public class RerunBuild extends com.intellij.openapi.actionSystem.AnAction {

    public RerunBuild() {
        super("Rerun Build");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Build build = event.getDataContext().getData(CircleCIDataKeys.listSelectedBuildKey);
        JobScheduler.getScheduler().schedule(() -> {
            try {
                String textResponse = Requests.retryBuild(build).forceHttps(true).readString();
            } catch (IOException ex) {
                Notifications.Bus.notify(new Notification("CircleCI",
                        "Error retrying build", ex.getMessage(), NotificationType.ERROR));
            }
        }, 0, TimeUnit.SECONDS);
    }
}
