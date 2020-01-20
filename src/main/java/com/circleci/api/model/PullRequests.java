package com.circleci.api.model;

// Sadly doesn't exist anymore in api v1
public class PullRequests {

    public String url;

    public PullRequests() {
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}
