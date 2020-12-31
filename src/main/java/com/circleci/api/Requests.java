package com.circleci.api;

import com.circleci.CircleCISettings;
import com.circleci.api.model.Build;
import com.circleci.api.model.Project;
import com.intellij.util.io.HttpRequests;
import com.intellij.util.io.RequestBuilder;

public class Requests {

    private static String API_V1_BASE_PATH = "/api/v1.1";
    private static String API_V2_BASE_PATH = "/api/v2";

    private static String JSON_MIME_TYPE = "application/json";

    public static RequestBuilder getMe(String serverUrl, String token) {
        return HttpRequests.request(serverUrl + API_V1_BASE_PATH + "/me")
                .tuner(tuner -> tuner.setRequestProperty("Circle-Token", token))
                .readTimeout(10000);
    }

    public static RequestBuilder getProject(Project project) {
        CircleCISettings settings = CircleCISettings.getInstance();
        return withSharedSettings(HttpRequests.request(settings.serverUrl + API_V2_BASE_PATH +
                String.format("/project/%s/%s/%s/", project.provider.equals("Github") ? "gh" : "bb",
                        project.organization, project.name)));
    }

    public static RequestBuilder retryBuild(Build build) {
        CircleCISettings settings = CircleCISettings.getInstance();
        return withSharedSettings(HttpRequests.post(settings.serverUrl + API_V1_BASE_PATH +
                String.format("/project/%s/%s/%s/%s/retry", "github".equals(build.getVcsType()) ? "gh" : "bb",
                        build.getOrganization(), build.getProject(), build.getBuildNumber()), JSON_MIME_TYPE));
    }

    public static RequestBuilder cancelBuild(Build build) {
        CircleCISettings settings = CircleCISettings.getInstance();
        return withSharedSettings(HttpRequests.post(settings.serverUrl + API_V1_BASE_PATH +
                String.format("/project/%s/%s/%s/%s/cancel", "github".equals(build.getVcsType()) ? "gh" : "bb",
                        build.getOrganization(), build.getProject(), build.getBuildNumber()), JSON_MIME_TYPE));
    }

    public static RequestBuilder getBuilds(GetBuildsRequestParameters parameters) {
        CircleCISettings settings = CircleCISettings.getInstance();
        return withSharedSettings(HttpRequests.request(settings.serverUrl + API_V1_BASE_PATH + String.format("/project/%s/%s/%s?limit=%d&offset=%d", parameters.provider,
                parameters.organization, parameters.project, parameters.limit, parameters.offset)));
    }

    private static RequestBuilder withSharedSettings(RequestBuilder builder) {
        CircleCISettings settings = CircleCISettings.getInstance();
        return builder.tuner(tuner -> tuner.setRequestProperty("Circle-Token", settings.token))
                .readTimeout(10000);
    }

}
