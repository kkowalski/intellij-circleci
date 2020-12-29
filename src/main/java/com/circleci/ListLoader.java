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

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author Chris Kowalski
 */
public class ListLoader {

    private static final Logger LOG = Logger.getInstance(ListLoader.class);

    private CollectionListModel<Build> listModel;
    private CircleCIProjectSettings projectSettings;

    private boolean loading = false;

    private EventDispatcher<LoadingListener> eventDispatcher = EventDispatcher.create(LoadingListener.class);

    private List<Build> lastCheckBuilds;
    private Project checkedProject;
    private EventDispatcher<CheckingListener> eventDispatcherChecking = EventDispatcher.create(CheckingListener.class);

    public ListLoader(CollectionListModel<Build> listModel, com.intellij.openapi.project.Project intellijProject) {
        this.listModel = listModel;
        this.projectSettings = CircleCIProjectSettings.getInstance(intellijProject);
    }

    public void init(com.intellij.openapi.project.Project intellijProject) {
        intellijProject.getMessageBus()
                .connect()
                .subscribe(CircleCIEvents.PROJECT_CHANGED_TOPIC, event -> {
                    if (event.getCurrent() == null) {
                        listModel.removeAll();
                    }
                    projectSettings.activeProject = event.getCurrent();
                    reload();
                });
        reload();
    }

    public void reload() {
        load(LoadRequests.reload())
                .thenAccept((builds -> {
                    listModel.removeAll();
                    listModel.add(builds);
                }));
    }

    public void loadNewAndUpdated() {
        load(LoadRequests.check())
                .thenAccept(this::check);
    }

    public void merge() {
        CompletableFuture.completedFuture(lastCheckBuilds)
                .thenCompose((builds) -> builds != null ? CompletableFuture.completedFuture(builds) : load(LoadRequests.merge()))
                .thenAccept(this::merge);
    }

    public void loadMore() {
        load(LoadRequests.more())
                .thenAccept(this::more);
    }

    void more(List<Build> builds) {
        // TODO this should be just append
        Build lastLocalBuild = listModel.getElementAt(listModel.getSize() - 1);
        int positionOfLastLocalInFetched = builds.indexOf(lastLocalBuild);
        if (positionOfLastLocalInFetched < 0) {
            listModel.add(builds);
        } else {
            listModel.add(builds.subList(positionOfLastLocalInFetched + 1, builds.size()));
        }
    }

    void check(List<Build> builds) {
        if (builds.size() == 0) {
            return;
        }

        // we have new head of the list
        if (!builds.get(0).getBuildNumber().equals(listModel.getElementAt(0).getBuildNumber())) {
            eventDispatcherChecking.getMulticaster().listUpdated(builds);
            lastCheckBuilds = builds;
            checkedProject = projectSettings.activeProject;
            return;
        }

        // checking if status of any of the builds changed
        // TODO build numbers can be missing, switch to a hashmap
        for (int i = 0; i < builds.size(); i++) {
            if (!builds.get(i).getStatus().equals(listModel.getItems().get(i).getStatus())) {
                eventDispatcherChecking.getMulticaster().listUpdated(builds);
                lastCheckBuilds = builds;
            }
        }

        checkedProject = projectSettings.activeProject;
    }

    void merge(List<Build> builds) {
        if (checkedProject != projectSettings.activeProject) {
            return;
        }

        // prepend new builds
        int i = 0;
        while (i < builds.size() && !builds.get(i).getBuildNumber().equals(listModel.getElementAt(0).getBuildNumber())) {
            i++;
        }
        listModel.addAll(0, builds.subList(0, i));

        // update statuses
        // TODO build numbers can be missing, switch to a hashmap
        List<Build> oldBuilds = listModel.getItems();

        i = 0;
        while (i < builds.size()) {
            oldBuilds.get(i).setStatus(builds.get(i).getStatus());
            i++;
        }
    }

    public CompletableFuture<List<Build>> load(LoadRequest loadRequest) {
        if (projectSettings.activeProject == null) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        if (!loading) {
            loading = true;
            eventDispatcher.getMulticaster().loadingStarted(loadRequest instanceof ReloadRequest);

            return CompletableFuture.supplyAsync(() -> {
                Instant start = Instant.now();
                try {
                    Build latest = getLatest();
                    if (latest == null) {
                        return new ArrayList<>();
                    }
                    GetBuildsRequestParameters buildsRequestParameters = resolveGetBuildsRequestParameters(loadRequest, latest);
                    return JSON.fromJson(Requests.getBuilds(buildsRequestParameters).readString(),
                            new TypeReference<List<Build>>() {
                            });
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
                return null;
            }, JobScheduler.getScheduler());
        }
        return CompletableFuture.completedFuture(new ArrayList<>());
    }

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
        List<Build> builds = JSON.fromJson(Requests.getBuilds(getBuildsRequestParameters(0, 1)).readString(),
                new TypeReference<List<Build>>() {
                });
        return builds.size() > 0 ? builds.get(0) : null;
    }

    private GetBuildsRequestParameters getBuildsRequestParameters(int offset, int limit) {
        return new GetBuildsRequestParameters(projectSettings.activeProject.provider.equals("Github") ? "gh" : "bb",
                projectSettings.activeProject.organization, projectSettings.activeProject.name,
                limit, offset);
    }

    public void addLoadingListener(LoadingListener listener) {
        eventDispatcher.addListener(listener);
    }

    public void addCheckingListener(CheckingListener listener) {
        eventDispatcherChecking.addListener(listener);
    }

}