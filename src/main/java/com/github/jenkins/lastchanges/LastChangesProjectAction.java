package com.github.jenkins.lastchanges;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.ProminentProjectAction;
import hudson.model.Run;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import jenkins.model.TransientActionFactory;

public class LastChangesProjectAction extends LastChangesBaseAction implements ProminentProjectAction {

    private final transient Job<?, ?> job;
    private final List<Run<?, ?>> lastChangesBuilds;
    private final String jobName;
    
    public LastChangesProjectAction(Job<?, ?> job, List<Run<?, ?>> lastChangesBuilds) {
        this.job = job;
        this.lastChangesBuilds = lastChangesBuilds;
        this.jobName = job.getName();
    }

    public String job() {
        return jobName;
    }

    public Job<?, ?> getProject() {
        return job;
    }

    @Override
    protected String getTitle() {
        return this.job.getDisplayName();
    }

    public List<Run<?, ?>> getLastChangesBuilds() {
        return lastChangesBuilds;
    }
    
    @Extension
    public static class LastChangesActionFactory extends TransientActionFactory<Job<?, ?>> {

        @Override
        public Collection<? extends Action> createFor(Job<?, ?> j) {
            List<Run<?, ?>> lastChangesBuilds = new ArrayList<>();

            //collects the list of builds that published last changes to show on the last changes history 
            if (j.getBuilds() != null && !j.getBuilds().isEmpty()) {
                for (Run<?, ?> build : j.getBuilds()) {
                    if (build.getAction(LastChangesBuildAction.class) != null) {
                        lastChangesBuilds.add(build);
                    }
                }
            }
            LastChangesProjectAction lastChangesProjectAction = new LastChangesProjectAction(j, lastChangesBuilds);
            return Collections.singleton(lastChangesProjectAction);
        }

        @Override
        public Class type() {
            return Job.class;
        }
    }

}
