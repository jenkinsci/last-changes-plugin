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
import com.github.jenkins.lastchanges.model.*;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.scm.SCM;
import hudson.scm.SubversionSCM;
import hudson.slaves.SlaveComputer;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.RunList;
import jenkins.tasks.SimpleBuildStep;
import jenkins.triggers.SCMTriggerItem;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationProvider;
import org.tmatesoft.svn.core.wc.SVNRevision;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import static com.github.jenkins.lastchanges.impl.GitLastChanges.repository;

/**
 * @author rmpestano
 */
public class LastChangesPublisher extends Recorder implements SimpleBuildStep {

    private static final short RECURSION_DEPTH = 50;

    private String specificRevision; //revision id to crete the diff

    private String specificBuild; // create the diff with the revision of an specific build

    private SinceType since;//since when you want the get last changes

    private FormatType format;

    private MatchingType matching;

    private String vcsDir;//directory relative to workspace to start searching for the VCS directory (.git or .svn)

    private Boolean showFiles;

    private Boolean synchronisedScroll;

    private String matchWordsThreshold;

    private String matchingMaxComparisons;

    private static final String GIT_DIR = ".git";
    private static final String SVN_DIR = ".svn";

    private transient FilePath vcsDirFound = null; //location of vcs directory (.git or .svn) in job workspace (is here for caching purposes)

    @DataBoundConstructor
    public LastChangesPublisher(SinceType since, FormatType format, MatchingType matching, Boolean showFiles, Boolean synchronisedScroll, String matchWordsThreshold,
                                String matchingMaxComparisons, String specificRevision, String vcsDir, String specificBuild) {
        this.specificRevision = specificRevision;
        this.format = format;
        this.since = since;
        this.matching = matching;
        this.showFiles = showFiles;
        this.synchronisedScroll = synchronisedScroll;
        this.matchWordsThreshold = matchWordsThreshold;
        this.matchingMaxComparisons = matchingMaxComparisons;
        this.vcsDir = vcsDir;
        this.specificBuild = specificBuild;
    }

    @Override
    public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {

        LastChangesProjectAction projectAction = new LastChangesProjectAction(build.getParent());
        boolean isGit = false;
        boolean isSvn = false;
        Repository gitRepository = null;
        File svnRepository = null;
        ISVNAuthenticationProvider svnAuthProvider = null;
        FilePath workspaceTargetDir = getMasterWorkspaceDir(build);//always on master
        FilePath vcsDirParam = null; //folder to be used as param on vcs directory search
        FilePath vcsTargetDir = null; //directory on master workspace containing a copy of vcsDir (.git or .svn)

        if (this.vcsDir != null && !"".equals(vcsDir.trim())) {
            vcsDirParam = new FilePath(workspace, this.vcsDir);
        } else {
            vcsDirParam = workspace;
        }

        if (findVCSDir(vcsDirParam, GIT_DIR)) {
            isGit = true;
            // workspace can be on slave so copy resources to master
            vcsTargetDir = new FilePath(new File(workspaceTargetDir.getRemote() + "/.git"));
            vcsDirFound.copyRecursiveTo("**/*", vcsTargetDir);
            gitRepository = repository(workspaceTargetDir.getRemote() + "/.git");
        } else if (findVCSDir(vcsDirParam, SVN_DIR)) {
            isSvn = true;

            SubversionSCM scm = null;
            try {
                Collection<? extends SCM> scMs = SCMTriggerItem.SCMTriggerItems.asSCMTriggerItem(projectAction.getProject()).getSCMs();
                scm = (SubversionSCM) scMs.iterator().next();
                svnAuthProvider = scm.createAuthenticationProvider(build.getParent(), scm.getLocations()[0]);

            } catch (NoSuchMethodError e) {
                if (scm != null) {
                    svnAuthProvider = scm.getDescriptor().createAuthenticationProvider();
                }

            } catch (Exception ex) {
            }

            vcsTargetDir = new FilePath(new File(workspaceTargetDir.getRemote() + "/.svn"));
            vcsDirFound.copyRecursiveTo("**/*", vcsTargetDir);
            svnRepository = new File(workspaceTargetDir.getRemote());

        }

        if (!isGit && !isSvn) {
            throw new RuntimeException(String.format("Git or Svn directories not found in workspace %s.", vcsDirParam.toURI().toString()));
        }


        boolean hasTargetRevision = false;
        String targetRevision = null;
        String targetBuild = null;

        final EnvVars env = build.getEnvironment(listener);
        if (specificRevision != null && !"".equals(specificRevision)) {
            targetRevision = env.expand(specificRevision);
        }

        boolean hasSpecificRevision = targetRevision != null && !"".equals(targetRevision.trim());
        //only look into builds revision if no specific revision is provided (specificRevision has higher priority over build revision)
        if (!hasSpecificRevision && (specificBuild != null && !"".equals(specificBuild))) {
            targetBuild = env.expand(specificBuild);
            targetRevision = findBuildRevision(targetBuild, projectAction.getProject().getBuilds());
            hasSpecificRevision = targetRevision != null && !"".equals(targetRevision.trim());
        }


        listener.getLogger().println("Publishing build last changes...");

        //only look at 'since' parameter when specific revision is NOT set
        if (since != null && !hasSpecificRevision) {

            switch (since) {

                case LAST_SUCCESSFUL_BUILD: {
                    boolean hasSuccessfulBuild = projectAction.getProject().getLastSuccessfulBuild() != null;
                    if (hasSuccessfulBuild) {
                        LastChangesBuildAction action = projectAction.getProject().getLastSuccessfulBuild().getAction(LastChangesBuildAction.class);
                        if (action != null && action.getBuildChanges().getCurrentRevision() != null) {
                            targetRevision = action.getBuildChanges().getCurrentRevision().getCommitId();
                        }
                    } else {
                        listener.error("No successful build found, last changes will use previous revision.");
                    }
                    break;
                }

                case LAST_TAG: {

                    try {
                        if (isGit) {
                            ObjectId lastTagRevision = GitLastChanges.getInstance().getLastTagRevision(gitRepository);
                            if (lastTagRevision != null) {
                                targetRevision = lastTagRevision.name();
                            }
                        } else if (isSvn) {
                            SVNRevision lastTagRevision = getSvnLastChanges(svnAuthProvider).getLastTagRevision(svnRepository);
                            if (lastTagRevision != null) {
                                targetRevision = lastTagRevision.toString();
                            }
                        }
                    } catch (Exception e) {
                        listener.error("Could not resolve last tag revision, last changes will use previous revision.");
                    }
                }
                break;
            }

        }

        hasTargetRevision = targetRevision != null && !"".equals(targetRevision);

        try {
            LastChanges lastChanges = null;
            if (isGit) {
                if (hasTargetRevision) {
                    //compares current repository revision with provided revision
                    lastChanges = GitLastChanges.getInstance().changesOf(gitRepository, GitLastChanges.getInstance().resolveCurrentRevision(gitRepository), gitRepository.resolve(targetRevision));
                } else {
                    //compares current repository revision with previous one
                    lastChanges = GitLastChanges.getInstance().changesOf(gitRepository);
                }
            } else if (isSvn) {
                SvnLastChanges svnLastChanges = getSvnLastChanges(svnAuthProvider);
                if (hasTargetRevision) {
                    //compares current repository revision with provided revision
                    Long svnRevision = Long.parseLong(targetRevision);
                    lastChanges = svnLastChanges.changesOf(svnRepository, SVNRevision.HEAD, SVNRevision.create(svnRevision));
                } else {
                    //compares current repository revision with previous one
                    lastChanges = svnLastChanges.changesOf(svnRepository);
                }
            }

            String resultMessage = String.format("Last changes from revision %s to %s published successfully!", truncate(lastChanges.getCurrentRevision().getCommitId(), 8), truncate(lastChanges.getPreviousRevision().getCommitId(), 8));
            listener.hyperlink("../" + build.getNumber() + "/" + LastChangesBaseAction.BASE_URL, resultMessage);
            listener.getLogger().println("");

            build.addAction(new LastChangesBuildAction(build, lastChanges,
                    new LastChangesConfig(since, specificRevision, format, matching, showFiles, synchronisedScroll, matchWordsThreshold, matchingMaxComparisons)));
        } catch (Exception e) {
            listener.error("Last Changes NOT published due to the following error: " + (e.getMessage() == null ? e.toString() : e.getMessage()) + (e.getCause() != null ? " - " + e.getCause() : ""));
            e.printStackTrace();
        } finally {
            if (vcsTargetDir != null && vcsTargetDir.exists()) {
                vcsTargetDir.deleteRecursive();//delete copied dir on master
            }
        }
        // always success (only warn when no diff was generated)

        build.setResult(Result.SUCCESS);

    }

    private static String findBuildRevision(String targetBuild, RunList<?> builds) {

        if (builds == null || builds.isEmpty()) {
            return null;
        }

        Integer buildParam = null;
        try {
            buildParam = Integer.parseInt(targetBuild);
        } catch (NumberFormatException ne) {

        }
        if (buildParam == null) {
            throw new RuntimeException(String.format("%s is an invalid build number for 'specificBuild' param. It must resolve to an integer.", targetBuild));
        }
        LastChangesBuildAction actionFound = null;
        for (Run build : builds) {
            if (build.getNumber() == buildParam) {
                actionFound = build.getAction(LastChangesBuildAction.class);
                break;
            }
        }

        if (actionFound == null) {
            throw new RuntimeException(String.format("No build found with number %s. Maybe the build was discarded or not has published LastChanges.", buildParam));
        }

        return actionFound.getBuildChanges().getCurrentRevision().getCommitId();


    }

    private boolean isSlave() {
        return Computer.currentComputer() instanceof SlaveComputer;
    }

    private SvnLastChanges getSvnLastChanges(ISVNAuthenticationProvider svnAuthProvider) {
        return svnAuthProvider != null ? SvnLastChanges.getInstance(svnAuthProvider) : SvnLastChanges.getInstance();
    }

    private String truncate(String value, int length) {
        if (value == null || value.length() <= length) {
            return value;
        }

        return value.substring(0, length - 1);
    }

    /**
     * .git directory can be on a workspace sub dir, see JENKINS-36971
     *
     * @return boolean indicating weather the vcs directory was found or not
     */
    private boolean findVCSDir(FilePath workspace, String dir) throws IOException, InterruptedException {
        FilePath vcsDir = null;
        if (workspace.child(dir).exists()) {
            vcsDirFound = workspace.child(dir);
            return true;
        }
        int recursionDepth = RECURSION_DEPTH;
        while ((vcsDir = findVCSDirInSubDirectories(workspace, dir)) == null && recursionDepth > 0) {
            recursionDepth--;
        }
        if (vcsDir == null) {
            return false;
        } else {
            vcsDirFound = vcsDir; //vcs directory  gitDir;
            return true;
        }
    }

    private FilePath findVCSDirInSubDirectories(FilePath sourceDir, String dir) throws IOException, InterruptedException {
        List<FilePath> filePaths = sourceDir.listDirectories();
        if(filePaths == null || filePaths.isEmpty()) {
            return null;
        }

        for (FilePath filePath : sourceDir.listDirectories()) {
            if (filePath.getName().equalsIgnoreCase(dir)) {
                return filePath;
            } else {
                return findVCSDirInSubDirectories(filePath, dir);
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

        private List<Run<?, ?>> builds;

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

        @Restricted(NoExternalUse.class) // Only for UI calls
        public ListBoxModel doFillSinceItems() {
            ListBoxModel items = new ListBoxModel();
            for (SinceType sinceType : SinceType.values()) {
                items.add(sinceType.getName(), sinceType.name());
            }
            return items;
        }

        public FormValidation doCheckSpecificBuild(@QueryParameter String specificBuild, @AncestorInPath AbstractProject project) {
            if (specificBuild == null || "".equals(specificBuild.trim())) {
                return FormValidation.ok();
            }
            boolean isOk = false;
            try {
                if (project.isParameterized() && specificBuild.contains("$")) {
                    return FormValidation.ok();//skip validation for parametrized build number as we don't have the parameter value
                }
                Integer.parseInt(specificBuild);
                findBuildRevision(specificBuild, project.getBuilds());
                isOk = true;
            } catch (NumberFormatException e) {
                return FormValidation.error("Build number is invalid, it must resolve to an Integer.");
            } catch (Exception e) {

            }

            if (isOk) {
                return FormValidation.ok();
            } else {
                return FormValidation.error(String.format("Build #%s is invalid or does not exists anymore or not has published LastChanges.", specificBuild));
            }
        }


    }

    public SinceType getSince() {
        return since;
    }

    public String getSpecificRevision() {
        return specificRevision;
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

    public String getVcsDir() {
        return vcsDir;
    }

    public String getSpecificBuild() {
        return specificBuild;
    }

    @DataBoundSetter
    public void setSince(SinceType since) {
        this.since = since;
    }

    @DataBoundSetter
    public void setSpecificRevision(String specificRevision) {
        this.specificRevision = specificRevision;
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

    @DataBoundSetter
    public void setVcsDir(String vcsDir) {
        this.vcsDir = vcsDir;
    }

    @DataBoundSetter
    public void setSpecificBuild(String buildNumber) {
        this.specificBuild = buildNumber;
    }


}
