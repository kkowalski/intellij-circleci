package com.circleci.ui;

import com.circleci.ActiveProjectChangeEvent;
import com.circleci.CircleCIEvents;
import com.circleci.CircleCIProjectSettings;
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
    private JTextField organizationField;
    private ComboBox<String> providerComboBox;
    public JTextField projectField;
    private JLabel projectStatusLabel;
    private JButton checkProjectButton;

    private CircleCISettings settings;
    private CircleCIProjectSettings projectSettings;
    private com.intellij.openapi.project.Project projectIntellij;


    public CircleCIDialog(com.intellij.openapi.project.Project projectIntellij) {
        super(true); // use current window as parent
        setTitle("Add CircleCI Project");
        init();
        this.projectIntellij = projectIntellij;

        settings = CircleCISettings.getInstance();
        projectSettings = CircleCIProjectSettings.getInstance(projectIntellij);

        providerComboBox.setModel(new CollectionComboBoxModel<>(Arrays.asList("Github", "Bitbucket")));

        restoreFromSettings();

        setOKActionEnabled(false);

        organizationField.addKeyListener(new ProjectNameListener());
        projectField.addKeyListener(new ProjectNameListener());

        checkProjectButton.setEnabled(false);
        checkProjectButton.addActionListener(event -> {
            if (!"".equals(organizationField.getText()) && !"".equals(projectField.getText())) {
                Project proj = new Project(organizationField.getText(), projectField.getText(), (String) providerComboBox.getSelectedItem());
                if (alreadyAdded(proj)) {
                    projectStatusLabel.setText("Already added.");
                    projectStatusLabel.setForeground(JBColor.red);
                    setOKActionEnabled(false);
                } else {
                    JobScheduler.getScheduler().schedule(() -> {
                        try {
                            ProjectApiV2 checkedProject = JSON.fromJson(Requests.getProject(proj).readString(),
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
        organizationField.setText(settings.defaultOrganization);
        if ("Github".equals(settings.defaultProvider)) {
            providerComboBox.setSelectedItem("Github");
        } else {
            providerComboBox.setSelectedItem("Bitbucket");
        }
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return mainPanel;
    }

    @Override
    protected void doOKAction() {
        Project project = new Project(this.organizationField.getText(),
                projectField.getText(), (String) providerComboBox.getSelectedItem());
        projectSettings.projects.add(project);

        projectIntellij.getMessageBus()
                .syncPublisher(CircleCIEvents.PROJECT_CHANGED_TOPIC)
                .projectChanged(new ActiveProjectChangeEvent(projectSettings.activeProject, project));

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
            if (!"".equals(organizationField.getText()) && !"".equals(projectField.getText())) {
                checkProjectButton.setEnabled(true);
            } else {
                checkProjectButton.setEnabled(false);
            }
        }
    }

    boolean alreadyAdded(Project project) {
        return projectSettings.projects.contains(project);
    }

}
