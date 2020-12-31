package com.circleci.ui;

import com.circleci.ActiveProjectChangeEvent;
import com.circleci.CircleCIEvents;
import com.circleci.CircleCIProjectSettings;
import com.circleci.CircleCISettings;
import com.circleci.api.JSON;
import com.circleci.api.model.Me;
import com.circleci.api.model.Project;
import com.circleci.api.Requests;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Strings;
import com.intellij.concurrency.JobScheduler;
import com.intellij.notification.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.Comparing;
import com.intellij.ui.*;
import com.intellij.ui.components.JBList;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author Chris Kowalski
 */
public class CircleCIProjectsConfigurable implements Configurable {
    private JPanel mainPanel;
    private JPanel projectsPanel;
    private JTextField token;
    private JTextField organization;
    private ComboBox<String> provider;
    private JTextField serverUrl;
    private JButton testConnectionButton;
    private JLabel connectionResultLabel;
    private JBList<Project> projectList;

    private CircleCISettings settings;
    private CircleCIProjectSettings projectSettings;

    private com.intellij.openapi.project.Project projectIntellij;

    public CircleCIProjectsConfigurable(com.intellij.openapi.project.Project projectIntellij) {
        settings = CircleCISettings.getInstance();
        projectSettings = CircleCIProjectSettings.getInstance(projectIntellij);
        this.projectIntellij = projectIntellij;

        projectList = new JBList<>(new CollectionListModel<>(projectSettings.projects));
        projectList.getEmptyText().setText("No projects");
        projectList.setCellRenderer(new ColoredListCellRenderer<Project>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends Project> list, Project value,
                                                 int index, boolean selected, boolean hasFocus) {
                append(value.toString());
            }
        });

        provider.setModel(new CollectionComboBoxModel<>(Arrays.asList("Github", "Bitbucket")));
        provider.setSelectedItem(null);

        restoreFromSettings();

        ToolbarDecorator toolbarDecorator = ToolbarDecorator.createDecorator(projectList).disableUpDownActions();
        projectsPanel.add(toolbarDecorator.createPanel());
        toolbarDecorator.setRemoveAction(anActionButton -> {
            Project selectedValue = projectList.getSelectedValue();
            CollectionListModel<Project> model = (CollectionListModel<Project>) projectList.getModel();
            model.remove(selectedValue);
            projectSettings.projects.remove(selectedValue);

            if (selectedValue.equals(projectSettings.activeProject)) {
                Project current = null;
                if (model.getSize() > 0) {
                    current = model.getElementAt(0);
                }

                projectIntellij.getMessageBus()
                        .syncPublisher(CircleCIEvents.PROJECT_CHANGED_TOPIC)
                        .projectChanged(new ActiveProjectChangeEvent(projectSettings.activeProject, current));
            }
        });

        testConnectionButton.addActionListener(e -> {
            JobScheduler.getScheduler().schedule(() -> {
                try {
                    Me me = JSON.fromJson(Requests.getMe(serverUrl.getText(), token.getText()).readString(),
                            new TypeReference<Me>() {
                            });
                    connectionResultLabel.setText("Success.");
                    connectionResultLabel.setForeground(JBColor.GREEN);
                } catch (Exception ex) {
                    connectionResultLabel.setForeground(JBColor.RED);
                    String message = "Exception on connecting to the server.";
                    connectionResultLabel.setText(message);

                    Notifications.Bus.notify(new Notification("CircleCI",
                            message, ex.getMessage(), NotificationType.ERROR));
                }
            }, 0, TimeUnit.SECONDS);
        });

        serverUrl.addKeyListener(new CredentialsKeyListener());
        token.addKeyListener(new CredentialsKeyListener());

    }

    private void restoreFromSettings() {
        serverUrl.setText(settings.serverUrl);
        token.setText(settings.token);
        organization.setText(settings.defaultOrganization);

        if ("Bitbucket".equals(settings.defaultProvider)) {
            provider.setSelectedItem("Bitbucket");
        } else if ("Github".equals(settings.defaultProvider)) {
            provider.setSelectedItem("Github");
        }

        if (!"".equals(serverUrl.getText()) && !"".equals(token.getText())) {
            testConnectionButton.setEnabled(true);
        } else {
            testConnectionButton.setEnabled(false);
        }
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Projects";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return mainPanel;
    }

    @Override
    public boolean isModified() {
        return !Comparing.strEqual(settings.serverUrl, serverUrl.getText()) ||
                !Comparing.strEqual(settings.token, token.getText()) ||
                !Comparing.strEqual(settings.defaultOrganization, organization.getText()) ||
                !Comparing.strEqual(settings.defaultProvider, (String) provider.getSelectedItem());
    }

    @Override
    public void apply() {
        if (serverUrl.getText().endsWith("/")) {
            settings.serverUrl = StringUtils.chop(serverUrl.getText());
        } else {
            settings.serverUrl = serverUrl.getText();
        }

        settings.token = token.getText();
        settings.defaultOrganization = organization.getText();
        settings.defaultProvider = (String) provider.getSelectedItem();

        if (settings.serverUrl != null && settings.token != null) {
            ApplicationManager.getApplication()
                    .getMessageBus()
                    .syncPublisher(CircleCIEvents.SETTINGS_UPDATED_TOPIC)
                    .settingsUpdate(settings);
        }
    }

    private class CredentialsKeyListener implements KeyListener {
        @Override
        public void keyTyped(KeyEvent e) {

        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (!Strings.isNullOrEmpty(connectionResultLabel.getText())) {
                connectionResultLabel.setText("");
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (!"".equals(serverUrl.getText()) && !"".equals(token.getText())) {
                testConnectionButton.setEnabled(true);
            } else {
                testConnectionButton.setEnabled(false);
            }
        }
    }

}
