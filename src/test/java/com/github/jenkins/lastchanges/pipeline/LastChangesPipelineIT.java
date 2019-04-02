package com.github.jenkins.lastchanges.pipeline;

import hudson.FilePath;
import hudson.model.Result;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.graph.FlowGraphWalker;
import org.jenkinsci.plugins.workflow.graphanalysis.DepthFirstScanner;
import org.jenkinsci.plugins.workflow.graphanalysis.NodeStepTypePredicate;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import static org.assertj.core.api.Assertions.assertThat;
import java.io.File;
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
    
    @Test
    public void shouldGetHtmlDiffViaPipeline() throws Exception {
        WorkflowJob job = j.jenkins.createProject(WorkflowJob.class, "last-changes");
        job.setDefinition(new CpsFlowDefinition(StringUtils.join(Arrays.asList(
                "pipeline {\n" + 
                "    agent any\n" + 
                "    stages {\n" + 
                "        stage('Checkout') {\n" + 
                "            steps {\n" + 
                "                git 'https://github.com/jenkinsci/last-changes-plugin.git'\n" + 
                "                script {\n" + 
                "                  def publisher = LastChanges.getLastChangesPublisher \"PREVIOUS_REVISION\", \"SIDE\", \"LINE\", true, true, \"\", \"\", \"\", \"\", \"\"\n" + 
                "                  publisher.publishLastChanges()\n" + 
                "                  def htmlDiff = publisher.getHtmlDiff()\n" + 
                "                  writeFile file: 'build-diff.html', text: htmlDiff\n" + 
                "                } //end script\n" + 
                "            }\n" + 
                "        }\n" + 
                "    }\n" + 
                "}"), "\n"),true));
        WorkflowRun run = j.assertBuildStatusSuccess(job.scheduleBuild2(0).get());
        j.assertLogContains("Last changes from revision", run);
        j.assertLogContains("published successfully!", run);
        FilePath htmlDiff = j.jenkins.getWorkspaceFor(job).child("build-diff.html");
        assertThat(htmlDiff.exists()).isTrue();
    }

}
