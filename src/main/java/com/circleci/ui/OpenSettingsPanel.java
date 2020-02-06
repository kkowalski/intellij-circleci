package com.circleci.ui;

import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.util.ui.components.BorderLayoutPanel;

import javax.swing.*;

public class OpenSettingsPanel extends BorderLayoutPanel {

    public OpenSettingsPanel(Project project) {
        LinkLabel<?> linkLabel = LinkLabel.create("Open Settings", () -> {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, "CircleCI");
        });
        linkLabel.setVerticalTextPosition(SwingConstants.CENTER);
        linkLabel.setHorizontalAlignment(SwingConstants.CENTER);

        addToCenter(linkLabel);
    }
}
