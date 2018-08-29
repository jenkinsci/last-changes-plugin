package com.github.jenkins.lastchanges;

import com.github.jenkins.lastchanges.model.CommitChanges;
import com.github.jenkins.lastchanges.model.LastChanges;
import com.github.jenkins.lastchanges.model.LastChangesConfig;
import hudson.model.Action;
import hudson.model.Run;
import jenkins.model.RunAction2;
import jenkins.tasks.SimpleBuildStep;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LastChangesBuildAction extends LastChangesBaseAction implements SimpleBuildStep.LastBuildAction, RunAction2 {

    private transient Run<?, ?> build;
    private final LastChanges buildChanges;
    private final LastChangesConfig config;
    private final List<LastChangesProjectAction> projectActions;

    public LastChangesBuildAction(Run<?, ?> build, LastChanges lastChanges, LastChangesConfig config) {
        this.build = build;
        buildChanges = lastChanges;
        if (config == null) {
            config = new LastChangesConfig();
        }
        this.config = config;
        projectActions = new ArrayList<>();
        projectActions.add(new LastChangesProjectAction(build.getParent(), null));
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

    public CommitRenderer getCommit(String commitId) {

        CommitChanges commit = null;
        for (CommitChanges commitChanges : buildChanges.getCommits()) {
            if(commitId.equals(commitChanges.getCommitInfo().getCommitId())) {
                commit = commitChanges;
                break;
            }
        }

        return new CommitRenderer(this, commit);
    }


    @Override
    public Collection<? extends Action> getProjectActions() {
        return this.projectActions;
    }

    @Override
    public void onAttached(Run<?, ?> run) {
        build = run;
    }

    @Override
    public void onLoad(Run<?, ?> run) {
        onAttached(run);
    }
}
