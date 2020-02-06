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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.CollectionListModel;
import com.intellij.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Chris Kowalski
 */
public class BuildListLoader {

    private static final Logger LOG = Logger.getInstance(BuildListLoader.class);

    private CollectionListModel<Build> listModel;
    private JPanel infoPanel;

    private CircleCISettings settings;

    private boolean loading = false;
    private List<Build> lastCheckBuilds;
    private Project lastActiveProject;

    private EventDispatcher<LoadingListener> eventDispatcher = EventDispatcher.create(LoadingListener.class);

    public BuildListLoader(CollectionListModel<Build> listModel,
                           JPanel infoPanel, CircleCISettings settings) {
        this.listModel = listModel;
        this.infoPanel = infoPanel;
        this.settings = settings;

        ApplicationManager.getApplication().getMessageBus()
                .connect()
                .subscribe(CircleCIEvents.PROJECT_CHANGED_TOPIC, event -> {
                    if (event.getCurrent() == null) {
                        listModel.removeAll();
                    }
                    settings.activeProject = event.getCurrent();
                    load(LoadRequests.reload());
                });
    }

    public void load(LoadRequest loadRequest) {
        if (settings.activeProject == null) {
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
                    Notifications.Bus.notify(new Notification("CircleCI",
                            "Error loading builds", e.getMessage(), NotificationType.ERROR));
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
        if (loadRequest instanceof CheckRequest) {
            if (builds.size() == 0) {
                return;
            }

            // we have new head of the list
            if (!builds.get(0).getBuildNumber().equals(listModel.getElementAt(0).getBuildNumber())) {
                infoPanel.setVisible(true);
                lastCheckBuilds = builds;
                lastActiveProject = settings.activeProject;
                return;
            }

            // checking if status of any of the builds changed
            for (int i = 0; i < builds.size(); i++) {
                if (!builds.get(i).getStatus().equals(listModel.getItems().get(i).getStatus())) {
                    infoPanel.setVisible(true);
                    lastCheckBuilds = builds;
                }
            }

            lastActiveProject = settings.activeProject;
        } else if (loadRequest instanceof MergeRequest) {
            if (lastActiveProject != settings.activeProject) {
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

            infoPanel.setVisible(false);
        } else if (loadRequest instanceof MoreRequest) {
            Build lastLocalBuild = listModel.getElementAt(listModel.getSize() - 1);
            int lastLocalPositionInFetched = builds.indexOf(lastLocalBuild);
            if (lastLocalPositionInFetched == -1) {
                listModel.add(builds);
            } else {
                listModel.add(builds.subList(lastLocalPositionInFetched + 1, builds.size()));
            }
        } else {
            infoPanel.setVisible(false);
            listModel.removeAll();
            listModel.add(builds);
        }
    }

    private GetBuildsRequestParameters getBuildsRequestParameters(int offset, int limit) {
        return new GetBuildsRequestParameters(settings.activeProject.provider.equals("Github") ? "gh" : "bb",
                settings.activeProject.organization, settings.activeProject.name,
                limit, offset);
    }

    public void addLoadingListener(LoadingListener listener) {
        eventDispatcher.addListener(listener);
    }

}