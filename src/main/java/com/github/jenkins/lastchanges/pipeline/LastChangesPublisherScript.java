package com.github.jenkins.lastchanges.pipeline;

import com.github.jenkins.lastchanges.LastChangesPublisher;
import com.github.jenkins.lastchanges.model.LastChanges;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.cps.CpsScript;

import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class LastChangesPublisherScript implements Serializable {

    private CpsScript cpsScript;
    private LastChangesPublisher publisher;

    public LastChangesPublisherScript(LastChangesPublisher publisher) {
        this.publisher = publisher;
    }

    public void setCpsScript(CpsScript cpsScript) {
        this.cpsScript = cpsScript;
    }

    public void doPublish(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
        this.publisher.perform(build, workspace, launcher, listener);
    }

    @Whitelisted
    public LastChangesPublisherScript publishLastChanges () throws Exception {
        Map<String, Object> stepVariables = new LinkedHashMap<>();
        stepVariables.put("publisher", this);
        LastChangesPublisherScript buildInfo = (LastChangesPublisherScript) cpsScript.invokeMethod("publishLastChanges", stepVariables);
        buildInfo.setCpsScript(cpsScript);
        return buildInfo;
    }

    @Whitelisted
    public LastChanges getLastChanges () throws Exception {
        return this.publisher.getLastChanges();
    }
}