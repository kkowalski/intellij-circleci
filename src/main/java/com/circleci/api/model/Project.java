package com.circleci.api.model;

import java.util.Objects;

public class Project {

    public String organization;
    public String name;
    public String provider;

    public Project() {
    }

    public Project(String organization, String name, String provider) {
        this.organization = organization;
        this.name = name;
        this.provider = provider;
    }

    public String getOrganization() {
        return organization;
    }

    public String getName() {
        return name;
    }

    public String getProvider() {
        return provider;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    @Override
    public String toString() {
        return organization + "/" + name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Project project = (Project) o;
        return organization.equals(project.organization) &&
                name.equals(project.name) &&
                provider.equals(project.provider);
    }

    @Override
    public int hashCode() {
        return Objects.hash(organization, name, provider);
    }
}
