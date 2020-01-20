package com.circleci;


import com.circleci.api.model.Project;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Chris Kowalski
 */
@State(name = "CircleCI.Settings", storages = @Storage(value = "circleci.xml"))
public class CircleCISettings implements PersistentStateComponent<CircleCISettings> {
    public String serverUrl;
    public String token;

    public String defaultProvider;
    public String defaultOrganization;

    public Project activeProject;
    public List<Project> projects = new ArrayList<>();

    @Override
    public CircleCISettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull CircleCISettings settings) {
        XmlSerializerUtil.copyBean(settings, this);
    }

    public static CircleCISettings getInstance() {
        return ServiceManager.getService(CircleCISettings.class);
    }

}