package com.circleci.actions;

import com.circleci.CircleCIEvents;
import com.circleci.CircleCIProjectSettings;
import com.circleci.api.model.Project;
import com.circleci.ActiveProjectChangeEvent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.CollectionComboBoxModel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author Chris Kowalski
 */
public class CircleCIProjectComboBox extends AnAction implements CustomComponentAction {
    private com.intellij.openapi.project.Project projectIntellij;

    public CircleCIProjectComboBox(com.intellij.openapi.project.Project projectIntellij) {
        this.projectIntellij = projectIntellij;
    }

    @Override
    @NotNull
    public JComponent createCustomComponent(@NotNull Presentation presentation, @NotNull String place) {
        CircleCIProjectSettings projectSettings = CircleCIProjectSettings.getInstance(projectIntellij);

        ComboBoxModel<Project> model = new CollectionComboBoxModel<>(projectSettings.projects);
        model.setSelectedItem(projectSettings.activeProject);
        ComboBox<Project> comboBox = new ComboBox<>(model, 200);

        projectIntellij.getMessageBus()
                .connect()
                .subscribe(CircleCIEvents.PROJECT_CHANGED_TOPIC, event -> {
                    if (event.getPrevious() == event.getCurrent()) {
                        return;
                    }
                    model.setSelectedItem(event.getCurrent());
                });

        comboBox.addActionListener(event -> {
            projectIntellij.getMessageBus()
                    .syncPublisher(CircleCIEvents.PROJECT_CHANGED_TOPIC)
                    .projectChanged(new ActiveProjectChangeEvent(projectSettings.activeProject, (Project) model.getSelectedItem()));
        });

        return comboBox;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

    }
}
