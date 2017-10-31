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

import com.github.jenkins.lastchanges.impl.GitLastChanges;
import com.github.jenkins.lastchanges.impl.SvnLastChanges;
import com.github.jenkins.lastchanges.model.FormatType;
import com.github.jenkins.lastchanges.model.LastChanges;
import com.github.jenkins.lastchanges.model.LastChangesConfig;
import com.github.jenkins.lastchanges.model.MatchingType;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;
import hudson.scm.SubversionSCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import jenkins.triggers.SCMTriggerItem;
import org.eclipse.jgit.lib.Repository;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.tmatesoft.svn.core.io.SVNRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;

import static com.github.jenkins.lastchanges.impl.GitLastChanges.repository;

/**
 * @author rmpestano
 */
public class LastChangesPublisher extends Recorder implements SimpleBuildStep {

    private static final short RECURSION_DEPTH = 50;

    private String previousRevision;

    private FormatType format;

    private MatchingType matching;

    private Boolean showFiles;

    private Boolean synchronisedScroll;

    private Boolean sinceLastSuccessfulBuild;

    private String matchWordsThreshold;

    private String matchingMaxComparisons;

    private static final String GIT_DIR = ".git";

    @DataBoundConstructor
    public LastChangesPublisher(FormatType format, MatchingType matching, Boolean showFiles, Boolean synchronisedScroll, String matchWordsThreshold,
                                String matchingMaxComparisons, String previousRevision, Boolean sinceLastSuccessfulBuild) {
        this.previousRevision = previousRevision;
        this.format = format;
        this.matching = matching;
        this.showFiles = showFiles;
        this.synchronisedScroll = synchronisedScroll;
        this.matchWordsThreshold = matchWordsThreshold;
        this.matchingMaxComparisons = matchingMaxComparisons;
        this.sinceLastSuccessfulBuild = sinceLastSuccessfulBuild;
    }


    @Override
    public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {

        LastChangesProjectAction projectAction = new LastChangesProjectAction(build.getParent());
        boolean isGit = false;
        boolean isSvn = false;

        Collection<? extends SCM> scms = SCMTriggerItem.SCMTriggerItems.asSCMTriggerItem(projectAction.getProject()).getSCMs();
        for (SCM scm : scms) {
            if (scm instanceof GitSCM) {
                isGit = true;
                break;
            }

            if (scm instanceof SubversionSCM) {
                isSvn = true;
                break;
            }
        }

        if (!isGit && !isSvn) {
            //if scm is not found try to find .git dir.
            //Note that last changes does depend on a SCM only for Subversion, in git we can retrieve repository information from .git
            if (workspace.child(GIT_DIR).exists() || findGitDir(workspace) != null) {
                isGit = true;
            } else {
                throw new RuntimeException("Git or Svn must be configured as SCM on your job to publish Last Changes. Ignore this message and RERUN your job if you're using SVN on a Jenkins pipeline workflow for the first time. (See JENKINS-45720 for more details).");
            }

        }

        FilePath workspaceTargetDir = getMasterWorkspaceDir(build);//always on master

        boolean hasPreviousRevision = false;
        String previousRevisionExpanded = null;
        final EnvVars env = build.getEnvironment(listener);
        if (previousRevision != null) {
            previousRevisionExpanded = env.expand(previousRevision);
        }

        if (sinceLastSuccessfulBuild != null && sinceLastSuccessfulBuild && projectAction.getProject().getLastSuccessfulBuild() != null) {
            LastChangesBuildAction action = projectAction.getProject().getLastSuccessfulBuild().getAction(LastChangesBuildAction.class);
            if (action != null && action.getBuildChanges().getCurrentRevision() != null) {
                previousRevision = action.getBuildChanges().getCurrentRevision().getCommitId();
                previousRevisionExpanded = previousRevision;
            }
        }

        hasPreviousRevision = previousRevisionExpanded != null && !"".equals(previousRevisionExpanded);

        try {
            LastChanges lastChanges = null;
            listener.getLogger().println("Publishing build last changes...");
            if (isGit) {
                FilePath gitDir = workspace.child(GIT_DIR).exists() ? workspace.child(GIT_DIR) : findGitDir(workspace);
                if(gitDir == null) {
                    throw new RuntimeException("No .git directory found in workspace.");
                }
                // workspace can be on slave so copy resources to master
                // we are only copying when on git because in svn we are reading
                // the current revision from remote repository
                gitDir.copyRecursiveTo("**/*", new FilePath(new File(workspaceTargetDir.getRemote() + "/.git")));
                if (hasPreviousRevision) {
                    //compares current repository revision with provided previousRevision
                    Repository repository = repository(workspaceTargetDir.getRemote() + "/.git");
                    lastChanges = GitLastChanges.getInstance().changesOf(repository, GitLastChanges.resolveCurrentRevision(repository), repository.resolve(previousRevisionExpanded));
                } else {
                    //compares current repository revision with previous one
                    lastChanges = GitLastChanges.getInstance().changesOf(repository(workspaceTargetDir.getRemote() + "/.git"));
                }
            } else {
                //svn repository
                SubversionSCM scm = (SubversionSCM) SCMTriggerItem.SCMTriggerItems
                        .asSCMTriggerItem(projectAction.getProject()).getSCMs().iterator().next();

                if (hasPreviousRevision) {
                    //compares current repository revision with provided previousRevision
                    Long svnRevision = Long.parseLong(previousRevisionExpanded);
                    SVNRepository repository = SvnLastChanges.repository(scm, projectAction.getProject(), env);
                    lastChanges = SvnLastChanges.getInstance().changesOf(repository, repository.getLatestRevision(), svnRevision);
                } else {
                    //compares current repository revision with previous one
                    lastChanges = SvnLastChanges.getInstance().changesOf(SvnLastChanges.repository(scm, projectAction.getProject(), env));
                }
            }

            String resultMessage = String.format("Last changes from revision %s to %s published successfully!", truncate(lastChanges.getCurrentRevision().getCommitId(), 8), truncate(lastChanges.getPreviousRevision().getCommitId(), 8));
            listener.hyperlink("../" + build.getNumber() + "/" + LastChangesBaseAction.BASE_URL, resultMessage);
            listener.getLogger().println("");

            build.addAction(new LastChangesBuildAction(build, lastChanges,
                    new LastChangesConfig(previousRevision, sinceLastSuccessfulBuild, format, matching, showFiles, synchronisedScroll, matchWordsThreshold, matchingMaxComparisons)));
        } catch (Exception e) {
            listener.error("Last Changes NOT published due to the following error: " + (e.getMessage() == null ? e.toString() : e.getMessage()) + (e.getCause() != null ? " - " + e.getCause() : ""));
            e.printStackTrace();
        }
        // always success (only warn when no diff was generated)

        build.setResult(Result.SUCCESS);

    }

    private String truncate(String value, int length) {
        if (value == null || value.length() <= length) {
            return value;
        }

        return value.substring(0, length - 1);
    }

    /**
     * .git directory can be on a workspace sub dir, see JENKINS-36971
     */
    private FilePath findGitDir(FilePath workspace) throws IOException, InterruptedException {
        FilePath gitDir = null;
        int recursionDepth = RECURSION_DEPTH;
        while ((gitDir = findGitDirInSubDirectories(workspace)) == null && recursionDepth > 0) {
            recursionDepth--;
        }
        return gitDir;
    }

    private FilePath findGitDirInSubDirectories(FilePath sourceDir) throws IOException, InterruptedException {
        for (FilePath filePath : sourceDir.listDirectories()) {
            if (filePath.getName().equalsIgnoreCase(GIT_DIR)) {
                return filePath;
            } else {
                return findGitDirInSubDirectories(filePath);
            }
        }
        return null;
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
    @Symbol("lastChanges")
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project
            // types
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Publish Last Changes";
        }

        @Restricted(NoExternalUse.class) // Only for UI calls
        public ListBoxModel doFillFormatItems() {
            ListBoxModel items = new ListBoxModel();
            for (FormatType formatType : FormatType.values()) {
                items.add(formatType.getFormat(), formatType.name());
            }
            return items;
        }

        @Restricted(NoExternalUse.class) // Only for UI calls
        public ListBoxModel doFillMatchingItems() {
            ListBoxModel items = new ListBoxModel();
            for (MatchingType matchingType : MatchingType.values()) {
                items.add(matchingType.getMatching(), matchingType.name());
            }
            return items;
        }

    }

    public String getPreviousRevision() {
        return previousRevision;
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

    public Boolean getSinceLastSuccessfulBuild() {
        return sinceLastSuccessfulBuild;
    }

    @DataBoundSetter
    public void setPreviousRevision(String previousRevision) {
        this.previousRevision = previousRevision;
    }

    @DataBoundSetter
    public void setSinceLastSuccessfulBuild(Boolean sinceLastSuccessfulBuild) {
        this.sinceLastSuccessfulBuild = sinceLastSuccessfulBuild;
    }

    @DataBoundSetter
    public void setFormat(FormatType format) {
        this.format = format;
    }

    @DataBoundSetter
    public void setMatching(MatchingType matching) {
        this.matching = matching;
    }

    @DataBoundSetter
    public void setMatchingMaxComparisons(String matchingMaxComparisons) {
        this.matchingMaxComparisons = matchingMaxComparisons;
    }

    @DataBoundSetter
    public void setMatchWordsThreshold(String matchWordsThreshold) {
        this.matchWordsThreshold = matchWordsThreshold;
    }

    @DataBoundSetter
    public void setShowFiles(Boolean showFiles) {
        this.showFiles = showFiles;
    }

    @DataBoundSetter
    public void setSynchronisedScroll(Boolean synchronisedScroll) {
        this.synchronisedScroll = synchronisedScroll;
    }
}
