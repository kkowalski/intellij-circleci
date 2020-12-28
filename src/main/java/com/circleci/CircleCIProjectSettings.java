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
@State(name = "CircleCI.ProjectSettings", storages = @Storage(value = "circleci.xml"))
public class CircleCIProjectSettings implements PersistentStateComponent<CircleCIProjectSettings> {

    public Project activeProject;
    public List<Project> projects = new ArrayList<>();

    @Override
    public CircleCIProjectSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull CircleCIProjectSettings settings) {
        XmlSerializerUtil.copyBean(settings, this);
    }

    public static CircleCIProjectSettings getInstance(com.intellij.openapi.project.Project project) {
        return ServiceManager.getService(project, CircleCIProjectSettings.class);
    }

}