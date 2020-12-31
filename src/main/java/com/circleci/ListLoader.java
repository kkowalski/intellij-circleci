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

    private CollectionListModel<Build> listModel;
    private CircleCIProjectSettings projectSettings;

    private List<Build> lastCheckBuilds;
    private Project checkedProject;

    private boolean loading = false;

    private EventDispatcher<LoadingListener> eventDispatcher = EventDispatcher.create(LoadingListener.class);
    private EventDispatcher<CheckingListener> eventDispatcherChecking = EventDispatcher.create(CheckingListener.class);

    private static final Logger LOG = Logger.getInstance(ListLoader.class);
    private final com.intellij.openapi.project.Project intellijProject;

    public ListLoader(CollectionListModel<Build> listModel, com.intellij.openapi.project.Project intellijProject) {
        this.listModel = listModel;
        this.projectSettings = CircleCIProjectSettings.getInstance(intellijProject);
        this.intellijProject = intellijProject;
    }

    public void init() {
        subscribeToProjectChangedEvent();
    }

    private void subscribeToProjectChangedEvent() {
        intellijProject.getMessageBus()
                .connect()
                .subscribe(CircleCIEvents.PROJECT_CHANGED_TOPIC, event -> {
                    if (event.getCurrent() == null) {
                        listModel.removeAll();
                    }
                    projectSettings.activeProject = event.getCurrent();
                    reload();
                });
    }

    public void reload() {
        load(LoadRequests.reload())
                .thenAccept((builds -> {
                    listModel.removeAll();
                    listModel.add(builds);
                }))
                .thenRun(this::sendListUpdatedEvent);
    }

    public void loadNewAndUpdated() {
        load(LoadRequests.getNewAndUpdated())
                .thenAccept(this::check);
    }

    public void loadMore() {
        load(LoadRequests.getMore())
                .thenAccept(this::more)
                .thenRun(this::sendListUpdatedEvent);
    }

    public void merge() {
        merge(lastCheckBuilds);
        sendListUpdatedEvent();
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
            eventDispatcher.getMulticaster().loadingStarted(loadRequest instanceof Reload);

            return CompletableFuture.supplyAsync(() -> {
                Instant start = Instant.now();
                try {
                    Build latest = getLatest(); // TODO work towards extracting this out
                    if (latest == null) {
                        return new ArrayList<>();
                    }
                    GetBuildsRequestParameters buildsRequestParameters = loadRequest.getRequestParameters(projectSettings, listModel, latest);
                    return JSON.fromJson(Requests.getBuilds(buildsRequestParameters).readString(),
                            new TypeReference<List<Build>>() {
                            });
                } catch (Exception e) {
                    String msg = "Error loading builds";
                    LOG.error(msg, e);
                    Notifications.Bus.notify(new Notification("CircleCI",
                            msg, e.getMessage(), NotificationType.ERROR));
                    return new ArrayList<>();
                } finally {
                    loading = false;
                    eventDispatcher.getMulticaster().loadingFinished();

                    Instant end = Instant.now();
                    LOG.debug("Fetching builds time : " + Duration.between(start, end).getNano() / 1000_1000 + "ms");
                }
            }, JobScheduler.getScheduler());
        }
        return CompletableFuture.completedFuture(new ArrayList<>());
    }

    private void sendListUpdatedEvent() {
        intellijProject
                .getMessageBus()
                .syncPublisher(CircleCIEvents.LIST_UPDATED_TOPIC)
                .listUpdate();
    }

    private void sendNewItemsStored() {
        intellijProject
                .getMessageBus()
                .syncPublisher(CircleCIEvents.NEW_ITEMS_STORED_TOPIC)
                .newItemsStored();
    }

    private Build getLatest() throws IOException {
        List<Build> builds = JSON.fromJson(Requests.getBuilds(requestParamsLatestBuild()).readString(),
                new TypeReference<List<Build>>() {
                });
        return builds.size() > 0 ? builds.get(0) : null;
    }

    private GetBuildsRequestParameters requestParamsLatestBuild() {
        return new GetBuildsRequestParameters(projectSettings.activeProject.provider.equals("Github") ? "gh" : "bb",
                projectSettings.activeProject.organization, projectSettings.activeProject.name,
                0, 1);
    }

    public void addLoadingListener(LoadingListener listener) {
        eventDispatcher.addListener(listener);
    }

    public void addCheckingListener(CheckingListener listener) {
        eventDispatcherChecking.addListener(listener);
    }

}