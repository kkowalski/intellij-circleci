package com.circleci;

import com.circleci.api.model.Build;
import com.circleci.api.model.Project;
import com.intellij.ui.CollectionListModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListChangeChecker {

    private List<Build> lastCheckBuilds;
    private Project lastCheckedProject;
    private com.intellij.openapi.project.Project intellijProject;
    private CircleCIProjectSettings projectSettings;
    private CollectionListModel<Build> listModel;

    public ListChangeChecker(com.intellij.openapi.project.Project intellijProject, CircleCIProjectSettings projectSettings, CollectionListModel<Build> listModel) {
        this.intellijProject = intellijProject;
        this.projectSettings = projectSettings;
        this.listModel = listModel;
    }

    void check(List<Build> builds) {
        if (builds.size() == 0) {
            return;
        }

        // we have new head of the list
        if (!builds.get(0).getBuildNumber().equals(listModel.getElementAt(0).getBuildNumber())) {
            lastCheckBuilds = builds;
            lastCheckedProject = projectSettings.activeProject;
            sendNewDataEvent();
            return;
        }

        // check statuses
        // build numbers might be missing, can't count on a nice sequence
        Map<Integer, Build> buildsMap = toBuildsMap(builds);
        List<Build> oldBuilds = listModel.getItems();
        for (Build oldBuild : oldBuilds) {
            Build build = buildsMap.get(oldBuild.getBuildNumber());
            if (build != null && !oldBuild.getStatus().equals(build.getStatus())) {
                lastCheckBuilds = builds;
                sendNewDataEvent();
            }
        }
        lastCheckedProject = projectSettings.activeProject;
    }

    private Map<Integer, Build> toBuildsMap(List<Build> builds) {
        Map<Integer, Build> map = new HashMap<>();
        for (Build build : builds) {
            map.put(build.getBuildNumber(), build);
        }
        return map;
    }

    private void sendNewDataEvent() {
        intellijProject
                .getMessageBus()
                .syncPublisher(CircleCIEvents.NEW_DATA_TOPIC).newData();
    }

    public List<Build> getLastCheckBuilds() {
        return lastCheckBuilds;
    }

    public Project getLastCheckedProject() {
        return lastCheckedProject;
    }
}
