package com.github.jenkins.lastchanges;

import com.github.jenkins.lastchanges.model.LastChanges;
import com.github.jenkins.lastchanges.model.LastChangesConfig;
import hudson.model.Action;
import hudson.model.Run;
import jenkins.tasks.SimpleBuildStep;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LastChangesBuildAction extends LastChangesBaseAction implements SimpleBuildStep.LastBuildAction {

    private final Run<?, ?> build;
    private LastChanges buildChanges;
    private LastChangesConfig config;
    private List<LastChangesProjectAction> projectActions;

    public LastChangesBuildAction(Run<?, ?> build, LastChanges lastChanges, LastChangesConfig config) {
        this.build = build;
        buildChanges = lastChanges;
        if (config == null) {
            config = new LastChangesConfig();
        }
        this.config = config;
        List<LastChangesProjectAction> projectActions = new ArrayList<>();
        projectActions.add(new LastChangesProjectAction(build.getParent()));
        this.projectActions = projectActions;
    }

    @Override
    protected String getTitle() {
        return "Last Changes of Build #" + this.build.getNumber();
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


    @Override
    public Collection<? extends Action> getProjectActions() {
        return this.projectActions;
    }
}
