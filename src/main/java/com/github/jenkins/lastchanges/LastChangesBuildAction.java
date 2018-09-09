package com.github.jenkins.lastchanges;

import com.github.jenkins.lastchanges.model.CommitChanges;
import com.github.jenkins.lastchanges.model.LastChanges;
import com.github.jenkins.lastchanges.model.LastChangesConfig;
import hudson.model.Run;
import jenkins.model.RunAction2;

public class LastChangesBuildAction extends LastChangesBaseAction implements RunAction2 {

    private transient Run<?, ?> build;
    private final LastChanges buildChanges;
    private final LastChangesConfig config;

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
    
    public DownloadRenderer getDownload() {
        return new DownloadRenderer(buildChanges, build.getFullDisplayName().replace(" ", ""));
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
