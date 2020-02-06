package com.circleci.actions;

import com.circleci.CircleCIEvents;
import com.circleci.CircleCISettings;
import com.circleci.api.model.Project;
import com.circleci.ActiveProjectChangeEvent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.CollectionComboBoxModel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author Chris Kowalski
 */
public class CircleCIProjectComboBox extends AnAction implements CustomComponentAction {

    @Override
    @NotNull
    public JComponent createCustomComponent(@NotNull Presentation presentation, @NotNull String place) {
        CircleCISettings settings = CircleCISettings.getInstance();

        ComboBoxModel<Project> model = new CollectionComboBoxModel<>(settings.projects);
        model.setSelectedItem(settings.activeProject);
        ComboBox<Project> comboBox = new ComboBox<>(model, 200);

        ApplicationManager.getApplication().getMessageBus()
                .connect()
                .subscribe(CircleCIEvents.PROJECT_CHANGED_TOPIC, event -> {
                    if (event.getPrevious() == event.getCurrent()) {
                        return;
                    }
                    model.setSelectedItem(event.getCurrent());
                });

        comboBox.addActionListener(event -> {
            ApplicationManager.getApplication()
                    .getMessageBus()
                    .syncPublisher(CircleCIEvents.PROJECT_CHANGED_TOPIC)
                    .projectChanged(new ActiveProjectChangeEvent(settings.activeProject, (Project) model.getSelectedItem()));
        });

        return comboBox;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

    }
}
