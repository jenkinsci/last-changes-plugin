package com.github.jenkins.lastchanges.pipeline;

import com.google.inject.Inject;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;

public class PublishLastChangesStep extends AbstractStepImpl {

    private final LastChangesPublisherScript publisher;

    @DataBoundConstructor
    public PublishLastChangesStep(LastChangesPublisherScript publisher) {
        this.publisher = publisher;
    }

    public static class Execution extends AbstractSynchronousStepExecution<LastChangesPublisherScript> {

        @StepContextParameter
        private transient FilePath ws;

        @StepContextParameter
        private transient Run build;

        @StepContextParameter
        private transient Launcher launcher;

        @StepContextParameter
        private transient TaskListener listener;

        @Inject(optional = true)
        private transient PublishLastChangesStep step;

        @Override
        protected LastChangesPublisherScript run() throws Exception {
            step.publisher.doPublish (build, ws, launcher, listener);
            return step.publisher;
        }
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "publishLastChanges";
        }

        @Override
        public String getDisplayName() {
            return "publish the changes";
        }

    }

}