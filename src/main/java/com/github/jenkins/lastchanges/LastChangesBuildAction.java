package com.github.jenkins.lastchanges;

import hudson.model.Run;

import java.io.File;

public class LastChangesBuildAction extends LastChangesBaseAction {

    private final Run<?, ?> build;

    public LastChangesBuildAction(Run<?, ?> build) {
        this.build = build;
    }

    @Override
    protected String getTitle() {
        return this.build.getDisplayName();
    }

    @Override
    protected File dir() {
        return new File(build.getRootDir(), BASE_URL);
    }
}
