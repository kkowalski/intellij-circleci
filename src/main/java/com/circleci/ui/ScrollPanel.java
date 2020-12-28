package com.circleci.ui;

import com.circleci.ListLoader;
import com.circleci.api.model.Build;
import com.intellij.ui.ScrollingUtil;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;

import static com.circleci.LoadRequests.more;

class ScrollPanel extends JBScrollPane {

    public ScrollPanel(JBList<Build> list, ListLoader listLoader) {
        setViewportView(list);
        setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        ScrollingUtil.installActions(list);
        getVerticalScrollBar().getModel().addChangeListener(e -> {
            if (list.getModel().getSize() > 0 && isScrollAtThreshold(ScrollPanel.this)) {
                listLoader.load(more());
            }
        });
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
        return !(scrollFraction < 0.80);
    }

}
