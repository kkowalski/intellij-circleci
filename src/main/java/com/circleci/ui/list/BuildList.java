package com.circleci.ui.list;

import com.circleci.CircleCIEvents;
import com.circleci.CircleCIProjectSettings;
import com.circleci.api.model.Build;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.components.JBList;
import com.intellij.ui.speedSearch.FilteringListModel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;

public class BuildList extends JBList<Build> {

    public BuildList(@NotNull Project project, @NotNull ListModel<Build> dataModel) {
        this(project, new FilteringListModel<>(dataModel));
    }

    private BuildList(@NotNull Project project, @NotNull FilteringListModel<Build> dataModel) {
        super(dataModel);

        CircleCIProjectSettings projectSettings = CircleCIProjectSettings.getInstance(project);
        dataModel.setFilter(build -> build.getBranch().contains(projectSettings.branchFilter));
        project.getMessageBus().connect().subscribe(CircleCIEvents.BRANCH_FILTER_CHANGED_TOPIC, dataModel::refilter);
        dataModel.getOriginalModel().addListDataListener(new EmptyTextSetter());

        setCellRenderer(new BuildListCellRenderer());

        ActionManager actionManager = ActionManager.getInstance();
        PopupHandler popupHandler = new PopupHandler() {
            @Override
            public void invokePopup(Component comp, int x, int y) {
                ActionPopupMenu popupMenu = actionManager
                        .createActionPopupMenu("CircleCIBuildListPopup",
                                (DefaultActionGroup) actionManager.getAction("CircleCI.Build.ToolWindow.List.Popup"));
                popupMenu.setTargetComponent(BuildList.this);
                popupMenu.getComponent().show(comp, x, y);
            }
        };
        addMouseListener(popupHandler);
    }

    private class EmptyTextSetter implements ListDataListener {
        EmptyTextSetter() {
            setEmptyText();
        }

        @Override
        public void intervalAdded(ListDataEvent e) {
            setEmptyText();
        }

        @Override
        public void intervalRemoved(ListDataEvent e) {
            setEmptyText();
        }

        @Override
        public void contentsChanged(ListDataEvent e) {
            setEmptyText();
        }

        private void setEmptyText() {
            String emptyText = "No builds available";
            ListModel<Build> model = BuildList.this.getModel();
            if (model instanceof FilteringListModel) {
                int filteredSize = model.getSize();
                int orgSize = ((FilteringListModel<Build>) model).getOriginalModel().getSize();
                if (filteredSize == 0 && orgSize > 0) {
                    BuildList.this.setEmptyText(emptyText + " (" + orgSize + " builds are filtered)");
                    return;
                }
            }
            BuildList.this.setEmptyText(emptyText);
        }
    }
}
