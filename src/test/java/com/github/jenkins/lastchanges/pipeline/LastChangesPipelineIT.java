package com.github.jenkins.lastchanges.pipeline;

import hudson.model.Result;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Arrays;

public class LastChangesPipelineIT {

    @Rule
    public JenkinsRule j = new JenkinsRule();


    @Test
    public void shouldPublishLastChangesViaPipelineScript() throws Exception {
        WorkflowJob job = j.jenkins.createProject(WorkflowJob.class, "last-changes");
        job.setDefinition(new CpsFlowDefinition(StringUtils.join(Arrays.asList(
                "node {",
                "  git url: 'https://github.com/jenkinsci/last-changes-plugin.git'",
                "  def publisher = LastChanges.getLastChangesPublisher \"PREVIOUS_REVISION\", \"SIDE\", \"LINE\", true, true, \"\", \"\", \"\", \"\", \"\"",
                "  publisher.publishLastChanges()",
                "}"), "\n"),true));
        WorkflowRun run = j.assertBuildStatusSuccess(job.scheduleBuild2(0).get());
        j.assertLogContains("Last changes from revision", run);
        j.assertLogContains("published successfully!", run);
    }


    @Test
    public void shouldNotPublishLastChangesViaPipelineWithoutScm() throws Exception {
        WorkflowJob job = j.jenkins.createProject(WorkflowJob.class, "last-changes");
        job.setDefinition(new CpsFlowDefinition(StringUtils.join(Arrays.asList(
                "node {",
                "  def publisher = LastChanges.getLastChangesPublisher \"PREVIOUS_REVISION\", \"SIDE\", \"LINE\", true, true, \"\", \"\", \"\", \"\", \"\"",
                "  publisher.publishLastChanges()",
                "}"), "\n"),true));
        WorkflowRun run = j.assertBuildStatus(Result.FAILURE,job.scheduleBuild2(0).get());
        j.assertLogContains("Git or Svn directories not found in workspace", run);
    }

}
