package com.github.jenkins.lastchanges;

import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.ProminentProjectAction;
import hudson.model.Run;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LastChangesProjectAction extends LastChangesBaseAction implements ProminentProjectAction {

    private static final String LAST_CHANGES_PAGE = "sample.html";
    private final AbstractProject<?, ?> project;

    private String jobName;

    public LastChangesProjectAction(AbstractProject<?, ?> project) {
        this.project = project;
    }

    public String job(){
        if(jobName == null){
            jobName = project.getName();
        }
        return jobName;
    }

    public Job<?, ?> getProject() {
        return project;
    }

    @Override
    protected File dir() {
        Run<?, ?> run = this.project.getLastCompletedBuild();
        if (run != null) {
            File archiveDir = getBuildArchiveDir(run);

            if (archiveDir.exists()) {
                return archiveDir;
            }
        }

        return getProjectArchiveDir();
    }

    private File getProjectArchiveDir() {
        return new File(project.getRootDir(), LastChangesBaseAction.BASE_URL);
    }

    /** Gets the directory where the HTML report is stored for the given build. */
    private File getBuildArchiveDir(Run<?, ?> run) {
        return new File(run.getRootDir(), LastChangesBaseAction.BASE_URL);
    }

    @Override
    protected String getTitle() {
        return this.project.getDisplayName();
    }

    public Map<Run<?, ?>, List<String>> getLastChanges() {
        Map<Run<?, ?>, List<String>> changes = new LinkedHashMap<Run<?, ?>, List<String>>();

     /*   for (Run<?, ?> build : project.getBuilds()) {
            LastChangesBuildAction action = build.getAction(LastChangesBuildAction.class);
            if (action != null) {
                List<String> simNames = new ArrayList<String>();
                for (BuildSimulation sim : action.getDiff()) {
                    simNames.add(sim.getSimulationName());
                }
                reports.put(build, simNames);
            }
        }*/

        return changes;
    }

}