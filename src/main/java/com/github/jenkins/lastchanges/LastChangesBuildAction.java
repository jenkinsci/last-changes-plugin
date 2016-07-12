package com.github.jenkins.lastchanges;

import java.io.File;

import com.github.jenkins.lastchanges.model.LastChanges;
import com.github.jenkins.lastchanges.model.LastChangesConfig;

import hudson.model.Run;

public class LastChangesBuildAction extends LastChangesBaseAction {

    private final Run<?, ?> build;
    private LastChanges buildChanges;
    private LastChangesConfig config;

    public LastChangesBuildAction(Run<?, ?> build, LastChanges lastChanges, LastChangesConfig config) {
        this.build = build;
        buildChanges = lastChanges;
        if (config == null) {
            config = new LastChangesConfig();
        }
        this.config = config;
    }

    @Override
    protected String getTitle() {
        return "Last Changes of Build #" + this.build.getNumber();
    }

    @Override
    protected File dir() {
        return new File(build.getRootDir(), BASE_URL);
    }

    public LastChanges getBuildChanges() {
        return buildChanges;
    }

    public Run<?, ?> getBuild() {
        return build;
    }

    public LastChangesConfig getConfig() {
        return config;
    }


}
