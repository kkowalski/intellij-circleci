package com.circleci.ui;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

/**
 * @author Chris Kowalski
 */
public class CircleCIIcons {
    public static Icon SUCCESS_BUILD = IconLoader.getIcon("/icons/build-status/status-success.svg");
    public static Icon FAILED_BUILD = IconLoader.getIcon("/icons/build-status/status-failure.svg");
    public static Icon RUNNING_BUILD = IconLoader.getIcon("/icons/build-status/status-running.svg");
    public static Icon ON_HOLD = IconLoader.getIcon("/icons/build-status/status-on-hold.svg");
    public static Icon CANCELED_BUILD = IconLoader.getIcon("/icons/build-status/status-canceled.svg");
    public static Icon SETUP_NEEDED_BUILD = IconLoader.getIcon("/icons/build-status/status-setup-needed.svg");
}
