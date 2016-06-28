/*
 * The MIT License
 *
 * Copyright 2016 rmpestano.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.jenkins.lastchanges;

import com.github.jenkins.lastchanges.exception.LastChangesException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.io.FileUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.*;
import java.nio.file.Paths;

import static com.github.jenkins.lastchanges.LastChanges.lastChanges;
import static com.github.jenkins.lastchanges.LastChanges.repository;

/**
 * @author rmpestano
 */
public class LastChangesPublisher extends Recorder implements SimpleBuildStep {

    private LastChangesProjectAction lastChangesProjectAction;
    private static final String GIT_DIR = "/.git";

    @DataBoundConstructor
    public LastChangesPublisher() {
    }

    @Override
    public Action getProjectAction(AbstractProject<?, ?> project) {
        if (lastChangesProjectAction == null) {
            lastChangesProjectAction = new LastChangesProjectAction(project);
        }
        return lastChangesProjectAction;
    }

    @Override
    public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener)
            throws IOException, InterruptedException {

        FilePath workspaceTargetDir = getMasterWorkspaceDir(build);// here we're are going to generate pretty/rich diff html from diff file (always on master)

        File gitRepoSourceDir = new File(workspace.getRemote() + GIT_DIR);//sometimes on slave
        File gitRepoTargetDir = new File(workspaceTargetDir.getRemote());//always on master

        //workspace can be on slave so copy git resources to master
        FileUtils.copyDirectoryToDirectory(gitRepoSourceDir, gitRepoTargetDir);
        //workspace.copyRecursiveTo("**/*.git", workspaceTargetDir);//not helps because it can't copy .git dir

        try {
            OutputStream diffFileStream = new FileOutputStream(new File(workspaceTargetDir + "/diff.txt"));
            lastChanges(repository(gitRepoTargetDir.getPath() + GIT_DIR), diffFileStream);
            listener.hyperlink("../" + LastChangesBaseAction.BASE_URL, "Last changes generated successfully!");
        } catch (LastChangesException e) {
            listener.error(String.format("Last Changes NOT generated for build %s due to following error", "#" + build.getNumber()), e);
        }
        //always success (only warn when no diff was generated)
        build.setResult(Result.SUCCESS);

    }

    /**
     * mainly for findbugs be happy
     *
     * @param build
     * @return
     */
    private FilePath getMasterWorkspaceDir(Run<?, ?> build) {
        if (build != null && build.getRootDir() != null) {
            return new FilePath(build.getRootDir());
        } else {
            return new FilePath(Paths.get("").toFile());
        }
    }


    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }


    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {


        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }


        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Last Changes";
        }

    }


}

