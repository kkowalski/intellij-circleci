package com.circleci;

import com.intellij.util.messages.Topic;

public class CircleCIEvents {

    public static Topic<ProjectChangedListener> PROJECT_CHANGED_TOPIC = new Topic<>("CIRCLECI_PROJECT_CHANGED", ProjectChangedListener.class);
    public static Topic<SettingsUpdatedListener> SETTINGS_UPDATED_TOPIC = new Topic<>("CIRCLECI_SETTINGS_UPDATED", SettingsUpdatedListener.class);
    public static Topic<ListModelUpdatedListener> LIST_MODEL_UPDATED_TOPIC = new Topic<>("CIRCLECI_LIST_MODEL_UPDATED", ListModelUpdatedListener.class);
    public static Topic<BranchFilterChangedListener> BRANCH_FILTER_CHANGED_TOPIC = new Topic<>("CIRCLECI_BRANCH_FILTER_CHANGED_TOPIC", BranchFilterChangedListener.class);
    public static Topic<NewBuildsListener> NEW_BUILDS_TOPIC = new Topic<>("CIRCLECI_NEW_DATA_AVAILABLE", NewBuildsListener.class);

    public interface ProjectChangedListener {
        void projectChanged(ActiveProjectChangeEvent event);
    }

    public interface SettingsUpdatedListener {
        void settingsUpdate(CircleCISettings settings);
    }

    public interface ListModelUpdatedListener {
        void listUpdate();
    }

    public interface BranchFilterChangedListener {
        void branchFilterChanged();
    }

    public interface NewBuildsListener {
        void newBuilds();
    }

}
