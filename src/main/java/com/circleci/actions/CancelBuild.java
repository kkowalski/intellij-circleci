package com.circleci.actions;

import com.circleci.CircleCIDataKeys;
import com.circleci.api.Requests;
import com.circleci.api.model.Build;
import com.intellij.concurrency.JobScheduler;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author Chris Kowalski
 */
public class CancelBuild extends AnAction {
    public CancelBuild() {
        super("Cancel Build");
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        Build build = event.getDataContext().getData(CircleCIDataKeys.listSelectedBuildKey);
        JobScheduler.getScheduler().schedule(() -> {
            try {
                String textResponse = Requests.cancelBuild(build).forceHttps(true).readString();
            } catch (IOException ex) {
                Notifications.Bus.notify(new Notification("CircleCI",
                        "Error cancelling build", ex.getMessage(), NotificationType.ERROR));
            }
        }, 0, TimeUnit.SECONDS);
    }
}