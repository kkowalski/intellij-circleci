package com.circleci.ui;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

/**
 * @author Chris Kowalski
 */
public class CircleCIIcons {
    public static Icon SUCCESS_BUILD = IconLoader.getIcon("/icons/build-status/status-success.svg", CircleCIIcons.class);
    public static Icon FAILED_BUILD = IconLoader.getIcon("/icons/build-status/status-failure.svg", CircleCIIcons.class);
    public static Icon RUNNING_BUILD = IconLoader.getIcon("/icons/build-status/status-running.svg", CircleCIIcons.class);
    public static Icon ON_HOLD = IconLoader.getIcon("/icons/build-status/status-on-hold.svg", CircleCIIcons.class);
    public static Icon CANCELED_BUILD = IconLoader.getIcon("/icons/build-status/status-canceled.svg", CircleCIIcons.class);
    public static Icon SETUP_NEEDED_BUILD = IconLoader.getIcon("/icons/build-status/status-setup-needed.svg", CircleCIIcons.class );
}
