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
import com.github.jenkins.lastchanges.exception.RepositoryNotFoundException;
import com.github.jenkins.lastchanges.impl.GitLastChanges;
import com.github.jenkins.lastchanges.impl.SvnLastChanges;
import com.github.jenkins.lastchanges.model.*;
import com.github.jenkins.lastchanges.LastChangesUtil;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.remoting.VirtualChannel;
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
import org.jenkinsci.remoting.RoleChecker;
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
import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import static com.github.jenkins.lastchanges.impl.GitLastChanges.repository;
import java.io.Serializable;
import java.nio.file.Files;


/**
 * @author rmpestano
 */
public class LastChangesPublisher extends Recorder implements SimpleBuildStep, Serializable {

    private static Logger LOG = Logger.getLogger(LastChangesPublisher.class.getName());

    private static final String GIT_DIR = ".git";

    private static final String SVN_DIR = ".svn";

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

    private File svnRepository = null;

    private boolean isGit = false;

    private boolean isSvn = false;

    private transient LastChanges lastChanges = null;

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

        ISVNAuthenticationProvider svnAuthProvider = null;

        //TODO: Delete when svn version is fixed
        FilePath workspaceTargetDir = getMasterWorkspaceDir(build);//always on master

        FilePath vcsDirParam = null; //folder to be used as param on vcs directory search

        //TODO: Delete when svn version is fixed
        FilePath vcsTargetDir = null; //directory on master workspace containing a copy of vcsDir (.git or .svn)

        //TODO: Delete when svn version is fixed
        Boolean copiedVcsDir = false; // if we copied vcs data to workspaceTargetDir

        if (this.vcsDir != null && !"".equals(vcsDir.trim())) {
            vcsDirParam = new FilePath(workspace, this.vcsDir);
        } else {
            vcsDirParam = workspace;
        }

        if (findVCSDir(vcsDirParam, GIT_DIR)) {
            isGit = true;

        } else if (findVCSDir(vcsDirParam, SVN_DIR)) {
            isSvn = true;

            SubversionSCM scm = null;
            try {
                Collection<? extends SCM> scMs = SCMTriggerItem.SCMTriggerItems.asSCMTriggerItem(build.getParent()).getSCMs();
                scm = (SubversionSCM) scMs.iterator().next();
                svnAuthProvider = scm.createAuthenticationProvider(build.getParent(), scm.getLocations()[0]);
            } catch (NoSuchMethodError e) {
                if (scm != null) {
                    svnAuthProvider = scm.getDescriptor().createAuthenticationProvider();
                }

            } catch (Exception ex) {
                LOG.log(Level.WARNING,"Problem creating svn auth provider",ex);
            }

            vcsTargetDir = new FilePath(new File(workspaceTargetDir.getRemote() + "/.svn"));
            File remoteSvnDir = new File(workspaceTargetDir.getRemote() + "/.svn");
            //copy only if directory doesn't exists
            if (!remoteSvnDir.exists() || !Files.newDirectoryStream(remoteSvnDir.toPath()).iterator().hasNext()) {
                vcsDirFound.copyRecursiveTo("**/*", vcsTargetDir);
                copiedVcsDir = true;
            }
            
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
            targetRevision = findBuildRevision(targetBuild, build.getParent().getBuilds());
            hasSpecificRevision = targetRevision != null && !"".equals(targetRevision.trim());
        }

        listener.getLogger().println("Publishing build last changes...");

        //only look at 'since' parameter when specific revision is NOT set
        if (since != null && !hasSpecificRevision) {

            switch (since) {

                case LAST_SUCCESSFUL_BUILD: {
                    boolean hasSuccessfulBuild = build.getParent().getLastSuccessfulBuild() != null;
                    if (hasSuccessfulBuild) {
                        LastChangesBuildAction action = build.getParent().getLastSuccessfulBuild().getAction(LastChangesBuildAction.class);
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
                            String lastTagRevision = vcsDirFound.act(new getTargetRevisionCallableAction(isGit, listener));
                            if (lastTagRevision != null) {
                                targetRevision = lastTagRevision;
                            }

                        //TODO: Implement case inside the Callable method for SVN version.
                        } else if (isSvn) {
                            SVNRevision lastTagRevision = getSvnLastChanges(svnAuthProvider).getLastTagRevision(svnRepository);
                            if (lastTagRevision != null) {
                                targetRevision = lastTagRevision.toString();
                            }
                        }
                    } catch (Exception e) {
                        LOG.log(Level.WARNING, "Could not resolve last tag revision, last changes will use previous revision.", e);
                        listener.error("Could not resolve last tag revision, last changes will use previous revision.");
                    }
                }
                break;
            }

        }

        hasTargetRevision = targetRevision != null && !"".equals(targetRevision);

        try {
            if (isGit) {
                lastChanges = vcsDirFound.act(new gitCallableAction(hasTargetRevision, targetRevision, listener));

                //TODO: Apply changes FOR SVN so the code runs on the workspace selected (master or slave) as it is done for git.
            } else if (isSvn) {
                SvnLastChanges svnLastChanges = getSvnLastChanges(svnAuthProvider);
                if (hasTargetRevision) {
                    //compares current repository revision with provided revision
                    Long svnRevision = Long.parseLong(targetRevision);
                    lastChanges = svnLastChanges.changesOf(svnRepository, SVNRevision.HEAD, SVNRevision.create(svnRevision));
                    List<CommitInfo> commitInfoList = LastChangesUtil.getCommitsBetweenRevisions(null, false, lastChanges.getCurrentRevision().getCommitId(), targetRevision, svnRepository, svnAuthProvider);
                    lastChanges.addCommits(LastChangesUtil.commitChanges(null, false, commitInfoList, lastChanges.getPreviousRevision().getCommitId(), svnRepository, svnAuthProvider));
                } else {
                    //compares current repository revision with previous one
                    lastChanges = svnLastChanges.changesOf(svnRepository);
                    //in this case there will be only one commit
                    lastChanges.addCommit(new CommitChanges(lastChanges.getCurrentRevision(), lastChanges.getDiff()));
                }
            }

            String resultMessage = String.format("Last changes from revision %s (current) to %s (previous) published successfully!", truncate(lastChanges.getCurrentRevision().getCommitId(), 8), truncate(lastChanges.getPreviousRevision().getCommitId(), 8));
            listener.hyperlink("../" + build.getNumber() + "/" + LastChangesBaseAction.BASE_URL, resultMessage);
            listener.getLogger().println("");
            build.addAction(new LastChangesBuildAction(build, lastChanges,
                    new LastChangesConfig(since, specificRevision, format, matching, showFiles, synchronisedScroll, matchWordsThreshold, matchingMaxComparisons)));
        } catch (Exception e) {
            listener.error("Last Changes NOT published due to the following error: " + (e.getMessage() == null ? e.toString() : e.getMessage()) + (e.getCause() != null ? " - " + e.getCause() : ""));
            LOG.log(Level.SEVERE, "Could not publish LastChanges.", e);
        }
        // always success (only warn when no diff was generated)

        build.setResult(Result.SUCCESS);
    }

    /**
     * @return gets the LastChanges from current publisher
     */
    public LastChanges getLastChanges() {
        return lastChanges;
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
        if (filePaths == null || filePaths.isEmpty()) {
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
            return "Publish Last Changes_Test";
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

    /**
     * The callable will obtain the target revision selected by the user in case that it is required. It will
     * look for it in the corresponding node that is being executed (Master or Slave)
     */
    private static final class getTargetRevisionCallableAction implements FilePath.FileCallable <String> {

        boolean isGitVar;

        TaskListener listener;

        getTargetRevisionCallableAction(boolean isGit, TaskListener listener) {
            this.isGitVar = isGit;
            this.listener = listener;
        }

        @Override
        public String invoke(File workspace, VirtualChannel virtualChannel) throws RepositoryNotFoundException {

            if (workspace.exists() && workspace.isDirectory()) {
                Repository gitRepository = repository(workspace.getAbsolutePath());
                ObjectId lastTagRevision = GitLastChanges.getInstance().getLastTagRevision(gitRepository);
                if (lastTagRevision != null) {
                    return lastTagRevision.name();
                } else {
                    return null;
                }
            } else {
                String lasTagRevisionErrorMsg = "Could not find workspace directory when trying the last tag revision";
                LOG.log(Level.WARNING, lasTagRevisionErrorMsg);
                listener.error(lasTagRevisionErrorMsg);
                throw new RepositoryNotFoundException(lasTagRevisionErrorMsg);
            }
        }

        @Override
        public void checkRoles(RoleChecker roleChecker) throws SecurityException {
            //Nothing to do
        }
    }

    /**
     * The callable will identify if it is being run by the Master node or by the slave node
     * and thus it will execute the calculation on the corresponding node.
     */
    private static final class gitCallableAction implements FilePath.FileCallable <LastChanges> {

        boolean isGit = true;

        boolean hasTargetRevisionCallable;

        String targetRevisionCallable = null;

        TaskListener listener;

        gitCallableAction(boolean hasTargetRevision, String targetRevision, TaskListener listener) {
            this.hasTargetRevisionCallable = hasTargetRevision;
            this.targetRevisionCallable = targetRevision;
            this.listener = listener;
        }

        @Override
        public LastChanges invoke(File workspace, VirtualChannel channel)  {

            LastChanges lastChanges = null;
            try {
                if(workspace.exists() && workspace.isDirectory()) {
                    Repository gitRepository = repository(workspace.getAbsolutePath());
                    if (hasTargetRevisionCallable) {
                        //compares current repository revision with provided revision
                        lastChanges = GitLastChanges.getInstance().changesOf(gitRepository, GitLastChanges.getInstance().resolveCurrentRevision(gitRepository), gitRepository.resolve(targetRevisionCallable));
                        List<CommitInfo> commitInfoList = LastChangesUtil.getCommitsBetweenRevisions(gitRepository, isGit, lastChanges.getCurrentRevision().getCommitId(), targetRevisionCallable, null,null);
                        lastChanges.addCommits(LastChangesUtil.commitChanges(gitRepository, isGit, commitInfoList, lastChanges.getPreviousRevision().getCommitId(), null, null));

                    } else {
                        //compares current repository revision with previous one
                        lastChanges = GitLastChanges.getInstance().changesOf(gitRepository);
                        lastChanges.addCommit(new CommitChanges(lastChanges.getCurrentRevision(), lastChanges.getDiff()));
                    }
                    return lastChanges;
                } else {
                    String lastChangesWorkDirErrorMsg = "Could not find workspace directory when tried to get last changes of the revision";
                    LOG.log(Level.WARNING, lastChangesWorkDirErrorMsg);
                    listener.error(lastChangesWorkDirErrorMsg);
                    throw new RepositoryNotFoundException(lastChangesWorkDirErrorMsg);
                }
            } catch (IOException e) {
                String lastChangesErrorMsg = "Could not get last changes of the revisions";
                LOG.log(Level.SEVERE, lastChangesErrorMsg, e);
                listener.error(lastChangesErrorMsg);
                throw new LastChangesException(lastChangesErrorMsg);
            }
        }

        @Override
        public void checkRoles(RoleChecker roleChecker) throws SecurityException {
            //Nothing to do
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
