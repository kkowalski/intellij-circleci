package com.circleci;

import com.circleci.ui.CircleCIToolWindow;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;

/**
 * @author Chris Kowalski
 */
public class CircleCIToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        Disposable disposable = Disposer.newDisposable();
        CircleCIToolWindow circleCiToolWindow = new CircleCIToolWindow(project, toolWindow, disposable);
        circleCiToolWindow.init(project);
        ContentManager contentManager = toolWindow.getContentManager();
        Content content = contentManager.getFactory().createContent(circleCiToolWindow, "", false);
        toolWindow.getContentManager().addContent(content);
        content.setDisposer(disposable);
    }
}
