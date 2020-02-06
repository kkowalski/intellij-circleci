package com.circleci.ui;

import com.circleci.BuildListLoader;
import com.circleci.LoadingListener;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;

import static com.circleci.LoadRequests.merge;

class ListCheckPanel extends BorderLayoutPanel {

    public ListCheckPanel(BuildListLoader listLoader) {
        setOpaque(true);
        setVisible(false);

        JEditorPane jEditorPane = new JEditorPane();
        jEditorPane.setFocusable(false);
        jEditorPane.setEditable(false);
        jEditorPane.setOpaque(false);
        Color linkColor = JBUI.CurrentTheme.Link.linkColor();
        HTMLEditorKit htmlEditorKit = UIUtil.getHTMLEditorKit();
        htmlEditorKit.getStyleSheet().addRule(String.format("a {color: rgb(%d, %d, %d)}",
                linkColor.getRed(), linkColor.getGreen(), linkColor.getBlue()) +
                "body {text-align: center}");
        jEditorPane.setEditorKit(htmlEditorKit);
        jEditorPane.setBorder(JBUI.Borders.empty(8, UIUtil.DEFAULT_HGAP));
        jEditorPane.setText("Build list is outdated. <a href=''>Refresh</a>");
        jEditorPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                ListCheckPanel.this.setVisible(false);
                listLoader.load(merge());
            } else {
                if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
                    jEditorPane.setCursor(new Cursor(Cursor.HAND_CURSOR));
                } else {
                    jEditorPane.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
            }
        });
        add(jEditorPane);

        listLoader.addCheckingListener(builds -> ListCheckPanel.this.setVisible(true));
        listLoader.addLoadingListener(new LoadingListener() {
            @Override
            public void loadingStarted(boolean reload) {
                if (reload) {
                    ListCheckPanel.this.setVisible(false);
                }
            }

            @Override
            public void loadingFinished() {
            }
        });
    }
}
