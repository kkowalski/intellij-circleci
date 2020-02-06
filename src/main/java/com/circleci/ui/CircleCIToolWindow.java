package com.circleci.ui;

import com.circleci.*;
import com.circleci.api.model.Build;
import com.circleci.ui.list.BuildList;
import com.intellij.concurrency.JobScheduler;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.*;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBLoadingPanel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.util.concurrent.TimeUnit;

import static com.circleci.LoadRequests.*;

public class CircleCIToolWindow {
    private ActionManager actionManager = ActionManager.getInstance();
    private CircleCISettings settings = CircleCISettings.getInstance();
    private SimpleToolWindowPanel windowContent = new SimpleToolWindowPanel(true);
    private JBLoadingPanel loadingPanel;

    public CircleCIToolWindow(ToolWindow toolWindow, com.intellij.openapi.project.Project project) {
        Disposable disposable = Disposer.newDisposable();

        JPanel settingsPanel = new OpenSettingsPanel(project);

        JPanel infoPanel = JBUI.Panels.simplePanel(); // TODO remove soon
        CollectionListModel<Build> listModel = new CollectionListModel<>();
        BuildListLoader listLoader = new BuildListLoader(listModel, infoPanel, settings);

        // View
        BorderLayoutPanel content = JBUI.Panels.simplePanel();
        loadingPanel = new CircleCILoadingPanel(disposable, content, listLoader);

        // View logic
        if (settings.serverUrl == null || settings.token == null) {
            windowContent.setContent(settingsPanel);
        } else {
            windowContent.setContent(loadingPanel);
        }
        ApplicationManager.getApplication().getMessageBus()
                .connect().subscribe(CircleCIEvents.SETTINGS_UPDATED_TOPIC, state -> {
            if ("".equals(settings.token) || "".equals(settings.serverUrl)) {
                return;
            }
            windowContent.removeAll();
            windowContent.setContent(loadingPanel);
        });

        JBList<Build> list = new BuildList(listModel);
        // This is not really view
        list.setDataProvider(dataId -> {
            if (dataId.equals(CircleCIDataKeys.listSelectedBuildKey.getName())) {
                return list.getSelectedValue();
            } else if (dataId.equals(CircleCIDataKeys.listLoaderKey.getName())) {
                return listLoader;
            } else {
                return null;
            }
        });

        ActionToolbar toolbar = actionManager.createActionToolbar("CircleCI Toolbar",
                (DefaultActionGroup) actionManager.getAction("CircleCI.toolbar"), true);
        toolbar.setTargetComponent(list);


        // Info panel extraction to a class
        JEditorPane jEditorPane = new JEditorPane();
        jEditorPane.setFocusable(false);
        jEditorPane.setEditable(false);
        jEditorPane.setOpaque(false);
        Color linkColor = JBUI.CurrentTheme.Link.linkColor();
        HTMLEditorKit htmlEditorKit = UIUtil.getHTMLEditorKit();
        htmlEditorKit.getStyleSheet().addRule(String.format("a {color: rgb(%d, %d, %d)}", linkColor.getRed(), linkColor.getGreen(), linkColor.getBlue()) +
                "body {text-align: center}");
        jEditorPane.setEditorKit(htmlEditorKit);
        jEditorPane.setBorder(JBUI.Borders.empty(8, UIUtil.DEFAULT_HGAP));
        jEditorPane.setText("Build list is outdated. <a href=''>Refresh</a>");
        jEditorPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                listLoader.load(merge());
            } else {
                if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
                    jEditorPane.setCursor(new Cursor(Cursor.HAND_CURSOR));
                } else {
                    jEditorPane.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
            }
        });

        infoPanel.add(jEditorPane);
        infoPanel.setOpaque(true);
        infoPanel.setVisible(false);

        BorderLayoutPanel wrapper = JBUI.Panels.simplePanel();
        wrapper.add(toolbar.getComponent(), BorderLayout.NORTH);
        wrapper.add(infoPanel, BorderLayout.SOUTH);
        content.addToTop(wrapper);

        ScrollingUtil.installActions(list);
        JScrollPane scrollPane = new ScrollPane(list, listLoader);
        content.addToCenter(scrollPane);

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

    public JPanel getContent() {
        return windowContent;
    }
}