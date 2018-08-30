package com.github.jenkins.lastchanges;

public abstract class LastChangesBaseAction {

    protected static final String BASE_URL = "last-changes";

    public String getUrlName() {
        return BASE_URL;
    }

    public String getDisplayName() {
        return "View Last Changes";
    }

    public String getIconFileName() {
        return "/plugin/last-changes/git.png";
    }


    protected abstract String getTitle();


}
