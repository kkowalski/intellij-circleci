package com.circleci.ui;

import com.circleci.*;
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

import static com.circleci.LoadRequests.*;

public class CircleCIToolWindow extends SimpleToolWindowPanel {

    private CircleCISettings settings = CircleCISettings.getInstance();
    private Disposable parentDisposable;

    public CircleCIToolWindow(ToolWindow toolWindow, Disposable parentDisposable) {
        super(true);
        this.parentDisposable = parentDisposable;
    }

    public void init(Project project) {
        CollectionListModel<Build> listModel = new CollectionListModel<>();
        ListLoader listLoader = new ListLoader(listModel, settings);

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

        ActionManager actionManager = ActionManager.getInstance();
        ActionToolbar toolbar = actionManager.createActionToolbar("CircleCI Toolbar",
                (DefaultActionGroup) actionManager.getAction("CircleCI.toolbar"), true);
        toolbar.setTargetComponent(list);

        BorderLayoutPanel wrapper = JBUI.Panels.simplePanel();
        wrapper.add(toolbar.getComponent(), BorderLayout.NORTH);
        ListCheckPanel listCheckPanel = new ListCheckPanel(listLoader);
        wrapper.add(listCheckPanel, BorderLayout.SOUTH);

        content.addToTop(wrapper);
        JScrollPane scrollPane = new ScrollPanel(list, listLoader);
        content.addToCenter(scrollPane);


        // TODO extract to checker Checker
        JobScheduler.getScheduler().scheduleWithFixedDelay(() -> {
            if (settings.activeProject == null || listModel.getSize() == 0) {
                return;
            }
            listLoader.load(check());
        }, 5, 15, TimeUnit.SECONDS);

        if (settings.activeProject != null) {
            listLoader.load(reload());
        }
    }

    private void openSettingsPanelIfNotConfigured(JBLoadingPanel loadingPanel, JPanel openSettingsPanel) {
        if (settings.serverUrl == null || settings.token == null) {
            setContent(openSettingsPanel);
        } else {
            setContent(loadingPanel);
        }
    }
}