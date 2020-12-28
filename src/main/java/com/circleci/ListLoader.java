package com.circleci;

import com.circleci.api.GetBuildsRequestParameters;
import com.circleci.api.Requests;
import com.circleci.api.model.Build;
import com.circleci.api.JSON;
import com.circleci.api.model.Project;
import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.concurrency.JobScheduler;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.CollectionListModel;
import com.intellij.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Chris Kowalski
 */
public class ListLoader {

    private static final Logger LOG = Logger.getInstance(ListLoader.class);

    private CollectionListModel<Build> listModel;
    private CircleCISettings settings;
    private CircleCIProjectSettings projectSettings;

    private boolean loading = false;

    private EventDispatcher<LoadingListener> eventDispatcher = EventDispatcher.create(LoadingListener.class);

    private List<Build> lastCheckBuilds;
    private Project lastActiveProject;
    private EventDispatcher<CheckingListener> eventDispatcherChecking = EventDispatcher.create(CheckingListener.class);

    public ListLoader(CollectionListModel<Build> listModel, com.intellij.openapi.project.Project intellijProject) {
        this.listModel = listModel;
        this.settings = CircleCISettings.getInstance();
        this.projectSettings = CircleCIProjectSettings.getInstance(intellijProject);

        intellijProject.getMessageBus()
                .connect()
                .subscribe(CircleCIEvents.PROJECT_CHANGED_TOPIC, event -> {
                    if (event.getCurrent() == null) {
                        listModel.removeAll();
                    }
                    projectSettings.activeProject = event.getCurrent();
                    load(LoadRequests.reload());
                });
    }

    public void load(LoadRequest loadRequest) {
        if (projectSettings.activeProject == null) {
            return;
        }

        if (!loading) {
            loading = true;
            eventDispatcher.getMulticaster().loadingStarted(loadRequest instanceof ReloadRequest);

            JobScheduler.getScheduler().schedule(() -> {
                Instant start = Instant.now();
                try {
                    Build latest = getLatest();
                    if (latest == null) {
                        return;
                    }
                    // TODO refactor this
                    GetBuildsRequestParameters buildsRequestParameters = resolveGetBuildsRequestParameters(loadRequest, latest);

                    List<Build> builds;
                    if (mergeRequestWithFreshCheckData(loadRequest, latest)) {
                        builds = lastCheckBuilds;
                    } else {
                        builds = JSON.fromJsonString(Requests.getBuilds(buildsRequestParameters).readString(),
                                new TypeReference<List<Build>>() {
                                });
                    }
                    loadRequestActionAfterLoad(loadRequest, builds);
                } catch (Exception e) {
                    String msg = "Error loading builds";
                    LOG.error(msg, e);
                    Notifications.Bus.notify(new Notification("CircleCI",
                            msg, e.getMessage(), NotificationType.ERROR));
                } finally {
                    loading = false;
                    eventDispatcher.getMulticaster().loadingFinished();

                    Instant end = Instant.now();
                    LOG.debug("Fetching builds time : " + Duration.between(start, end).getNano() / 1000_1000 + "ms");
                }
            }, 0, TimeUnit.SECONDS);
        }
    }

    @NotNull
    private GetBuildsRequestParameters resolveGetBuildsRequestParameters(LoadRequest loadRequest, Build latest) {
        GetBuildsRequestParameters buildsRequestParameters;
        if (loadRequest instanceof MoreRequest) {
            buildsRequestParameters = getBuildsRequestParameters(latest.getBuildNumber() - listModel.getElementAt(listModel.getSize() - 1).getBuildNumber() + 1, 10);
        } else if (loadRequest instanceof ReloadRequest) {
            buildsRequestParameters = getBuildsRequestParameters(0, 25);
        } else {
            buildsRequestParameters = getBuildsRequestParameters(0, Math.min(latest.getBuildNumber() - listModel.getElementAt(listModel.getSize() - 1).getBuildNumber() + 1, 100));
        }
        return buildsRequestParameters;
    }

    private Build getLatest() throws IOException {
        List<Build> builds = JSON.fromJsonString(Requests.getBuilds(getBuildsRequestParameters(0, 1)).readString(),
                new TypeReference<List<Build>>() {
                });
        return builds.size() > 0 ? builds.get(0) : null;
    }

    private boolean mergeRequestWithFreshCheckData(LoadRequest loadRequest, Build latest) {
        return loadRequest instanceof MergeRequest && lastCheckBuilds != null && latest.getBuildNumber().equals(lastCheckBuilds.get(0).getBuildNumber());
    }

    public void loadRequestActionAfterLoad(LoadRequest loadRequest, List<Build> builds) {
        if (loadRequest instanceof CheckRequest) { // TODO extract to checker Checker
            if (builds.size() == 0) {
                return;
            }

            // we have new head of the list
            if (!builds.get(0).getBuildNumber().equals(listModel.getElementAt(0).getBuildNumber())) {
                eventDispatcherChecking.getMulticaster().listUpdated(builds);
                lastCheckBuilds = builds;
                lastActiveProject = projectSettings.activeProject;
                return;
            }

            // checking if status of any of the builds changed
            for (int i = 0; i < builds.size(); i++) {
                if (!builds.get(i).getStatus().equals(listModel.getItems().get(i).getStatus())) {
                    eventDispatcherChecking.getMulticaster().listUpdated(builds);
                    lastCheckBuilds = builds;
                }
            }

            lastActiveProject = projectSettings.activeProject;
        } else if (loadRequest instanceof MergeRequest) {
            if (lastActiveProject != projectSettings.activeProject) {
                return;
            }

            // prepend new builds
            int i = 0;
            while (i < builds.size() && !builds.get(i).getBuildNumber().equals(listModel.getElementAt(0).getBuildNumber())) {
                i++;
            }
            listModel.addAll(0, builds.subList(0, i));

            // update statuses
            List<Build> oldBuilds = listModel.getItems();

            i = 0;
            while (i < builds.size()) {
                oldBuilds.get(i).setStatus(builds.get(i).getStatus());
                i++;
            }
        } else if (loadRequest instanceof MoreRequest) {
            Build lastLocalBuild = listModel.getElementAt(listModel.getSize() - 1);
            int lastLocalPositionInFetched = builds.indexOf(lastLocalBuild);
            if (lastLocalPositionInFetched == -1) {
                listModel.add(builds);
            } else {
                listModel.add(builds.subList(lastLocalPositionInFetched + 1, builds.size()));
            }
        } else {
            listModel.removeAll();
            listModel.add(builds);
        }
    }

    private GetBuildsRequestParameters getBuildsRequestParameters(int offset, int limit) {
        return new GetBuildsRequestParameters(projectSettings.activeProject.provider.equals("Github") ? "gh" : "bb",
                projectSettings.activeProject.organization, projectSettings.activeProject.name,
                limit, offset);
    }

    public void addLoadingListener(LoadingListener listener) {
        eventDispatcher.addListener(listener);
    }

    // TODO extract to checker Checker
    public void addCheckingListener(CheckingListener listener) {
        eventDispatcherChecking.addListener(listener);
    }

}