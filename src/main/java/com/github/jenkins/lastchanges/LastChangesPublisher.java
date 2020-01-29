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

import com.github.jenkins.lastchanges.exception.CommitInfoException;
import com.github.jenkins.lastchanges.exception.LastChangesException;
import com.github.jenkins.lastchanges.exception.RepositoryNotFoundException;
import com.github.jenkins.lastchanges.impl.GitLastChanges;
import com.github.jenkins.lastchanges.impl.SvnLastChanges;
import com.github.jenkins.lastchanges.model.*;
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
import jenkins.MasterToSlaveFileCallable;
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
import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.github.jenkins.lastchanges.impl.GitLastChanges.repository;
import java.io.Serializable;

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

        FilePath vcsDirParam = null; //folder to be used as param on vcs directory search

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
                        //The callable will obtain the target revision selected by the user in the corresponding node (master or slave)
                        if (isGit) {
                            String lastTagRevision = vcsDirFound.act(new GetGitLastTagRevisionCallable(listener));
                            if (lastTagRevision != null) {
                                targetRevision = lastTagRevision;
                            }
                        } else if (isSvn) {
                            String lastTagRevision = vcsDirParam.act(new GetSvnLastTagRevisionCallable(listener, svnAuthProvider));
                            if (lastTagRevision != null) {
                                targetRevision = lastTagRevision;
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
            //The callable will obtain the last changes between revisions in the corresponding node (master or slave)
            if (isGit) {
                lastChanges = vcsDirFound.act(new GetGITLastChangesCallable(hasTargetRevision, targetRevision, listener));
            } else if (isSvn) {
                lastChanges = vcsDirParam.act(new GetSVNLastChangesCallable(hasTargetRevision, targetRevision, listener, svnAuthProvider));
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

    private static List<CommitChanges> obtainCommitChangesFromGit(final Repository gitRepository, final List<CommitInfo> commitInfoList) {
        if (commitInfoList == null || commitInfoList.isEmpty()) {
            return null;
        }
        List<CommitChanges> commitChanges = new ArrayList<>();

        try {
            Collections.sort(commitInfoList, new CommitsByDateComparator());

            for (int i = commitInfoList.size() - 1; i >= 0; i--) {
                ObjectId previousRevision = gitRepository.resolve(commitInfoList.get(i).getCommitId() + "^1");
                ObjectId currentRevision = gitRepository.resolve(commitInfoList.get(i).getCommitId());
                LastChanges lastChanges = GitLastChanges.getInstance().changesOf(gitRepository, currentRevision, previousRevision);

                String diff;
                if (lastChanges != null) {
                    diff = lastChanges.getDiff();
                } else {
                    diff = "";
                }
                commitChanges.add(new CommitChanges(commitInfoList.get(i), diff));
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Could not get commit changes from Git.", e);
        }

        return commitChanges;
    }

    private static List<CommitChanges> obtainCommitChangesFromSvn(final File svnRepository, final List<CommitInfo> commitInfoList, final String oldestCommit, final ISVNAuthenticationProvider svnAuthProvider) {
        if (commitInfoList == null || commitInfoList.isEmpty()) {
            return null;
        }
        List<CommitChanges> commitChanges = new ArrayList<>();

        try {
            Collections.sort(commitInfoList, new CommitsByDateComparator());

            for (int i = commitInfoList.size() - 1; i >= 0; i--) {
                LastChanges lastChanges;
                SVNRevision previousRevision;
                SVNRevision currentRevision;

                if (i == 0) { //here we can't compare with (i -1) so we compare with first commit of oldest commit (retrieved in main diff)
                    //here we have the older commit from current tree (see LastChanges.java) which diff must be compared with oldestCommit which is currentRevision from previous tree
                    previousRevision = SVNRevision.parse(oldestCommit);
                } else {
                    //get changes comparing current commit (i) with previous one (i -1)
                    previousRevision = SVNRevision.parse(commitInfoList.get(i - 1).getCommitId());
                }
                currentRevision = SVNRevision.parse(commitInfoList.get(i).getCommitId());
                lastChanges = SvnLastChanges.getInstance(svnAuthProvider).changesOf(svnRepository, currentRevision, previousRevision);

                String diff;
                if (lastChanges != null) {
                    diff = lastChanges.getDiff();
                } else {
                    diff = "";
                }
                commitChanges.add(new CommitChanges(commitInfoList.get(i), diff));
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Could not get commit changes from SVN.", e);
        }

        return commitChanges;
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

    private static SvnLastChanges getSvnLastChanges(ISVNAuthenticationProvider svnAuthProvider) {
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

    private static final class GetGitLastTagRevisionCallable extends MasterToSlaveFileCallable <String> {

        private final TaskListener listener;

        public GetGitLastTagRevisionCallable(final TaskListener listener) {
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
                String lastTagRevisionErrorMsg = "Last Changes Plugin: Could not find the workspace directory for GIT in order to obtain the last changes of the revisions: " + workspace.getAbsolutePath();
                listener.error(lastTagRevisionErrorMsg);
                throw new RepositoryNotFoundException(lastTagRevisionErrorMsg);
            }
        }
    }

    private static final class GetSvnLastTagRevisionCallable extends MasterToSlaveFileCallable <String> {

        private final TaskListener listener;

        private final ISVNAuthenticationProvider svnAuthProvider;

        public GetSvnLastTagRevisionCallable(final TaskListener listener, final ISVNAuthenticationProvider svnAuthProvider) {
            this.listener = listener;
            this.svnAuthProvider = svnAuthProvider;
        }

        @Override
        public String invoke(File workspace, VirtualChannel virtualChannel) throws RepositoryNotFoundException {
            if (workspace.exists() && workspace.isDirectory()) {
                File svnRepository = new File(workspace.getAbsolutePath());
                SVNRevision lastTagRevision = getSvnLastChanges(svnAuthProvider).getLastTagRevision(svnRepository);
                if (lastTagRevision != null) {
                    return lastTagRevision.toString();
                } else {
                    return null;
                }
            } else {
                String lasTagRevisionErrorMsg = "Last Changes Plugin: Could not find the workspace directory for SVN in order to obtain the last changes of the revisions: " + workspace.getAbsolutePath();
                listener.error(lasTagRevisionErrorMsg);
                throw new RepositoryNotFoundException(lasTagRevisionErrorMsg);
            }
        }
    }

    private static final class GetGITLastChangesCallable extends MasterToSlaveFileCallable <LastChanges> {

        private final boolean hasTargetRevision;

        private final String targetRevision;

        private final TaskListener listener;

        public GetGITLastChangesCallable(final boolean hasTargetRevision, final String targetRevision, final TaskListener listener) {
            this.hasTargetRevision = hasTargetRevision;
            this.targetRevision = targetRevision;
            this.listener = listener;
        }

        @Override
        public LastChanges invoke(final File workspace, VirtualChannel channel) {
            if(workspace.exists() && workspace.isDirectory()) {
                return getGITLastChanges(workspace);
            } else {
                String lastChangesWorkDirErrorMsg = "Last Changes Plugin: Could not find the workspace directory in order to obtain the last changes of the revisions: " + workspace.getAbsolutePath();
                listener.error(lastChangesWorkDirErrorMsg);
                throw new RepositoryNotFoundException(lastChangesWorkDirErrorMsg);
            }
        }

        private LastChanges getGITLastChanges(final File workspace) {
            LastChanges lastChanges;
            try {
                Repository gitRepository = repository(workspace.getAbsolutePath());
                if (hasTargetRevision) {
                    //compares current repository revision with provided revision
                    ObjectId previousRevision = gitRepository.resolve(targetRevision);
                    ObjectId currentRevision = GitLastChanges.getInstance().resolveCurrentRevision(gitRepository);
                    lastChanges = GitLastChanges.getInstance().changesOf(gitRepository, currentRevision, previousRevision);

                    currentRevision = gitRepository.resolve(lastChanges.getCurrentRevision().getCommitId());
                    List<CommitInfo> commitInfoList = GitLastChanges.getInstance().getCommitsBetweenRevisions(gitRepository, currentRevision, previousRevision);
                    lastChanges.addCommits(LastChangesPublisher.obtainCommitChangesFromGit(gitRepository, commitInfoList));
                } else {
                    //compares current repository revision with previous one
                    lastChanges = GitLastChanges.getInstance().changesOf(gitRepository);
                    lastChanges.addCommit(new CommitChanges(lastChanges.getCurrentRevision(), lastChanges.getDiff()));
                }
                return lastChanges;
            } catch (IOException e) {
                String lastChangesErrorMsg = "Last Changes Plugin: Last changes between revisions from GIT workspace were not obtained";
                listener.error(lastChangesErrorMsg);
                throw new LastChangesException(lastChangesErrorMsg, e);
            }
        }
    }

    private static final class GetSVNLastChangesCallable extends MasterToSlaveFileCallable <LastChanges> {

        private final boolean hasTargetRevision;

        private final String targetRevision;

        private final TaskListener listener;

        private final ISVNAuthenticationProvider svnAuthProvider;

        public GetSVNLastChangesCallable(final boolean hasTargetRevision, final String targetRevision, final TaskListener listener, final ISVNAuthenticationProvider svnAuthProvider) {
            this.hasTargetRevision = hasTargetRevision;
            this.targetRevision = targetRevision;
            this.listener = listener;
            this.svnAuthProvider = svnAuthProvider;
        }

        @Override
        public LastChanges invoke(final File workspace, VirtualChannel channel) {
            if(workspace.exists() && workspace.isDirectory()) {
                    return getSVNLastChanges(workspace);
            } else {
                String lastChangesWorkDirErrorMsg = "Last Changes Plugin: Could not find the SVN workspace directory in order to obtain the last changes of the revisions: " + workspace.getAbsolutePath();
                listener.error(lastChangesWorkDirErrorMsg);
                throw new RepositoryNotFoundException(lastChangesWorkDirErrorMsg);
            }
        }

        private LastChanges getSVNLastChanges(final File workspace) {
            LastChanges lastChanges;
            try {
                SvnLastChanges svnLastChanges = getSvnLastChanges(svnAuthProvider);
                File svnRepository = new File(workspace.getAbsolutePath());
                if(hasTargetRevision) {
                    //compares current repository revision with provided revision
                    final Long svnRevision = Long.parseLong(targetRevision);
                    SVNRevision previousRevision = SVNRevision.create(svnRevision);
                    SVNRevision currentRevision = SVNRevision.HEAD;
                    lastChanges = svnLastChanges.changesOf(svnRepository, currentRevision, previousRevision);

                    currentRevision = SVNRevision.create(Long.parseLong(lastChanges.getCurrentRevision().getCommitId()));
                    List<CommitInfo> commitInfoList = SvnLastChanges.getInstance(svnAuthProvider).getCommitsBetweenRevisions(svnRepository, currentRevision, previousRevision);
                    String oldestCommit = lastChanges.getPreviousRevision().getCommitId();
                    lastChanges.addCommits(obtainCommitChangesFromSvn(svnRepository, commitInfoList, oldestCommit, svnAuthProvider));
                } else {
                    //compares current repository revision with previous one
                    lastChanges = svnLastChanges.changesOf(svnRepository);
                    //in this case there will be only one commit
                    lastChanges.addCommit(new CommitChanges(lastChanges.getCurrentRevision(), lastChanges.getDiff()));
                }
                return lastChanges;
            } catch (LastChangesException e) {
                String lastChangesErrorMsg = "Last Changes Plugin: Last changes between revisions from SVN workspace were not obtained";
                listener.error(lastChangesErrorMsg);
                throw new LastChangesException(lastChangesErrorMsg, e);
            }
        }
    }

    private static final class CommitsByDateComparator implements Comparator<CommitInfo> {
        @Override
        public int compare(CommitInfo c1, CommitInfo c2) {
            try {
                DateFormat format = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT);
                return format.parse(c1.getCommitDate()).compareTo(format.parse(c2.getCommitDate()));
            } catch (ParseException e) {
                String couldNotParseCommitDatesErrorMsg = String.format("Could not parse commit dates %s and %s ", c1.getCommitDate(), c2.getCommitDate());
                LOG.severe(couldNotParseCommitDatesErrorMsg);
                throw new CommitInfoException(couldNotParseCommitDatesErrorMsg, e);
            }
        }
    }
}