package com.circleci.api.model;

public class Me {
    private String login;
    private String basicEmailPrefs;

    public Me() {
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getBasicEmailPrefs() {
        return basicEmailPrefs;
    }

    public void setBasicEmailPrefs(String basicEmailPrefs) {
        this.basicEmailPrefs = basicEmailPrefs;
    }
}