package com.github.jenkins.lastchanges;

import hudson.model.Action;

public abstract class LastChangesBaseAction implements Action {

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
