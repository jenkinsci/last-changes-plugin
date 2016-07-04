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

import static com.github.jenkins.lastchanges.impl.GitLastChanges.repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import com.github.jenkins.lastchanges.impl.GitLastChanges;
import com.github.jenkins.lastchanges.impl.SvnLastChanges;
import com.github.jenkins.lastchanges.model.LastChanges;
import org.apache.commons.io.FileUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import com.github.jenkins.lastchanges.model.FormatType;
import com.github.jenkins.lastchanges.model.LastChangesConfig;
import com.github.jenkins.lastchanges.model.MatchingType;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.SubversionSCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;

/**
 * @author rmpestano
 */
public class LastChangesPublisher extends Recorder implements SimpleBuildStep {

    private FormatType format;

    private MatchingType matching;

    private Boolean showFiles;

    private Boolean synchronisedScroll;

    private String matchWordsThreshold;

    private String matchingMaxComparisons;


    private LastChangesProjectAction lastChangesProjectAction;

    private static final String GIT_DIR = "/.git";
    private static final String SVN_DIR = "/.svn";


    @DataBoundConstructor
    public LastChangesPublisher(FormatType format, MatchingType matching, Boolean showFiles, Boolean synchronisedScroll, String matchWordsThreshold, String matchingMaxComparisons) {
        this.format = format;
        this.matching = matching;
        this.showFiles = showFiles;
        this.synchronisedScroll = synchronisedScroll;
        this.matchWordsThreshold = matchWordsThreshold;
        this.matchingMaxComparisons = matchingMaxComparisons;
    }


    @Override
    public Action getProjectAction(AbstractProject<?, ?> project) {
        if (lastChangesProjectAction == null) {
            lastChangesProjectAction = new LastChangesProjectAction(project);
        }
        return lastChangesProjectAction;
    }

    @Override
    public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {

        FilePath workspaceTargetDir = getMasterWorkspaceDir(build);// here we're are going to generate pretty/rich diff html from diff file (always on master)
        boolean isGit = new File(workspace.getRemote() + GIT_DIR).exists();
        boolean isSvn = new File(workspace.getRemote() + SVN_DIR).exists();
        if (!isGit && !isSvn) {
            throw new RuntimeException("Not git or svn repository found at " + workspace.getRemote());
        }
       
        File repoTargetDir = new File(workspaceTargetDir.getRemote());//always on master

        try {
        	 File sourceDir = null;
             if (isGit) {
                 sourceDir = new File(workspace.getRemote() + GIT_DIR);
                 //workspace can be on slave so copy resources to master
                 FileUtils.copyDirectoryToDirectory(sourceDir, repoTargetDir);
                 //workspace.copyRecursiveTo("**/*", workspaceTargetDir);//not helps because it can't copy .git dir
             }

            LastChanges lastChanges = null;
            if (isGit) {
                lastChanges = GitLastChanges.of(repository(repoTargetDir.getPath() + GIT_DIR));
            } else {
            	AbstractProject<?, ?> rootProject = (AbstractProject<?,?>)lastChangesProjectAction.getProject();
                	SubversionSCM scm = SubversionSCM.class.cast(rootProject.getScm());
                	lastChanges = SvnLastChanges.of(SvnLastChanges.repository(scm.getLocations()[0].getURL()));
            }  

            listener.hyperlink("../" + build.getNumber() + "/" + LastChangesBaseAction.BASE_URL, "Last changes generated successfully!");
            listener.getLogger().println("");
            build.addAction(new LastChangesBuildAction(build, lastChanges, new LastChangesConfig(format, matching, showFiles, synchronisedScroll, matchWordsThreshold, matchingMaxComparisons)));
        } catch (Exception e) {
            listener.error("Last Changes NOT published due to the following error: " + e.getMessage());
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
            return "Publish Last Changes";
        }

        public ListBoxModel doFillFormatItems() {
            ListBoxModel items = new ListBoxModel();
            for (FormatType formatType : FormatType.values()) {
                items.add(formatType.getFormat(), formatType.name());
            }
            return items;
        }

        public ListBoxModel doFillMatchingItems() {
            ListBoxModel items = new ListBoxModel();
            for (MatchingType matchingType : MatchingType.values()) {
                items.add(matchingType.getMatching(), matchingType.name());
            }
            return items;
        }


    }

    public FormatType getFormat() {
        return format;
    }


    public MatchingType getMatching() {
        return matching;
    }


    public String getMatchWordsThreshold() {
        return matchWordsThreshold;
    }


    public String getMatchingMaxComparisons() {
        return matchingMaxComparisons;
    }


    public Boolean getShowFiles() {
        return showFiles;
    }


    public Boolean getSynchronisedScroll() {
        return synchronisedScroll;
    }


}
