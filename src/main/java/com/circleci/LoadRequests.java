package com.circleci;

import com.circleci.api.GetBuildsRequestParameters;
import com.circleci.api.model.Build;
import com.intellij.ui.CollectionListModel;

public class LoadRequests {

    public static LoadRequest getNewAndUpdated() {
        return new GetNewAndUpdated();
    }

    public static LoadRequest reload() {
        return new Reload();
    }

    public static LoadRequest getMore() {
        return new GetMore();
    }

}

interface LoadRequest {
    GetBuildsRequestParameters getRequestParameters(CircleCIProjectSettings projectSettings, CollectionListModel<Build> listModel, Build latest);
}

class GetNewAndUpdated implements LoadRequest {

    public GetBuildsRequestParameters getRequestParameters(CircleCIProjectSettings projectSettings, CollectionListModel<Build> listModel, Build latest) {
        return new GetBuildsRequestParameters(projectSettings.activeProject.provider.equals("Github") ? "gh" : "bb",
                projectSettings.activeProject.organization, projectSettings.activeProject.name,
                0, Math.min(latest.getBuildNumber() - listModel.getElementAt(listModel.getSize() - 1).getBuildNumber() + 1, 100));
    }

}

class Reload implements LoadRequest {

    public GetBuildsRequestParameters getRequestParameters(CircleCIProjectSettings projectSettings, CollectionListModel<Build> listModel, Build latest) {
        return new GetBuildsRequestParameters(projectSettings.activeProject.provider.equals("Github") ? "gh" : "bb",
                projectSettings.activeProject.organization, projectSettings.activeProject.name,
                0, 25);
    }
}

class GetMore implements LoadRequest {

    public GetBuildsRequestParameters getRequestParameters(CircleCIProjectSettings projectSettings, CollectionListModel<Build> listModel, Build latest) {
        return new GetBuildsRequestParameters(projectSettings.activeProject.provider.equals("Github") ? "gh" : "bb",
                projectSettings.activeProject.organization, projectSettings.activeProject.name,
                latest.getBuildNumber() - listModel.getElementAt(listModel.getSize() - 1).getBuildNumber() + 1, 10);
    }

}