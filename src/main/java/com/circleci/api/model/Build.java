package com.circleci.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties
public class Build {

    String subject;
    User user;
    @JsonProperty("vcs_url")
    String vcsURL;
    String status;
    @JsonProperty("build_num")
    Integer buildNumber;
    String branch;
    @JsonProperty("author_name")
    String author;
    @JsonProperty("build_url")
    String url;
    @JsonProperty("vcs_type")
    String vcsType;
    @JsonProperty("reponame")
    String project;
    @JsonProperty("username")
    String organization;

    Workflows workflows;

    // It seems it doesn't exists anymore
    @JsonProperty("pull_requests")
    PullRequests pullRequests;

    @JsonProperty("all_commit_details")
    List<CommitDetails> allCommitDetails;

    public Build() {
    }

    public PullRequests getPullRequests() {
        return pullRequests;
    }

    public void setPullRequests(PullRequests pullRequests) {
        this.pullRequests = pullRequests;
    }

    public Workflows getWorkflows() {
        return workflows;
    }

    public void setWorkflows(Workflows workflows) {
        this.workflows = workflows;
    }

    public String getVcsURL() {
        return vcsURL;
    }

    public void setVcsURL(String vcsURL) {
        this.vcsURL = vcsURL;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(Integer buildNumber) {
        this.buildNumber = buildNumber;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<CommitDetails> getAllCommitDetails() {
        return allCommitDetails;
    }

    public void setAllCommitDetails(List<CommitDetails> allCommitDetails) {
        this.allCommitDetails = allCommitDetails;
    }

    public String getVcsType() {
        return vcsType;
    }

    public void setVcsType(String vcsType) {
        this.vcsType = vcsType;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Build build = (Build) o;
        return Objects.equals(buildNumber, build.buildNumber) &&
                Objects.equals(url, build.url) &&
                Objects.equals(vcsType, build.vcsType) &&
                Objects.equals(project, build.project) &&
                Objects.equals(organization, build.organization);
    }

    @Override
    public int hashCode() {
        return Objects.hash(buildNumber, url, vcsType, project, organization);
    }

    public String getFilterableText() {
        return new StringBuilder()
                .append(getBranch())
                .append(getBuildNumber())
                .append(getSubject())
                .append(getUser() != null ? getUser().getLogin() : "")
                .append(getWorkflows() != null ? getWorkflows().getJobName() : "")
                .toString();
    }
}