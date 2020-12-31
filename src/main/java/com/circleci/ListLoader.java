package com.circleci;

import com.circleci.api.GetBuildsRequestParameters;
import com.circleci.api.Requests;
import com.circleci.api.model.Build;
import com.circleci.api.JSON;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author Chris Kowalski
 */
public class ListLoader {

    private CollectionListModel<Build> listModel;
    private ListChangeChecker listChangeChecker;
    private CircleCIProjectSettings projectSettings;

    private boolean loading = false;

    private EventDispatcher<LoadingListener> eventDispatcher = EventDispatcher.create(LoadingListener.class);

    private static final Logger LOG = Logger.getInstance(ListLoader.class);
    private final com.intellij.openapi.project.Project intellijProject;

    public ListLoader(CollectionListModel<Build> listModel, ListChangeChecker listChangeChecker, com.intellij.openapi.project.Project intellijProject) {
        this.listModel = listModel;
        this.listChangeChecker = listChangeChecker;
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
                .thenAccept(builds -> {
                    listChangeChecker.check(builds);
                    updateStatuses(builds);
                })
                .thenRun(this::sendListUpdatedEvent);
    }

    public void loadMore() {
        load(LoadRequests.getMore())
                .thenAccept(this::addNewBuilds)
                .thenRun(this::sendListUpdatedEvent);
    }

    public void merge() {
        if (listChangeChecker.getLastCheckedProject().equals(projectSettings.activeProject)) {
            addNewBuilds(listChangeChecker.getLastCheckBuilds());
            updateStatuses(listChangeChecker.getLastCheckBuilds());
        }
    }

    public void addNewBuilds(List<Build> builds) {
        if (listModel.getSize() == 0 ||
                listModel.getElementAt(listModel.getSize() - 1).getBuildNumber() > builds.get(0).getBuildNumber()) {
            // append builds
            listModel.add(builds);
        } else {
            // prepend builds
            Build firstLocal = listModel.getElementAt(0);
            int position = builds.indexOf(firstLocal);
            if (position < 0) {
                listModel.addAll(0, builds);
            } else {
                listModel.addAll(0, builds.subList(0, position));
            }
        }
    }

    public void updateStatuses(List<Build> builds) {
        Map<Integer, Build> buildsMap = toBuildsMap(builds);
        List<Build> oldBuilds = listModel.getItems();
        for (Build oldBuild : oldBuilds) {
            Build build = buildsMap.get(oldBuild.getBuildNumber());
            if (build != null) {
                oldBuild.setStatus(build.getStatus());
            }
        }
    }

    private Map<Integer, Build> toBuildsMap(List<Build> builds) {
        Map<Integer, Build> map = new HashMap<>();
        for (Build build : builds) {
            map.put(build.getBuildNumber(), build);
        }
        return map;
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

}