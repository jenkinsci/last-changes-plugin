package com.github.jenkins.lastchanges;

import com.github.jenkins.lastchanges.model.CommitChanges;
import com.github.jenkins.lastchanges.model.CommitInfo;
import com.github.jenkins.lastchanges.model.FormatType;
import com.github.jenkins.lastchanges.model.LastChanges;
import com.github.jenkins.lastchanges.model.MatchingType;
import com.github.jenkins.lastchanges.model.SinceType;
import hudson.model.Actionable;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.UserRemoteConfig;
import hudson.plugins.git.extensions.impl.DisableRemotePoll;
import hudson.scm.SubversionSCM.ModuleLocation;
import hudson.slaves.DumbSlave;
import jenkins.plugins.git.GitSampleRepoRule;
import jenkins.plugins.git.junit.jupiter.WithGitSampleRepo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import static org.assertj.core.api.Assertions.assertThat;

@WithJenkins
@WithGitSampleRepo
class LastChangesTest {

    private static final String newLine = System.lineSeparator();

    private JenkinsRule jenkins;
    private static GitSampleRepoRule sampleRepo;

    private final File sampleRepoDir = new File(LastChangesTest.class.getResource("/git-sample-repo").getFile());

    private static final Logger log = Logger.getLogger(Actionable.class.getName()); // matches the logger in the affected class
    private static OutputStream logCapturingStream;
    private static StreamHandler customLogHandler;
    private static final Random random = new Random();
    private static final String INITIAL_COMMIT_MESSAGE = "initial-commit-" + random.nextInt(10000);
    private static String sampleRepoHead = null;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        jenkins = rule;
        logCapturingStream = new ByteArrayOutputStream();
        Handler[] handlers = log.getParent().getHandlers();
        customLogHandler = new StreamHandler(logCapturingStream, handlers[0].getFormatter());
        log.addHandler(customLogHandler);
        // Local checkout is disallowed by default for security.
        // It is safe to enable local checkout for tests
        GitSCM.ALLOW_LOCAL_CHECKOUT = true;
    }

    @BeforeAll
    static void setUp(GitSampleRepoRule repo) throws Exception {
        sampleRepo = repo;
        sampleRepo.init();
        sampleRepo.write("file", INITIAL_COMMIT_MESSAGE);
        sampleRepo.git("commit", "--all", "--message=" + INITIAL_COMMIT_MESSAGE);
        sampleRepoHead = sampleRepo.head();
    }

    @AfterEach
    void tearDown() throws IOException {
        logCapturingStream.close();
        GitSCM.ALLOW_LOCAL_CHECKOUT = false;
    }

    private String getTestCapturedLog() {
        customLogHandler.flush();
        return logCapturingStream.toString();
    }

    @Test
    void shouldGetLastChangesOfGitRepository() throws Exception {

        // given
        List<UserRemoteConfig> remoteConfigs = new ArrayList<>();
        remoteConfigs.add(new UserRemoteConfig(sampleRepoDir.getAbsolutePath(), "origin", "", null));
        List<BranchSpec> branches = new ArrayList<>();
        branches.add(new BranchSpec("master"));
        GitSCM scm = new GitSCM(remoteConfigs, branches, false,
                Collections.emptyList(), null, null,
                Collections.singletonList(new DisableRemotePoll()));
        FreeStyleProject project = jenkins.createFreeStyleProject("git-test");
        project.setScm(scm);
        LastChangesPublisher publisher = new LastChangesPublisher(SinceType.PREVIOUS_REVISION, FormatType.LINE, MatchingType.NONE, true, false, "0.50", "1500", null, null, null);
        project.getPublishersList().add(publisher);
        project.save();

        // when
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

        // then
        LastChangesBuildAction action = build.getAction(LastChangesBuildAction.class);
        assertThat(action).isNotNull();
        LastChanges lastChanges = action.getBuildChanges();
        assertThat(lastChanges).isNotNull();
        assertThat(lastChanges.getCurrentRevision()).isNotNull();
        assertThat(lastChanges.getCurrentRevision().getCommitMessage()).isEqualTo("Added javadoc\n");
        assertThat(lastChanges.getCurrentRevision().getCommitId()).isEqualTo("27ad83a8fbee4b551670a03fc035bf87f7a3bcfb");

        final String diff = ("diff --git a/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt b/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + newLine +
                "index 6d28c9b..bcc2ac0 100644" + newLine +
                "--- a/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + newLine +
                "+++ b/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + newLine +
                "@@ -31,6 +31,12 @@" + newLine +
                " /**" + newLine +
                "  * Creates a container which lists all instances of given entity. To restrict the list to a particular entity only," + newLine +
                "  * simply call [JPAContainer.addContainerFilter] on the container produced." + newLine +
                "+ *" + newLine +
                "+ * Containers produced by this method have the following properties:" + newLine +
                "+ * * The container's [Item] IDs are not the entity instances themselves - instead, [Item] ID contains the value of the JPA entity ID. This is important when using the container" + newLine +
                "+ * together with [AbstractSelect] as the select's value is taken amongst the Item ID." + newLine +
                "+ * * [Item]'s Property IDs are [String] values - the field names of given JPA bean." + newLine +
                "+ *" + newLine +
                "  * @param entity the entity type" + newLine +
                "  * @return the new container which can be assigned to a [Grid]" + newLine +
                "  */" + newLine +
                "@@ -279,9 +285,12 @@" + newLine +
                "  * An utility method which adds an item and sets item's caption." + newLine +
                "  * @param the Identification of the item to be created." + newLine +
                "  * @param caption the new caption" + newLine +
                "+ * @return the newly created item ID." + newLine +
                "  */" + newLine +
                " fun AbstractSelect.addItem(itemId: Any?, caption: String) = addItem(itemId).apply { setItemCaption(itemId, caption) }!!" + newLine +
                " " + newLine +
                "+" + newLine +
                "+" + newLine +
                " /**" + newLine +
                "  * Walks over this component and all descendants of this component, breadth-first." + newLine +
                "  * @return iterable which iteratively walks over this component and all of its descendants.").replaceAll("\r", "");

        assertThat(lastChanges.getDiff()).isEqualToIgnoringWhitespace(diff);

        assertThat(lastChanges.getCommits()).isNotNull()
                .hasSize(1);
        assertThat(lastChanges.getCommits().get(0))
                .isEqualTo(new CommitChanges(new CommitInfo().setCommitId("27ad83a8fbee4b551670a03fc035bf87f7a3bcfb"), null));

        assertThat(lastChanges.getCommits().get(0)
                .getChanges()).isEqualToIgnoringWhitespace(diff);

        jenkins.assertLogContains("Last changes from revision 27ad83a (current) to a511a43 (previous) published successfully!", build);
    }

    @Test
    @Issue("JENKINS-53860")
    void shouldNotProduceWarnWhenJobDoesNotPublishLastChanges() throws Exception {

        // given
        List<UserRemoteConfig> remoteConfigs = new ArrayList<>();
        remoteConfigs.add(new UserRemoteConfig(sampleRepoDir.getAbsolutePath(), "origin", "", null));
        List<BranchSpec> branches = new ArrayList<>();
        branches.add(new BranchSpec("master"));
        GitSCM scm = new GitSCM(remoteConfigs, branches, false,
                Collections.emptyList(), null, null,
                Collections.singletonList(new DisableRemotePoll()));
        FreeStyleProject project = jenkins.createFreeStyleProject("git-test");
        project.setScm(scm);
        project.save();

        // when
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        // then
        assertThat(getTestCapturedLog()).doesNotContain("WARNING\thudson.model.Actionable#createFor: Actions from com.github.jenkins.lastchanges.LastChangesProjectAction$LastChangesActionFactory");
    }

    @Test
    void shouldGetLastChangesUsingVcsDir() throws Exception {

        // given
        List<UserRemoteConfig> remoteConfigs = new ArrayList<>();
        remoteConfigs.add(new UserRemoteConfig(sampleRepo.fileUrl(), "origin", "", null));
        List<BranchSpec> branches = new ArrayList<>();
        branches.add(new BranchSpec("master"));
        GitSCM scm = new GitSCM(remoteConfigs, branches, false,
                Collections.emptyList(), null, null,
                Collections.singletonList(new DisableRemotePoll()));
        FreeStyleProject project = jenkins.createFreeStyleProject("git-test");
        project.setScm(scm);
        LastChangesPublisher publisher = new LastChangesPublisher(SinceType.PREVIOUS_REVISION, FormatType.LINE, MatchingType.NONE, true, false, "0.50", "1500", null, ".", null);
        project.getPublishersList().add(publisher);
        project.save();

        // when
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

        // then
        LastChangesBuildAction action = build.getAction(LastChangesBuildAction.class);
        assertThat(action).isNotNull();
        LastChanges lastChanges = action.getBuildChanges();
        assertThat(lastChanges).isNotNull();
        assertThat(lastChanges.getCurrentRevision()).isNotNull();
        assertThat(lastChanges.getCurrentRevision().getCommitMessage()).isEqualTo(INITIAL_COMMIT_MESSAGE + "\n");
        assertThat(lastChanges.getCurrentRevision().getCommitId()).isEqualTo(sampleRepoHead);
        assertThat(lastChanges.getDiff()).startsWith("diff --git a/file b/file");
        jenkins.assertLogContains("Last changes from revision ", build);
        jenkins.assertLogContains(" (previous) published successfully!", build);
    }

    @Test
    void shouldGetLastChangesOfLastSuccessfulBuild() throws Exception {

        // given
        List<UserRemoteConfig> remoteConfigs = new ArrayList<>();
        remoteConfigs.add(new UserRemoteConfig(sampleRepoDir.getAbsolutePath(), "origin", "", null));
        List<BranchSpec> branches = new ArrayList<>();
        branches.add(new BranchSpec("master"));
        GitSCM scm = new GitSCM(remoteConfigs, branches, false,
                Collections.emptyList(), null, null,
                Collections.singletonList(new DisableRemotePoll()));
        FreeStyleProject project = jenkins.createFreeStyleProject("git-test");
        project.setScm(scm);
        LastChangesPublisher publisher = new LastChangesPublisher(SinceType.LAST_SUCCESSFUL_BUILD, FormatType.LINE, MatchingType.NONE, true, false, "0.50", "1500", null, null, null);
        project.getPublishersList().add(publisher);
        project.save();

        // when
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

        // then
        LastChangesBuildAction action = build.getAction(LastChangesBuildAction.class);
        assertThat(action).isNotNull();
        LastChanges lastChanges = action.getBuildChanges();
        assertThat(lastChanges).isNotNull();
        assertThat(lastChanges.getCurrentRevision()).isNotNull();
        assertThat(lastChanges.getCurrentRevision().getCommitMessage()).isEqualTo("Added javadoc\n");
        assertThat(lastChanges.getCurrentRevision().getCommitId()).isEqualTo("27ad83a8fbee4b551670a03fc035bf87f7a3bcfb");
        assertThat(lastChanges.getDiff()).isEqualToIgnoringWhitespace(("diff --git a/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt b/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + newLine +
                "index 6d28c9b..bcc2ac0 100644" + newLine +
                "--- a/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + newLine +
                "+++ b/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + newLine +
                "@@ -31,6 +31,12 @@" + newLine +
                " /**" + newLine +
                "  * Creates a container which lists all instances of given entity. To restrict the list to a particular entity only," + newLine +
                "  * simply call [JPAContainer.addContainerFilter] on the container produced." + newLine +
                "+ *" + newLine +
                "+ * Containers produced by this method have the following properties:" + newLine +
                "+ * * The container's [Item] IDs are not the entity instances themselves - instead, [Item] ID contains the value of the JPA entity ID. This is important when using the container" + newLine +
                "+ * together with [AbstractSelect] as the select's value is taken amongst the Item ID." + newLine +
                "+ * * [Item]'s Property IDs are [String] values - the field names of given JPA bean." + newLine +
                "+ *" + newLine +
                "  * @param entity the entity type" + newLine +
                "  * @return the new container which can be assigned to a [Grid]" + newLine +
                "  */" + newLine +
                "@@ -279,9 +285,12 @@" + newLine +
                "  * An utility method which adds an item and sets item's caption." + newLine +
                "  * @param the Identification of the item to be created." + newLine +
                "  * @param caption the new caption" + newLine +
                "+ * @return the newly created item ID." + newLine +
                "  */" + newLine +
                " fun AbstractSelect.addItem(itemId: Any?, caption: String) = addItem(itemId).apply { setItemCaption(itemId, caption) }!!" + newLine +
                " " + newLine +
                "+" + newLine +
                "+" + newLine +
                " /**" + newLine +
                "  * Walks over this component and all descendants of this component, breadth-first." + newLine +
                "  * @return iterable which iteratively walks over this component and all of its descendants.").replaceAll("\r", ""));

        jenkins.assertLogContains("Last changes from revision 27ad83a (current) to a511a43 (previous) published successfully!", build);
    }

    @Test
    void shouldGetLastChangesOfSpecificBuild() throws Exception {

        // given
        List<UserRemoteConfig> remoteConfigs = new ArrayList<>();
        remoteConfigs.add(new UserRemoteConfig(sampleRepoDir.getAbsolutePath(), "origin", "", null));
        List<BranchSpec> branches = new ArrayList<>();
        branches.add(new BranchSpec("master"));
        GitSCM scm = new GitSCM(remoteConfigs, branches, false,
                Collections.emptyList(), null, null,
                Collections.singletonList(new DisableRemotePoll()));
        FreeStyleProject project = jenkins.createFreeStyleProject("git-test");
        project.setScm(scm);
        LastChangesPublisher publisher = new LastChangesPublisher(SinceType.PREVIOUS_REVISION, FormatType.LINE, MatchingType.NONE, true, false, "0.50", "1500", null, null, null);
        project.getPublishersList().add(publisher);
        project.save();

        // when
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

        publisher = new LastChangesPublisher(SinceType.PREVIOUS_REVISION, FormatType.LINE, MatchingType.NONE, true, false, "0.50", "1500", null, null, build.getNumber() + "");

        build = jenkins.buildAndAssertSuccess(project);

        // then
        LastChangesBuildAction action = build.getAction(LastChangesBuildAction.class);
        assertThat(action).isNotNull();
        LastChanges lastChanges = action.getBuildChanges();
        assertThat(lastChanges).isNotNull();
        assertThat(lastChanges.getCurrentRevision()).isNotNull();
        assertThat(lastChanges.getCurrentRevision().getCommitMessage()).isEqualTo("Added javadoc\n");
        assertThat(lastChanges.getCurrentRevision().getCommitId()).isEqualTo("27ad83a8fbee4b551670a03fc035bf87f7a3bcfb");
        assertThat(lastChanges.getDiff()).isEqualToIgnoringWhitespace(("diff --git a/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt b/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + newLine +
                "index 6d28c9b..bcc2ac0 100644" + newLine +
                "--- a/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + newLine +
                "+++ b/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + newLine +
                "@@ -31,6 +31,12 @@" + newLine +
                " /**" + newLine +
                "  * Creates a container which lists all instances of given entity. To restrict the list to a particular entity only," + newLine +
                "  * simply call [JPAContainer.addContainerFilter] on the container produced." + newLine +
                "+ *" + newLine +
                "+ * Containers produced by this method have the following properties:" + newLine +
                "+ * * The container's [Item] IDs are not the entity instances themselves - instead, [Item] ID contains the value of the JPA entity ID. This is important when using the container" + newLine +
                "+ * together with [AbstractSelect] as the select's value is taken amongst the Item ID." + newLine +
                "+ * * [Item]'s Property IDs are [String] values - the field names of given JPA bean." + newLine +
                "+ *" + newLine +
                "  * @param entity the entity type" + newLine +
                "  * @return the new container which can be assigned to a [Grid]" + newLine +
                "  */" + newLine +
                "@@ -279,9 +285,12 @@" + newLine +
                "  * An utility method which adds an item and sets item's caption." + newLine +
                "  * @param the Identification of the item to be created." + newLine +
                "  * @param caption the new caption" + newLine +
                "+ * @return the newly created item ID." + newLine +
                "  */" + newLine +
                " fun AbstractSelect.addItem(itemId: Any?, caption: String) = addItem(itemId).apply { setItemCaption(itemId, caption) }!!" + newLine +
                " " + newLine +
                "+" + newLine +
                "+" + newLine +
                " /**" + newLine +
                "  * Walks over this component and all descendants of this component, breadth-first." + newLine +
                "  * @return iterable which iteratively walks over this component and all of its descendants.").replaceAll("\r", ""));

        jenkins.assertLogContains("Last changes from revision 27ad83a (current) to a511a43 (previous) published successfully!", build);
    }

    @Test
    void shouldNotGetLastChangesOfNotExistingBuild() throws Exception {

        // given
        List<UserRemoteConfig> remoteConfigs = new ArrayList<>();
        remoteConfigs.add(new UserRemoteConfig(sampleRepoDir.getAbsolutePath(), "origin", "", null));
        List<BranchSpec> branches = new ArrayList<>();
        branches.add(new BranchSpec("master"));
        GitSCM scm = new GitSCM(remoteConfigs, branches, false,
                Collections.emptyList(), null, null,
                Collections.singletonList(new DisableRemotePoll()));
        FreeStyleProject project = jenkins.createFreeStyleProject("git-test");
        project.setScm(scm);
        LastChangesPublisher publisher = new LastChangesPublisher(SinceType.PREVIOUS_REVISION, FormatType.LINE, MatchingType.NONE, true, false, "0.50", "1500", null, null, "99");
        project.getPublishersList().add(publisher);
        project.save();

        // when
        FreeStyleBuild build = jenkins.assertBuildStatus(Result.FAILURE, project.scheduleBuild2(0).get());

        // then
        jenkins.assertLogContains("No build found with number 99. Maybe the build was discarded or not has published LastChanges", build);
    }

    @Test
    void shouldGetLastChangesOfGitRepositoryOnSlaveNode() throws Exception {

        // given
        List<UserRemoteConfig> remoteConfigs = new ArrayList<>();
        remoteConfigs.add(new UserRemoteConfig(sampleRepoDir.getAbsolutePath(), "origin", "", null));
        List<BranchSpec> branches = new ArrayList<>();
        branches.add(new BranchSpec("master"));
        GitSCM scm = new GitSCM(remoteConfigs, branches, false,
                Collections.emptyList(), null, null,
                Collections.singletonList(new DisableRemotePoll()));
        DumbSlave slave = jenkins.createSlave();
        FreeStyleProject project = jenkins.createFreeStyleProject("git-test-slave");
        project.setAssignedNode(slave);
        project.setScm(scm);
        LastChangesPublisher publisher = new LastChangesPublisher(SinceType.PREVIOUS_REVISION, FormatType.SIDE, MatchingType.WORD, true, false, null, null, null, null, null);
        project.getPublishersList().add(publisher);
        project.save();

        // when
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

        // then
        LastChangesBuildAction action = build.getAction(LastChangesBuildAction.class);
        assertThat(action).isNotNull();
        LastChanges lastChanges = action.getBuildChanges();
        assertThat(lastChanges).isNotNull();
        assertThat(lastChanges.getCurrentRevision()).isNotNull();
        assertThat(lastChanges.getCurrentRevision().getCommitMessage()).isEqualTo("Added javadoc\n");
        assertThat(lastChanges.getCurrentRevision().getCommitId()).isEqualTo("27ad83a8fbee4b551670a03fc035bf87f7a3bcfb");
        assertThat(lastChanges.getDiff()).isEqualToIgnoringWhitespace(("diff --git a/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt b/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + newLine +
                "index 6d28c9b..bcc2ac0 100644" + newLine +
                "--- a/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + newLine +
                "+++ b/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + newLine +
                "@@ -31,6 +31,12 @@" + newLine +
                " /**" + newLine +
                "  * Creates a container which lists all instances of given entity. To restrict the list to a particular entity only," + newLine +
                "  * simply call [JPAContainer.addContainerFilter] on the container produced." + newLine +
                "+ *" + newLine +
                "+ * Containers produced by this method have the following properties:" + newLine +
                "+ * * The container's [Item] IDs are not the entity instances themselves - instead, [Item] ID contains the value of the JPA entity ID. This is important when using the container" + newLine +
                "+ * together with [AbstractSelect] as the select's value is taken amongst the Item ID." + newLine +
                "+ * * [Item]'s Property IDs are [String] values - the field names of given JPA bean." + newLine +
                "+ *" + newLine +
                "  * @param entity the entity type" + newLine +
                "  * @return the new container which can be assigned to a [Grid]" + newLine +
                "  */" + newLine +
                "@@ -279,9 +285,12 @@" + newLine +
                "  * An utility method which adds an item and sets item's caption." + newLine +
                "  * @param the Identification of the item to be created." + newLine +
                "  * @param caption the new caption" + newLine +
                "+ * @return the newly created item ID." + newLine +
                "  */" + newLine +
                " fun AbstractSelect.addItem(itemId: Any?, caption: String) = addItem(itemId).apply { setItemCaption(itemId, caption) }!!" + newLine +
                " " + newLine +
                "+" + newLine +
                "+" + newLine +
                " /**" + newLine +
                "  * Walks over this component and all descendants of this component, breadth-first." + newLine +
                "  * @return iterable which iteratively walks over this component and all of its descendants.").replaceAll("\r", ""));
        jenkins.assertLogContains("Last changes from revision 27ad83a (current) to a511a43 (previous) published successfully!", build);
    }

    @Test
    @Disabled("Test classpath issue, it throws: Caused by: org.tmatesoft.svn.core.SVNException: svn: E200007: Runner for 'org.tmatesoft.svn.core.wc2.SvnDiff' command have not been found; probably not yet implement in this API. Although it works on a \"real\" Jenkins.")
    void shouldGetLastChangesOfSvnRepository() throws Exception {

        // given
        ModuleLocation location = new ModuleLocation("https://subversion.assembla.com/svn/cucumber-json-files/trunk", "");
        List<ModuleLocation> locations = new ArrayList<>();
        locations.add(location);
        SvnSCM scm = new SvnSCM(".svn", sampleRepoDir, locations);//directory content is irrelevant cause LastChangesPublisher will look only into dir name (in case of svn)
        FreeStyleProject project = jenkins.createFreeStyleProject("svn-test");
        project.setScm(scm);
        LastChangesPublisher publisher = new LastChangesPublisher(SinceType.PREVIOUS_REVISION, FormatType.LINE, MatchingType.NONE, true, false, "0.50", "1500", null, null, null);
        project.getPublishersList().add(publisher);
        project.save();

        // when
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

        // then
        LastChangesBuildAction action = build.getAction(LastChangesBuildAction.class);
        assertThat(action).isNotNull();
        assertThat(action.getBuildChanges()).isNotNull();
        assertThat(action.getBuildChanges().getCurrentRevision().getCommitterName()).isEqualTo("rmpestano");
        jenkins.assertLogContains("published successfully!", build);
    }

    @Test
    void shouldNotGetLastChangesOfNonExistingRepository() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("non-existing-test");
        LastChangesPublisher publisher = new LastChangesPublisher(SinceType.PREVIOUS_REVISION, FormatType.LINE, MatchingType.NONE, true, false, "0.50", "1500", null, null, null);
        project.getPublishersList().add(publisher);
        project.save();

        // when
        FreeStyleBuild build = jenkins.assertBuildStatus(Result.FAILURE, project.scheduleBuild2(0).get());

        // then
        jenkins.assertLogContains(" Git or Svn directories not found in workspace", build);
    }
}
