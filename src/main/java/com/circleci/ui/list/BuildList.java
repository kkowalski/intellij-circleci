package com.circleci.ui.list;

import com.circleci.api.model.Build;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class BuildList extends JBList<Build> {

    public BuildList(@NotNull ListModel<Build> dataModel) {
        super(dataModel);

        setEmptyText("No Builds available");
        setCellRenderer(new BuildListCellRenderer());
        BuildList list = this;
        ActionManager actionManager = ActionManager.getInstance();
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
        addMouseListener(popupHandler);
    }
}
