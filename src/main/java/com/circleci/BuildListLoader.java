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
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBLoadingPanel;
import com.intellij.vcs.log.ui.frame.ProgressStripe;

import javax.swing.*;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Chris Kowalski
 */
public class BuildListLoader {
    private CollectionListModel<Build> listModel;
    private JBLoadingPanel loadingPanel;
    private ProgressStripe progressStripe;
    private JPanel infoPanel;

    private CircleCISettings settings;

    private boolean loading = false;
    private List<Build> lastCheckBuilds;
    private Instant lastCheckTimestamp;
    private Project lastActiveProject;

    public BuildListLoader(CollectionListModel<Build> listModel, JBLoadingPanel loadingPanel,
                           ProgressStripe progressStripe, JPanel infoPanel, CircleCISettings settings) {
        this.listModel = listModel;
        this.loadingPanel = loadingPanel;
        this.progressStripe = progressStripe;
        this.infoPanel = infoPanel;
        this.settings = settings;

        ApplicationManager.getApplication().getMessageBus()
                .connect()
                .subscribe(CircleCIEvents.PROJECT_CHANGED_TOPIC, event -> {
                    if (event.getCurrent() == null) {
                        listModel.removeAll();
                    }
                    settings.activeProject = event.getCurrent();
                    load(LoadRequests.refresh());
                });
    }

    public void load(LoadRequest loadRequest) {
        if (settings.activeProject == null) {
            return;
        }

        if (!loading) {
            loading = true;
            System.out.println("Loading started.");
            updateUIOnLoadingStarted(loadRequest);

            JobScheduler.getScheduler().schedule(() -> {
                Instant start = Instant.now();
                try {
                    List<Build> builds;
                    if (mergeRequestWithFreshCheckData(loadRequest)) {
                        builds = lastCheckBuilds;
                    } else {
                        builds = JSON.fromJsonString(Requests.getBuilds(getBuildsRequestParameters(loadRequest)).readString(),
                                new TypeReference<List<Build>>() {
                                });
                    }
                    loadRequestActionAfterLoad(loadRequest, builds);
                } catch (Exception e) {
                    Notifications.Bus.notify(new Notification("CircleCI",
                            "Error loading builds", e.getMessage(), NotificationType.ERROR));
                } finally {
                    loading = false;
                    updateUIAfterLoadingFinished();

                    System.out.println("Loading finished.");
                    Instant end = Instant.now();
                    System.out.println("Fetching builds time : " + Duration.between(start, end).getNano() / 1000_1000 + "ms");
                }
            }, 0, TimeUnit.SECONDS);
        }
    }

    private boolean mergeRequestWithFreshCheckData(LoadRequest loadRequest) {
        return loadRequest instanceof MergeRequest && Instant.now().getEpochSecond() - lastCheckTimestamp.getEpochSecond() > 0;
    }

    private void updateUIOnLoadingStarted(LoadRequest loadRequest) {
        if (loadRequest instanceof RefreshRequest) {
            loadingPanel.startLoading();
        }
        progressStripe.startLoading();
    }

    private void updateUIAfterLoadingFinished() {
        loadingPanel.stopLoading();
        progressStripe.stopLoading();
    }

    public void loadRequestActionAfterLoad(LoadRequest loadRequest, List<Build> builds) {
        if (loadRequest instanceof CheckRequest) {
            if (builds.size() == 0) {
                return;
            }

            // we have new head of the list
            if (!builds.get(0).getBuildNumber().equals(listModel.getElementAt(0).getBuildNumber())) {
                infoPanel.setVisible(true);
            }

            // checking if status of any of the builds changed
            for (int i = 0; i < builds.size(); i++) {
                if (!builds.get(i).getStatus().equals(listModel.getItems().get(i).getStatus())) {
                    infoPanel.setVisible(true);
                }
            }

            lastCheckBuilds = builds;
            lastCheckTimestamp = Instant.now();
            lastActiveProject = settings.activeProject;
        } else if (loadRequest instanceof MergeRequest) {
            if (lastActiveProject != settings.activeProject) {
                return;
            }

            // prepend new builds
            int i = 0;
            while (!builds.get(i).getBuildNumber().equals(listModel.getElementAt(0).getBuildNumber())) {
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
            listModel.removeAll();
            listModel.add(builds);
        }
    }

    private GetBuildsRequestParameters getBuildsRequestParameters(LoadRequest loadRequest) {
        return new GetBuildsRequestParameters(settings.activeProject.provider.equals("Github") ? "gh" : "bb",
                settings.activeProject.organization, settings.activeProject.name,
                loadRequest.getLimit(), loadRequest.getOffset());
    }
}