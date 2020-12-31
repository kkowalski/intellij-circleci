package com.circleci.ui;

import com.circleci.*;
import com.circleci.actions.CircleCIProjectComboBox;
import com.circleci.api.model.Build;
import com.circleci.ui.list.BuildList;
import com.intellij.concurrency.JobScheduler;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.*;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBLoadingPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.components.BorderLayoutPanel;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.TimeUnit;

public class CircleCIToolWindow extends SimpleToolWindowPanel {

    private CircleCISettings settings;
    private CircleCIProjectSettings projectSettings;
    private Disposable parentDisposable;

    public CircleCIToolWindow(Project project, ToolWindow toolWindow, Disposable parentDisposable) {
        super(true);
        this.parentDisposable = parentDisposable;
        this.settings = CircleCISettings.getInstance();
        this.projectSettings = CircleCIProjectSettings.getInstance(project);
    }

    public void init(Project project) {
        CollectionListModel<Build> listModel = new CollectionListModel<>();
        ListChangeChecker listChangeChecker = new ListChangeChecker(project, projectSettings, listModel);
        ListLoader listLoader = new ListLoader(listModel, listChangeChecker, project);

        BorderLayoutPanel content = JBUI.Panels.simplePanel();
        JBLoadingPanel loadingPanel = new LoadingPanel(parentDisposable, content, listLoader);
        JPanel openSettingsPanel = new OpenSettingsPanel(project);

        openSettingsPanelIfNotConfigured(loadingPanel, openSettingsPanel);

        ApplicationManager.getApplication().getMessageBus()
                .connect().subscribe(CircleCIEvents.SETTINGS_UPDATED_TOPIC, state -> {
            if ("".equals(settings.token) || "".equals(settings.serverUrl)) {
                return;
            }
            removeAll();
            setContent(loadingPanel);
        });

        JBList<Build> list = new BuildList(listModel);
        list.setDataProvider(dataId -> {
            if (dataId.equals(CircleCIDataKeys.listSelectedBuildKey.getName())) {
                return list.getSelectedValue();
            } else if (dataId.equals(CircleCIDataKeys.listLoaderKey.getName())) {
                return listLoader;
            } else {
                return null;
            }
        });

        ActionToolbar toolbar = createToolbar(project);
        toolbar.setTargetComponent(list);

        BorderLayoutPanel wrapper = JBUI.Panels.simplePanel();
        wrapper.add(toolbar.getComponent(), BorderLayout.NORTH);
        ListCheckPanel listCheckPanel = new ListCheckPanel(listLoader, project);
        wrapper.add(listCheckPanel, BorderLayout.SOUTH);

        content.addToTop(wrapper);
        JScrollPane scrollPane = new ScrollPanel(list, listLoader);
        content.addToCenter(scrollPane);

        // This needs to be disposed
        JobScheduler.getScheduler().scheduleWithFixedDelay(() -> {
            if (projectSettings.activeProject == null || listModel.getSize() == 0) {
                return;
            }
            listLoader.loadNewAndUpdated();
        }, 5, 15, TimeUnit.SECONDS);

        listLoader.init();
        listLoader.reload();
    }

    private ActionToolbar createToolbar(Project project) {
        ActionManager actionManager = ActionManager.getInstance();
        final DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(ActionManager.getInstance().getAction("CircleCI.Refresh"));
        actionGroup.add(ActionManager.getInstance().getAction("CircleCI.AddProject"));
        CircleCIProjectComboBox projectPicker = new CircleCIProjectComboBox(project);
        actionGroup.add(projectPicker);
        actionGroup.add(ActionManager.getInstance().getAction("CircleCI.OpenSettings"));
        actionGroup.addSeparator();

        return actionManager.createActionToolbar("CircleCI Toolbar",
                actionGroup, true);
    }

    private void openSettingsPanelIfNotConfigured(JBLoadingPanel loadingPanel, JPanel openSettingsPanel) {
        if (settings.serverUrl == null || settings.token == null) {
            setContent(openSettingsPanel);
        } else {
            setContent(loadingPanel);
        }
    }
}