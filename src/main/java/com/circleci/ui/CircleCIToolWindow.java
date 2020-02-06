package com.circleci.ui;

import com.circleci.*;
import com.circleci.api.model.Build;
import com.intellij.concurrency.JobScheduler;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.util.ProgressWindow;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.*;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBLoadingPanel;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import com.intellij.vcs.log.ui.frame.ProgressStripe;

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
        LinkLabel<?> linkLabel = LinkLabel.create("Open Settings", () -> {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, "CircleCI");
        });
        linkLabel.setVerticalTextPosition(SwingConstants.CENTER);
        linkLabel.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel settingsPanel = JBUI.Panels.simplePanel().addToCenter(linkLabel);

        Disposable disposable = Disposer.newDisposable();
        loadingPanel = new JBLoadingPanel(new BorderLayout(), disposable);

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


        BorderLayoutPanel content = JBUI.Panels.simplePanel();
        ProgressStripe progressStripe = new ProgressStripe(content, disposable, ProgressWindow.DEFAULT_PROGRESS_DIALOG_POSTPONE_TIME_MILLIS);
        loadingPanel.add(progressStripe);

        JPanel infoPanel = JBUI.Panels.simplePanel();
        CollectionListModel<Build> listModel = new CollectionListModel<>();
        BuildListLoader listLoader = new BuildListLoader(listModel, loadingPanel, progressStripe, infoPanel, settings);


        JBList<Build> list = new JBList<>(listModel);
        list.setEmptyText("No Builds");
        list.setCellRenderer(new BuildListCellRenderer());
        list.setDataProvider(dataId -> {
            if (dataId.equals(CircleCIDataKeys.listSelectedBuildKey.getName())) {
                return list.getSelectedValue();
            } else if (dataId.equals(CircleCIDataKeys.listLoaderKey.getName())) {
                return listLoader;
            } else {
                return null;
            }
        });
        PopupHandler popupHandler = new PopupHandler() {
            @Override
            public void invokePopup(Component comp, int x, int y) {
                ActionPopupMenu popupMenu = actionManager
                        .createActionPopupMenu("CircleCIBuildListPopup",
                                (DefaultActionGroup) actionManager.getAction("CircleCI.Build.ToolWindow.List.Popup"));
                popupMenu.setTargetComponent(list);
                popupMenu.getComponent().show(comp, x, y);
            }
        };
        list.addMouseListener(popupHandler);

        ActionToolbar toolbar = actionManager.createActionToolbar("CircleCI Toolbar",
                (DefaultActionGroup) actionManager.getAction("CircleCI.toolbar"), true);
        toolbar.setTargetComponent(list);

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
        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(list,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().getModel().addChangeListener(e -> {
            if (listModel.getSize() > 0 && isScrollAtThreshold(scrollPane)) {
                listLoader.load(more());
            }
        });
        content.addToCenter(scrollPane);

        JobScheduler.getScheduler().scheduleWithFixedDelay(() -> {
            if (settings.activeProject == null || listModel.getSize() == 0) {
                return;
            }
            listLoader.load(check());
        }, 5, 15, TimeUnit.SECONDS);

        if (settings.activeProject != null) {
            listLoader.load(refresh());
        }
    }

    private boolean isScrollAtThreshold(JScrollPane scrollPane) {
        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
        int visibleAmount = verticalScrollBar.getVisibleAmount();
        int value = verticalScrollBar.getValue();
        float maximum = verticalScrollBar.getMaximum() * 1.0f;
        if (maximum == 0) {
            return false;
        }
        float scrollFraction = (visibleAmount + value) / maximum;
        if (scrollFraction < 0.80) {
            return false;
        }

        return true;
    }

    public JPanel getContent() {
        return windowContent;
    }
}