package com.circleci.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Workflows {
    @JsonProperty("workflow_id")
    String id;
    @JsonProperty("workflow_name")
    String name;
    @JsonProperty("job_name")
    String jobName;
    @JsonProperty("job_id")
    String jobId;

    public Workflows() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }
}
