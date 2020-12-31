package com.circleci;

import com.circleci.api.model.Build;
import com.circleci.api.model.Project;
import com.intellij.ui.CollectionListModel;

import java.util.List;

public class ListChangeChecker {

    private List<Build> lastCheckBuilds;
    private Project lastCheckedProject;
    private CircleCIProjectSettings projectSettings;
    private CollectionListModel<Build> listModel;

    public ListChangeChecker(CircleCIProjectSettings projectSettings,
                             CollectionListModel<Build> listModel) {
        this.projectSettings = projectSettings;
        this.listModel = listModel;
    }

    boolean areNewBuildsAvailable(List<Build> builds) {
        if (builds.size() == 0) {
            return false;
        }

        // there's a new head
        if (!builds.get(0).getBuildNumber().equals(listModel.getElementAt(0).getBuildNumber())) {
            lastCheckBuilds = builds;
            lastCheckedProject = projectSettings.activeProject;
            return true;
        }
        return false;
    }

    public List<Build> getLastCheckBuilds() {
        return lastCheckBuilds;
    }

    public Project getLastCheckedProject() {
        return lastCheckedProject;
    }
}
