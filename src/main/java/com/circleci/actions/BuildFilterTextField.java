package com.circleci.actions;

import com.circleci.CircleCIEvents;
import com.circleci.CircleCIProjectSettings;
import com.circleci.i18n.CircleCIBundle;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.ui.TextFieldWithHistory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class BuildFilterTextField extends AnAction implements CustomComponentAction {
    private final com.intellij.openapi.project.Project projectIntellij;

    public BuildFilterTextField(com.intellij.openapi.project.Project projectIntellij) {
        this.projectIntellij = projectIntellij;
    }

    @Override
    @NotNull
    public JComponent createCustomComponent(@NotNull Presentation presentation, @NotNull String place) {
        CircleCIProjectSettings projectSettings = CircleCIProjectSettings.getInstance(projectIntellij);

        TextFieldWithHistory field = new TextFieldWithHistory();
        field.setText(projectSettings.buildFilter);
        field.setToolTipText(CircleCIBundle.message("build.filter.action"));
        field.setMinimumAndPreferredWidth(200);

        projectIntellij.getMessageBus()
                .connect()
                .subscribe(CircleCIEvents.PROJECT_CHANGED_TOPIC, event -> {
                    if (event.getPrevious() != event.getCurrent()) {
                        field.setText("");
                    }
                });

        field.addActionListener(event -> {
            projectSettings.buildFilter = field.getText();
            field.addCurrentTextToHistory();

            projectIntellij
                    .getMessageBus()
                    .syncPublisher(CircleCIEvents.BUILD_FILTER_CHANGED_TOPIC)
                    .buildFilterChanged();
        });

        return field;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
    }
}
