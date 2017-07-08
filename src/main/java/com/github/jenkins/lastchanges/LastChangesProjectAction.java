package com.github.jenkins.lastchanges;

import hudson.model.Job;
import hudson.model.ProminentProjectAction;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LastChangesProjectAction extends LastChangesBaseAction implements ProminentProjectAction {

    private final Job<?, ?> job;

    private String jobName;

    public LastChangesProjectAction(Job<?, ?> job) {
        this.job = job;
    }

    public String job() {
        if (jobName == null) {
            jobName = job.getName();
        }
        return jobName;
    }

    public Job<?, ?> getProject() {
        return job;
    }

    @Override
    protected File dir() {
        Run<?, ?> run = this.job.getLastCompletedBuild();
        if (run != null) {
            File archiveDir = getBuildArchiveDir(run);

            if (archiveDir.exists()) {
                return archiveDir;
            }
        }

        return getProjectArchiveDir();
    }

    private File getProjectArchiveDir() {
        return new File(job.getRootDir(), LastChangesBaseAction.BASE_URL);
    }

    /**
     * Gets the directory where the HTML report is stored for the given build.
     */
    private File getBuildArchiveDir(Run<?, ?> run) {
        return new File(run.getRootDir(), LastChangesBaseAction.BASE_URL);
    }

    @Override
    protected String getTitle() {
        return this.job.getDisplayName();
    }

    public List<Run<?, ?>> getLastChangesBuilds() {
        List<Run<?, ?>> builds = new ArrayList<>();

        for (Run<?, ?> build : job.getBuilds()) {
            LastChangesBuildAction action = build.getAction(LastChangesBuildAction.class);
            if (action != null) {
                builds.add(build);
            }
        }

        return builds;
    }

    public boolean isRunningInPipelineWorkflow() {
        return getProject() != null && getProject() instanceof WorkflowJob;
    }
}