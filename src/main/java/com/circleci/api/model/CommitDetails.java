package com.circleci.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CommitDetails {
    private String commitUrl;
    @JsonProperty("commit")
    private String commitHash;

    public CommitDetails() {
    }

    public String getCommitUrl() {
        return commitUrl;
    }

    public void setCommitUrl(String commitUrl) {
        this.commitUrl = commitUrl;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public void setCommitHash(String commitHash) {
        this.commitHash = commitHash;
    }
}