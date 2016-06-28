package com.github.jenkins.lastchanges;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.slaves.DumbSlave;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

;

public class LastChangesIT {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private Collection<File> workspaceFiles;

    @Before
    public void init(){
        if(workspaceFiles == null){
            String repoPath = LastChangesTest.class.getResource("/git-sample-repo").getFile();
            Collection<File> files = FileUtils.listFilesAndDirs(new File(repoPath), new RegexFileFilter("^(.*?)"),
                    DirectoryFileFilter.DIRECTORY);
        }
    }


    @Test
    public void shouldGenerateDiffFile() throws Exception {

        // given
        MultipleFileSCM scm = new MultipleFileSCM("/.git",workspaceFiles);
        FreeStyleProject project = jenkins.createFreeStyleProject("test");
        project.setScm(scm);
        LastChangesPublisher publisher = new LastChangesPublisher();
        project.getPublishersList().add(publisher);
        project.save();

        // when
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

        // then
        File generatedDiff = new File(build.getRootDir().toURI().getPath()+"/diff.txt");
        Assert.assertTrue(generatedDiff.exists());
        jenkins.assertLogContains("Last changes generated successfully!",build);

    }

    @Test
    public void shouldGenerateDiffFileOnSlaveNode() throws Exception {

        // given
        MultipleFileSCM scm = new MultipleFileSCM("/.git",workspaceFiles);
        DumbSlave slave = jenkins.createOnlineSlave();
        FreeStyleProject project = jenkins.createFreeStyleProject("test-slave");
        project.setAssignedNode(slave);
        project.setScm(scm);
        LastChangesPublisher publisher = new LastChangesPublisher();
        project.getPublishersList().add(publisher);
        project.save();

        // when
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

        // then
        File generatedDiff = new File(build.getRootDir().toURI().getPath()+"/diff.txt");
        Assert.assertTrue(generatedDiff.exists());
        jenkins.assertLogContains("Last changes generated successfully!", build);

    }

    private void copyGitSampleRepoInto(FreeStyleBuild lastBuild) throws IOException {
        String repoPath = LastChangesTest.class.getResource("/git-sample-repo").getFile();
        Collection<File> files = FileUtils.listFilesAndDirs(new File(repoPath), new RegexFileFilter("^(.*?)"),
                DirectoryFileFilter.DIRECTORY);
        File buildDir = lastBuild.getRootDir();
        File gitDir = new File(buildDir.getAbsolutePath() + "/.git");
        gitDir.setExecutable(true);
        gitDir.setReadable(true);
        gitDir.mkdirs();
        for (File file : files) {
            if (file.isDirectory()) {
                FileUtils.copyDirectory(file, gitDir);
            } else {
                FileUtils.copyFileToDirectory(file, gitDir);
            }
        }
    }

}
