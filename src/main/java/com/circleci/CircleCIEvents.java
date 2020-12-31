package com.circleci;

import com.intellij.util.messages.Topic;

public class CircleCIEvents {

    public static Topic<ProjectChangedListener> PROJECT_CHANGED_TOPIC = new Topic<>("CIRCLECI_PROJECT_CHANGED", ProjectChangedListener.class);
    public static Topic<SettingsUpdatedListener> SETTINGS_UPDATED_TOPIC = new Topic<>("CIRCLECI_SETTINGS_UPDATED", SettingsUpdatedListener.class);
    public static Topic<ListUpdatedListener> LIST_UPDATED_TOPIC = new Topic<>("CIRCLECI_LIST_UPDATED", ListUpdatedListener.class);
    public static Topic<NewDataListener> NEW_DATA_TOPIC = new Topic<>("CIRCLECI_NEW_DATA_AVAILABLE", NewDataListener.class);

    public interface ProjectChangedListener {
        void projectChanged(ActiveProjectChangeEvent event);
    }

    public interface SettingsUpdatedListener {
        void settingsUpdate(CircleCISettings settings);
    }

    public interface ListUpdatedListener {
        void listUpdate();
    }

    public interface NewDataListener {
        void newData();
    }

}
