package com.github.jenkins.lastchanges;

import com.github.jenkins.lastchanges.model.*;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.SubmoduleConfig;
import hudson.plugins.git.UserRemoteConfig;
import hudson.plugins.git.extensions.GitSCMExtension;
import hudson.plugins.git.extensions.impl.DisableRemotePoll;
import hudson.scm.SubversionSCM.ModuleLocation;
import hudson.slaves.DumbSlave;
import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;

@RunWith(JUnit4.class)
public class LastChangesIT {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private File sampleRepoDir = new File(LastChangesIT.class.getResource("/git-sample-repo").getFile());



    @Test
    public void shouldGetLastChangesOfGitRepository() throws Exception {

        // given
        List<UserRemoteConfig> remoteConfigs = new ArrayList<UserRemoteConfig>();
        remoteConfigs.add(new UserRemoteConfig(sampleRepoDir.getAbsolutePath(), "origin", "", null));
        List<BranchSpec> branches = new ArrayList<>();
        branches.add(new BranchSpec("master"));
        GitSCM scm = new GitSCM(remoteConfigs, branches, false,
                Collections.<SubmoduleConfig>emptyList(), null, null,
                Collections.<GitSCMExtension>singletonList(new DisableRemotePoll()));
        FreeStyleProject project = jenkins.createFreeStyleProject("git-test");
        project.setScm(scm);
        LastChangesPublisher publisher = new LastChangesPublisher(SinceType.PREVIOUS_REVISION, FormatType.LINE,MatchingType.NONE, true, false, "0.50","1500",null,null, null);
        project.getPublishersList().add(publisher);
        project.save();

        // when
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

        // then
        LastChangesBuildAction action = build.getAction(LastChangesBuildAction.class);
        assertThat(action).isNotNull();
        LastChanges lastChanges = action.getBuildChanges();
        assertThat(lastChanges).isNotNull();
        assertThat(lastChanges).isNotNull();
        assertThat(lastChanges.getCurrentRevision()).isNotNull();
        assertThat(lastChanges.getCurrentRevision().getCommitMessage()).isEqualTo("Added javadoc\n");
        assertThat(lastChanges.getCurrentRevision().getCommitId()).isEqualTo("27ad83a8fbee4b551670a03fc035bf87f7a3bcfb");

        final String diff = ("diff --git a/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt b/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + GitLastChangesTest.newLine +
                "index 6d28c9b..bcc2ac0 100644" + GitLastChangesTest.newLine +
                "--- a/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + GitLastChangesTest.newLine +
                "+++ b/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + GitLastChangesTest.newLine +
                "@@ -31,6 +31,12 @@" + GitLastChangesTest.newLine +
                " /**" + GitLastChangesTest.newLine +
                "  * Creates a container which lists all instances of given entity. To restrict the list to a particular entity only," + GitLastChangesTest.newLine +
                "  * simply call [JPAContainer.addContainerFilter] on the container produced." + GitLastChangesTest.newLine +
                "+ *" + GitLastChangesTest.newLine +
                "+ * Containers produced by this method have the following properties:" + GitLastChangesTest.newLine +
                "+ * * The container's [Item] IDs are not the entity instances themselves - instead, [Item] ID contains the value of the JPA entity ID. This is important when using the container" + GitLastChangesTest.newLine +
                "+ * together with [AbstractSelect] as the select's value is taken amongst the Item ID." + GitLastChangesTest.newLine +
                "+ * * [Item]'s Property IDs are [String] values - the field names of given JPA bean." + GitLastChangesTest.newLine +
                "+ *" + GitLastChangesTest.newLine +
                "  * @param entity the entity type" + GitLastChangesTest.newLine +
                "  * @return the new container which can be assigned to a [Grid]" + GitLastChangesTest.newLine +
                "  */" + GitLastChangesTest.newLine +
                "@@ -279,9 +285,12 @@" + GitLastChangesTest.newLine +
                "  * An utility method which adds an item and sets item's caption." + GitLastChangesTest.newLine +
                "  * @param the Identification of the item to be created." + GitLastChangesTest.newLine +
                "  * @param caption the new caption" + GitLastChangesTest.newLine +
                "+ * @return the newly created item ID." + GitLastChangesTest.newLine +
                "  */" + GitLastChangesTest.newLine +
                " fun AbstractSelect.addItem(itemId: Any?, caption: String) = addItem(itemId).apply { setItemCaption(itemId, caption) }!!" + GitLastChangesTest.newLine +
                " " + GitLastChangesTest.newLine +
                "+" + GitLastChangesTest.newLine +
                "+" + GitLastChangesTest.newLine +
                " /**" + GitLastChangesTest.newLine +
                "  * Walks over this component and all descendants of this component, breadth-first." + GitLastChangesTest.newLine +
                "  * @return iterable which iteratively walks over this component and all of its descendants.").replaceAll("\r", "");

        Assertions.assertThat(lastChanges.getDiff()).isEqualToIgnoringWhitespace(diff);
        

        assertThat(lastChanges.getCommits()).isNotNull()
                .hasSize(1);
        assertThat(lastChanges.getCommits().get(0))
                .isEqualTo(new CommitChanges(new CommitInfo().setCommitId("27ad83a8fbee4b551670a03fc035bf87f7a3bcfb"),null));

        assertThat(lastChanges.getCommits().get(0)
                .getChanges()).isEqualToIgnoringWhitespace(diff);


        jenkins.assertLogContains("Last changes from revision 27ad83a to a511a43 published successfully!", build);

    }

    @Ignore("Can't test it because when cloning git plugin will create just a .git and it is done at execution time, so there is nothing we can do but test manually")
    @Test
    public void shouldGetLastChangesUsingVcsDir() throws Exception {

        // given
        List<UserRemoteConfig> remoteConfigs = new ArrayList<UserRemoteConfig>();
        remoteConfigs.add(new UserRemoteConfig(sampleRepoDir.getParentFile().getAbsolutePath(), "origin", "", null));
        List<BranchSpec> branches = new ArrayList<>();
        branches.add(new BranchSpec("master"));
        GitSCM scm = new GitSCM(remoteConfigs, branches, false,
                Collections.<SubmoduleConfig>emptyList(), null, null,
                Collections.<GitSCMExtension>singletonList(new DisableRemotePoll()));
        FreeStyleProject project = jenkins.createFreeStyleProject("git-test");
        project.setScm(scm);
        LastChangesPublisher publisher = new LastChangesPublisher(SinceType.PREVIOUS_REVISION, FormatType.LINE,MatchingType.NONE, true, false, "0.50","1500",null,"git-sample-repo", null);
        project.getPublishersList().add(publisher);
        project.save();

        // when
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

        // then
        LastChangesBuildAction action = build.getAction(LastChangesBuildAction.class);
        assertThat(action).isNotNull();
        LastChanges lastChanges = action.getBuildChanges();
        assertThat(lastChanges).isNotNull();
        assertThat(lastChanges).isNotNull();
        assertThat(lastChanges.getCurrentRevision()).isNotNull();
        assertThat(lastChanges.getCurrentRevision().getCommitMessage()).isEqualTo("Added javadoc\n");
        assertThat(lastChanges.getCurrentRevision().getCommitId()).isEqualTo("27ad83a8fbee4b551670a03fc035bf87f7a3bcfb");
        Assertions.assertThat(lastChanges.getDiff()).isEqualToIgnoringWhitespace(("diff --git a/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt b/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + GitLastChangesTest.newLine +
                "index 6d28c9b..bcc2ac0 100644" + GitLastChangesTest.newLine +
                "--- a/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + GitLastChangesTest.newLine +
                "+++ b/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + GitLastChangesTest.newLine +
                "@@ -31,6 +31,12 @@" + GitLastChangesTest.newLine +
                " /**" + GitLastChangesTest.newLine +
                "  * Creates a container which lists all instances of given entity. To restrict the list to a particular entity only," + GitLastChangesTest.newLine +
                "  * simply call [JPAContainer.addContainerFilter] on the container produced." + GitLastChangesTest.newLine +
                "+ *" + GitLastChangesTest.newLine +
                "+ * Containers produced by this method have the following properties:" + GitLastChangesTest.newLine +
                "+ * * The container's [Item] IDs are not the entity instances themselves - instead, [Item] ID contains the value of the JPA entity ID. This is important when using the container" + GitLastChangesTest.newLine +
                "+ * together with [AbstractSelect] as the select's value is taken amongst the Item ID." + GitLastChangesTest.newLine +
                "+ * * [Item]'s Property IDs are [String] values - the field names of given JPA bean." + GitLastChangesTest.newLine +
                "+ *" + GitLastChangesTest.newLine +
                "  * @param entity the entity type" + GitLastChangesTest.newLine +
                "  * @return the new container which can be assigned to a [Grid]" + GitLastChangesTest.newLine +
                "  */" + GitLastChangesTest.newLine +
                "@@ -279,9 +285,12 @@" + GitLastChangesTest.newLine +
                "  * An utility method which adds an item and sets item's caption." + GitLastChangesTest.newLine +
                "  * @param the Identification of the item to be created." + GitLastChangesTest.newLine +
                "  * @param caption the new caption" + GitLastChangesTest.newLine +
                "+ * @return the newly created item ID." + GitLastChangesTest.newLine +
                "  */" + GitLastChangesTest.newLine +
                " fun AbstractSelect.addItem(itemId: Any?, caption: String) = addItem(itemId).apply { setItemCaption(itemId, caption) }!!" + GitLastChangesTest.newLine +
                " " + GitLastChangesTest.newLine +
                "+" + GitLastChangesTest.newLine +
                "+" + GitLastChangesTest.newLine +
                " /**" + GitLastChangesTest.newLine +
                "  * Walks over this component and all descendants of this component, breadth-first." + GitLastChangesTest.newLine +
                "  * @return iterable which iteratively walks over this component and all of its descendants.").replaceAll("\r", ""));


        jenkins.assertLogContains("Last changes from revision 27ad83a to a511a43 published successfully!", build);

    }


    @Test
    public void shouldGetLastChangesOfLastSuccessfulBuild() throws Exception {

        // given
        List<UserRemoteConfig> remoteConfigs = new ArrayList<UserRemoteConfig>();
        remoteConfigs.add(new UserRemoteConfig(sampleRepoDir.getAbsolutePath(), "origin", "", null));
        List<BranchSpec> branches = new ArrayList<>();
        branches.add(new BranchSpec("master"));
        GitSCM scm = new GitSCM(remoteConfigs, branches, false,
                Collections.<SubmoduleConfig>emptyList(), null, null,
                Collections.<GitSCMExtension>singletonList(new DisableRemotePoll()));
        FreeStyleProject project = jenkins.createFreeStyleProject("git-test");
        project.setScm(scm);
        LastChangesPublisher publisher = new LastChangesPublisher(SinceType.LAST_SUCCESSFUL_BUILD, FormatType.LINE,MatchingType.NONE, true, false, "0.50","1500",null,null, null);
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
        Assertions.assertThat(lastChanges.getDiff()).isEqualToIgnoringWhitespace(("diff --git a/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt b/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + GitLastChangesTest.newLine +
                "index 6d28c9b..bcc2ac0 100644" + GitLastChangesTest.newLine +
                "--- a/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + GitLastChangesTest.newLine +
                "+++ b/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + GitLastChangesTest.newLine +
                "@@ -31,6 +31,12 @@" + GitLastChangesTest.newLine +
                " /**" + GitLastChangesTest.newLine +
                "  * Creates a container which lists all instances of given entity. To restrict the list to a particular entity only," + GitLastChangesTest.newLine +
                "  * simply call [JPAContainer.addContainerFilter] on the container produced." + GitLastChangesTest.newLine +
                "+ *" + GitLastChangesTest.newLine +
                "+ * Containers produced by this method have the following properties:" + GitLastChangesTest.newLine +
                "+ * * The container's [Item] IDs are not the entity instances themselves - instead, [Item] ID contains the value of the JPA entity ID. This is important when using the container" + GitLastChangesTest.newLine +
                "+ * together with [AbstractSelect] as the select's value is taken amongst the Item ID." + GitLastChangesTest.newLine +
                "+ * * [Item]'s Property IDs are [String] values - the field names of given JPA bean." + GitLastChangesTest.newLine +
                "+ *" + GitLastChangesTest.newLine +
                "  * @param entity the entity type" + GitLastChangesTest.newLine +
                "  * @return the new container which can be assigned to a [Grid]" + GitLastChangesTest.newLine +
                "  */" + GitLastChangesTest.newLine +
                "@@ -279,9 +285,12 @@" + GitLastChangesTest.newLine +
                "  * An utility method which adds an item and sets item's caption." + GitLastChangesTest.newLine +
                "  * @param the Identification of the item to be created." + GitLastChangesTest.newLine +
                "  * @param caption the new caption" + GitLastChangesTest.newLine +
                "+ * @return the newly created item ID." + GitLastChangesTest.newLine +
                "  */" + GitLastChangesTest.newLine +
                " fun AbstractSelect.addItem(itemId: Any?, caption: String) = addItem(itemId).apply { setItemCaption(itemId, caption) }!!" + GitLastChangesTest.newLine +
                " " + GitLastChangesTest.newLine +
                "+" + GitLastChangesTest.newLine +
                "+" + GitLastChangesTest.newLine +
                " /**" + GitLastChangesTest.newLine +
                "  * Walks over this component and all descendants of this component, breadth-first." + GitLastChangesTest.newLine +
                "  * @return iterable which iteratively walks over this component and all of its descendants.").replaceAll("\r", ""));


        jenkins.assertLogContains("Last changes from revision 27ad83a to a511a43 published successfully!", build);

    }

    @Test
    public void shouldGetLastChangesOfSpecificBuild() throws Exception {

        // given
        List<UserRemoteConfig> remoteConfigs = new ArrayList<UserRemoteConfig>();
        remoteConfigs.add(new UserRemoteConfig(sampleRepoDir.getAbsolutePath(), "origin", "", null));
        List<BranchSpec> branches = new ArrayList<>();
        branches.add(new BranchSpec("master"));
        GitSCM scm = new GitSCM(remoteConfigs, branches, false,
                Collections.<SubmoduleConfig>emptyList(), null, null,
                Collections.<GitSCMExtension>singletonList(new DisableRemotePoll()));
        FreeStyleProject project = jenkins.createFreeStyleProject("git-test");
        project.setScm(scm);
        LastChangesPublisher publisher = new LastChangesPublisher(SinceType.PREVIOUS_REVISION, FormatType.LINE,MatchingType.NONE, true, false, "0.50","1500",null,null, null);
        project.getPublishersList().add(publisher);
        project.save();

        // when
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);


        publisher = new LastChangesPublisher(SinceType.PREVIOUS_REVISION, FormatType.LINE,MatchingType.NONE, true, false, "0.50","1500",null,null, build.getNumber()+"");

        build = jenkins.buildAndAssertSuccess(project);

        // then
        LastChangesBuildAction action = build.getAction(LastChangesBuildAction.class);
        assertThat(action).isNotNull();
        LastChanges lastChanges = action.getBuildChanges();
        assertThat(lastChanges).isNotNull();
        assertThat(lastChanges).isNotNull();
        assertThat(lastChanges.getCurrentRevision()).isNotNull();
        assertThat(lastChanges.getCurrentRevision().getCommitMessage()).isEqualTo("Added javadoc\n");
        assertThat(lastChanges.getCurrentRevision().getCommitId()).isEqualTo("27ad83a8fbee4b551670a03fc035bf87f7a3bcfb");
        Assertions.assertThat(lastChanges.getDiff()).isEqualToIgnoringWhitespace(("diff --git a/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt b/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + GitLastChangesTest.newLine +
                "index 6d28c9b..bcc2ac0 100644" + GitLastChangesTest.newLine +
                "--- a/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + GitLastChangesTest.newLine +
                "+++ b/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + GitLastChangesTest.newLine +
                "@@ -31,6 +31,12 @@" + GitLastChangesTest.newLine +
                " /**" + GitLastChangesTest.newLine +
                "  * Creates a container which lists all instances of given entity. To restrict the list to a particular entity only," + GitLastChangesTest.newLine +
                "  * simply call [JPAContainer.addContainerFilter] on the container produced." + GitLastChangesTest.newLine +
                "+ *" + GitLastChangesTest.newLine +
                "+ * Containers produced by this method have the following properties:" + GitLastChangesTest.newLine +
                "+ * * The container's [Item] IDs are not the entity instances themselves - instead, [Item] ID contains the value of the JPA entity ID. This is important when using the container" + GitLastChangesTest.newLine +
                "+ * together with [AbstractSelect] as the select's value is taken amongst the Item ID." + GitLastChangesTest.newLine +
                "+ * * [Item]'s Property IDs are [String] values - the field names of given JPA bean." + GitLastChangesTest.newLine +
                "+ *" + GitLastChangesTest.newLine +
                "  * @param entity the entity type" + GitLastChangesTest.newLine +
                "  * @return the new container which can be assigned to a [Grid]" + GitLastChangesTest.newLine +
                "  */" + GitLastChangesTest.newLine +
                "@@ -279,9 +285,12 @@" + GitLastChangesTest.newLine +
                "  * An utility method which adds an item and sets item's caption." + GitLastChangesTest.newLine +
                "  * @param the Identification of the item to be created." + GitLastChangesTest.newLine +
                "  * @param caption the new caption" + GitLastChangesTest.newLine +
                "+ * @return the newly created item ID." + GitLastChangesTest.newLine +
                "  */" + GitLastChangesTest.newLine +
                " fun AbstractSelect.addItem(itemId: Any?, caption: String) = addItem(itemId).apply { setItemCaption(itemId, caption) }!!" + GitLastChangesTest.newLine +
                " " + GitLastChangesTest.newLine +
                "+" + GitLastChangesTest.newLine +
                "+" + GitLastChangesTest.newLine +
                " /**" + GitLastChangesTest.newLine +
                "  * Walks over this component and all descendants of this component, breadth-first." + GitLastChangesTest.newLine +
                "  * @return iterable which iteratively walks over this component and all of its descendants.").replaceAll("\r", ""));


        jenkins.assertLogContains("Last changes from revision 27ad83a to a511a43 published successfully!", build);

    }

    @Test
    public void shouldNotGetLastChangesOfNotExistingBuild() throws Exception {

        // given
        List<UserRemoteConfig> remoteConfigs = new ArrayList<UserRemoteConfig>();
        remoteConfigs.add(new UserRemoteConfig(sampleRepoDir.getAbsolutePath(), "origin", "", null));
        List<BranchSpec> branches = new ArrayList<>();
        branches.add(new BranchSpec("master"));
        GitSCM scm = new GitSCM(remoteConfigs, branches, false,
                Collections.<SubmoduleConfig>emptyList(), null, null,
                Collections.<GitSCMExtension>singletonList(new DisableRemotePoll()));
        FreeStyleProject project = jenkins.createFreeStyleProject("git-test");
        project.setScm(scm);
        LastChangesPublisher publisher = new LastChangesPublisher(SinceType.PREVIOUS_REVISION, FormatType.LINE,MatchingType.NONE, true, false, "0.50","1500",null,null, "99");
        project.getPublishersList().add(publisher);
        project.save();

        // when
        FreeStyleBuild build = jenkins.assertBuildStatus(Result.FAILURE,project.scheduleBuild2(0).get());


        // then
        jenkins.assertLogContains("No build found with number 99. Maybe the build was discarded or not has published LastChanges", build);

    }





    @Test
    public void shouldGetLastChangesOfGitRepositoryOnSlaveNode() throws Exception {

        // given
        List<UserRemoteConfig> remoteConfigs = new ArrayList<UserRemoteConfig>();
        remoteConfigs.add(new UserRemoteConfig(sampleRepoDir.getAbsolutePath(), "origin", "", null));
        List<BranchSpec> branches = new ArrayList<>();
        branches.add(new BranchSpec("master"));
        GitSCM scm = new GitSCM(remoteConfigs, branches, false,
                Collections.<SubmoduleConfig>emptyList(), null, null,
                Collections.<GitSCMExtension>singletonList(new DisableRemotePoll()));
        DumbSlave slave = jenkins.createSlave();
        FreeStyleProject project = jenkins.createFreeStyleProject("git-test-slave");
        project.setAssignedNode(slave);
        project.setScm(scm);
        LastChangesPublisher publisher = new LastChangesPublisher(SinceType.PREVIOUS_REVISION, FormatType.SIDE,MatchingType.WORD, true, false, null,null,null,null, null);
        project.getPublishersList().add(publisher);
        project.save();

        // when
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

        // then
        LastChangesBuildAction action = build.getAction(LastChangesBuildAction.class);
        assertThat(action).isNotNull();
        LastChanges lastChanges = action.getBuildChanges();
        assertThat(lastChanges).isNotNull();
        assertThat(lastChanges).isNotNull();
        assertThat(lastChanges.getCurrentRevision()).isNotNull();
        assertThat(lastChanges.getCurrentRevision().getCommitMessage()).isEqualTo("Added javadoc\n");
        assertThat(lastChanges.getCurrentRevision().getCommitId()).isEqualTo("27ad83a8fbee4b551670a03fc035bf87f7a3bcfb");
        Assertions.assertThat(lastChanges.getDiff()).isEqualToIgnoringWhitespace(("diff --git a/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt b/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + GitLastChangesTest.newLine +
                "index 6d28c9b..bcc2ac0 100644" + GitLastChangesTest.newLine +
                "--- a/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + GitLastChangesTest.newLine +
                "+++ b/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + GitLastChangesTest.newLine +
                "@@ -31,6 +31,12 @@" + GitLastChangesTest.newLine +
                " /**" + GitLastChangesTest.newLine +
                "  * Creates a container which lists all instances of given entity. To restrict the list to a particular entity only," + GitLastChangesTest.newLine +
                "  * simply call [JPAContainer.addContainerFilter] on the container produced." + GitLastChangesTest.newLine +
                "+ *" + GitLastChangesTest.newLine +
                "+ * Containers produced by this method have the following properties:" + GitLastChangesTest.newLine +
                "+ * * The container's [Item] IDs are not the entity instances themselves - instead, [Item] ID contains the value of the JPA entity ID. This is important when using the container" + GitLastChangesTest.newLine +
                "+ * together with [AbstractSelect] as the select's value is taken amongst the Item ID." + GitLastChangesTest.newLine +
                "+ * * [Item]'s Property IDs are [String] values - the field names of given JPA bean." + GitLastChangesTest.newLine +
                "+ *" + GitLastChangesTest.newLine +
                "  * @param entity the entity type" + GitLastChangesTest.newLine +
                "  * @return the new container which can be assigned to a [Grid]" + GitLastChangesTest.newLine +
                "  */" + GitLastChangesTest.newLine +
                "@@ -279,9 +285,12 @@" + GitLastChangesTest.newLine +
                "  * An utility method which adds an item and sets item's caption." + GitLastChangesTest.newLine +
                "  * @param the Identification of the item to be created." + GitLastChangesTest.newLine +
                "  * @param caption the new caption" + GitLastChangesTest.newLine +
                "+ * @return the newly created item ID." + GitLastChangesTest.newLine +
                "  */" + GitLastChangesTest.newLine +
                " fun AbstractSelect.addItem(itemId: Any?, caption: String) = addItem(itemId).apply { setItemCaption(itemId, caption) }!!" + GitLastChangesTest.newLine +
                " " + GitLastChangesTest.newLine +
                "+" + GitLastChangesTest.newLine +
                "+" + GitLastChangesTest.newLine +
                " /**" + GitLastChangesTest.newLine +
                "  * Walks over this component and all descendants of this component, breadth-first." + GitLastChangesTest.newLine +
                "  * @return iterable which iteratively walks over this component and all of its descendants.").replaceAll("\r", ""));
        jenkins.assertLogContains("Last changes from revision 27ad83a to a511a43 published successfully!",build);

    }
    
    @Test
    @Ignore("Test classpath issue, it throws: Caused by: org.tmatesoft.svn.core.SVNException: svn: E200007: Runner for 'org.tmatesoft.svn.core.wc2.SvnDiff' command have not been found; probably not yet implement in this API. Although it works on a \"real\" Jenkins.")
    public void shouldGetLastChangesOfSvnRepository() throws Exception {

        // given
    	ModuleLocation location = new ModuleLocation("https://subversion.assembla.com/svn/cucumber-json-files/trunk", ""); 
    	List<ModuleLocation> locations = new ArrayList<>();
    	locations.add(location);
        SvnSCM scm = new SvnSCM(".svn",sampleRepoDir,locations);//directory content is irrelevant cause LastChangesPublisher will look only into dir name (in case of svn)
        FreeStyleProject project = jenkins.createFreeStyleProject("svn-test");
        project.setScm(scm);
        LastChangesPublisher publisher = new LastChangesPublisher(SinceType.PREVIOUS_REVISION,FormatType.LINE,MatchingType.NONE, true, false, "0.50","1500",null,null, null);
        project.getPublishersList().add(publisher);
        project.save();
        
        
        // when
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

        // then
        LastChangesBuildAction action = build.getAction(LastChangesBuildAction.class);
        assertThat(action).isNotNull();
        assertThat(action.getBuildChanges()).isNotNull();
        assertThat(action.getBuildChanges().getCurrentRevision().getCommitterName()).isEqualTo("rmpestano");
        jenkins.assertLogContains("published successfully!",build);
    }

    @Test
    public void shouldNotGetLastChangesOfNonExistingRepository() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("non-existing-test");
        LastChangesPublisher publisher = new LastChangesPublisher(SinceType.PREVIOUS_REVISION,FormatType.LINE,MatchingType.NONE, true, false, "0.50","1500",null,null, null);
        project.getPublishersList().add(publisher);
        project.save();

        // when
        FreeStyleBuild build = jenkins.assertBuildStatus(Result.FAILURE,project.scheduleBuild2(0).get());

        // then
        jenkins.assertLogContains(" Git or Svn directories not found in workspace" ,build);
    }

}
