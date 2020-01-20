package com.circleci.api;

public class GetBuildsRequestParameters {
    public String provider;
    public String organization;
    public String project;
    public int limit;
    public int offset;

    public GetBuildsRequestParameters(String provider, String organization, String project, int limit, int offset) {
        this.provider = provider;
        this.organization = organization;
        this.project = project;
        this.limit = limit;
        this.offset = offset;
    }

}
