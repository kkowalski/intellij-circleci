package com.circleci;

import com.circleci.ui.ActiveProjectChangeEvent;
import com.intellij.util.messages.Topic;

public class CircleCIEvents {

    public static Topic<ProjectChangedListener> PROJECT_CHANGED_TOPIC = new Topic<>("CIRCLECI_PROJECT_CHANGED", ProjectChangedListener.class);
    public static Topic<SettingsUpdatedListener> SETTINGS_UPDATED_TOPIC = new Topic<>("CIRCLECI_SETTINGS_UPDATED", SettingsUpdatedListener.class);

    public interface ProjectChangedListener {
        void projectChanged(ActiveProjectChangeEvent event);
    }

    public interface SettingsUpdatedListener {
        void settingsUpdate(CircleCISettings settings);
    }

}
