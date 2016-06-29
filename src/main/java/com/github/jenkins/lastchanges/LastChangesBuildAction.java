package com.github.jenkins.lastchanges;

import com.github.jenkins.lastchanges.model.LastChanges;
import hudson.model.Run;

import java.io.File;

public class LastChangesBuildAction extends LastChangesBaseAction {

    private final Run<?, ?> build;
    private LastChanges buildChanges;

    public LastChangesBuildAction(Run<?, ?> build, LastChanges lastChanges) {
        this.build = build;
        buildChanges = lastChanges;
    }

    @Override
    protected String getTitle() {
        return "Last Changes of Build #"+this.build.getNumber();
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
}
