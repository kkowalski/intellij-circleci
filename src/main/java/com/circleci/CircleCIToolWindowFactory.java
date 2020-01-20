package com.circleci;

import com.circleci.ui.CircleCIToolWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * @author Chris Kowalski
 */
public class CircleCIToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // TODO take a look at the panel composition and initialization

        CircleCIToolWindow circleCiToolWindow = new CircleCIToolWindow(toolWindow,project);
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(circleCiToolWindow.getContent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
