package com.circleci.ui;

import com.circleci.ActiveProjectChangeEvent;
import com.circleci.CircleCIEvents;
import com.circleci.CircleCISettings;
import com.circleci.api.JSON;
import com.circleci.api.model.Project;
import com.circleci.api.model.ProjectApiV2;
import com.circleci.api.Requests;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Strings;
import com.intellij.concurrency.JobScheduler;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.JBColor;
import com.intellij.util.io.HttpRequests;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class CircleCIDialog extends DialogWrapper {
    private JPanel mainPanel;
    private JTextField organization;
    private ComboBox<String> provider;
    private JTextField project;
    private JLabel projectStatusLabel;
    private JButton checkProjectButton;

    private CircleCISettings settings;

    public CircleCIDialog(DataContext dataContext) {
        super(true); // use current window as parent
        setTitle("Add CircleCI Project");
        init();

        project.requestFocus();

        settings = CircleCISettings.getInstance();
        restoreFromSettings();

        provider.setModel(new CollectionComboBoxModel<>(Arrays.asList("Github", "Bitbucket")));

        setOKActionEnabled(false);

        organization.addKeyListener(new ProjectNameListener());
        project.addKeyListener(new ProjectNameListener());

        checkProjectButton.setEnabled(false);
        checkProjectButton.addActionListener(event -> {
            if (!"".equals(organization.getText()) && !"".equals(project.getText())) {
                Project proj = new Project(organization.getText(), project.getText(), (String) provider.getSelectedItem());
                if (alreadyAdded(proj)) {
                    projectStatusLabel.setText("Already added.");
                    projectStatusLabel.setForeground(JBColor.red);
                    setOKActionEnabled(false);
                } else {
                    JobScheduler.getScheduler().schedule(() -> {
                        try {
                            ProjectApiV2 checkedProject = JSON.fromJsonString(Requests.getProject(proj).readString(),
                                    new TypeReference<ProjectApiV2>() {
                                    });
                        } catch (Exception ex) {
                            if (ex instanceof HttpRequests.HttpStatusException &&
                                    ((HttpRequests.HttpStatusException) ex).getStatusCode() == 404) {
                                projectStatusLabel.setText("Not found.");
                            } else {
                                projectStatusLabel.setText("Error.");
                                Notifications.Bus.notify(new Notification("CircleCI",
                                        "Error checking project", ex.getMessage(), NotificationType.ERROR));
                            }
                            projectStatusLabel.setForeground(JBColor.RED);
                            return;
                        }

                        projectStatusLabel.setText("Project found.");
                        projectStatusLabel.setForeground(JBColor.green);
                        setOKActionEnabled(true);
                    }, 0, TimeUnit.SECONDS);
                }
            }
        });
    }

    private void restoreFromSettings() {
        organization.setText(settings.defaultOrganization);
        provider.setSelectedItem(settings.defaultProvider);

        if ("Github".equals(settings.defaultProvider)) {
            provider.setSelectedItem("Github");
        } else {
            provider.setSelectedItem("Bitbucket");
        }
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return mainPanel;
    }

    @Override
    protected void doOKAction() {
        Project project = new Project(this.organization.getText(),
                this.project.getText(), (String) provider.getSelectedItem());
        settings.projects.add(project);

        ApplicationManager.getApplication()
                .getMessageBus()
                .syncPublisher(CircleCIEvents.PROJECT_CHANGED_TOPIC)
                .projectChanged(new ActiveProjectChangeEvent(settings.activeProject, project));

        super.doOKAction();
    }

    private class ProjectNameListener implements KeyListener {
        @Override
        public void keyTyped(KeyEvent e) {
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (!Strings.isNullOrEmpty(projectStatusLabel.getText())) {
                projectStatusLabel.setText("");
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            setOKActionEnabled(false);
            if (!"".equals(organization.getText()) && !"".equals(project.getText())) {
                checkProjectButton.setEnabled(true);
            } else {
                checkProjectButton.setEnabled(false);
            }
        }
    }

    boolean alreadyAdded(Project project) {
        return settings.projects.contains(project);
    }

}
