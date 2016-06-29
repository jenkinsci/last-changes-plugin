package com.github.jenkins.lastchanges;

import com.github.jenkins.lastchanges.model.LastChanges;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.slaves.DumbSlave;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import static org.assertj.core.api.Java6Assertions.assertThat;

@RunWith(JUnit4.class)
public class LastChangesIT {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private File sampleRepoDir = new File(LastChangesTest.class.getResource("/git-sample-repo").getFile());



    @Test
    public void shouldGenerateDiffFile() throws Exception {

        // given
        DirectorySCM scm = new DirectorySCM(".git",sampleRepoDir);
        FreeStyleProject project = jenkins.createFreeStyleProject("test");
        project.setScm(scm);
        LastChangesPublisher publisher = new LastChangesPublisher();
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
        assertThat(lastChanges.getCommitInfo()).isNotNull();
        assertThat(lastChanges.getCommitInfo().getCommitMessage()).isEqualTo("Added javadoc\n");
        assertThat(lastChanges.getCommitInfo().getCommitId()).isEqualTo("bb8a132b314888f2e8bee83bf534fa3e3f2815f9");
        Assertions.assertThat(lastChanges.getChanges()).isEqualToIgnoringWhitespace(("diff --git a/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt b/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + LastChangesTest.newLine +
                "index 6d28c9b..bcc2ac0 100644" + LastChangesTest.newLine +
                "--- a/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + LastChangesTest.newLine +
                "+++ b/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + LastChangesTest.newLine +
                "@@ -31,6 +31,12 @@" + LastChangesTest.newLine +
                " /**" + LastChangesTest.newLine +
                "  * Creates a container which lists all instances of given entity. To restrict the list to a particular entity only," + LastChangesTest.newLine +
                "  * simply call [JPAContainer.addContainerFilter] on the container produced." + LastChangesTest.newLine +
                "+ *" + LastChangesTest.newLine +
                "+ * Containers produced by this method have the following properties:" + LastChangesTest.newLine +
                "+ * * The container's [Item] IDs are not the entity instances themselves - instead, [Item] ID contains the value of the JPA entity ID. This is important when using the container" + LastChangesTest.newLine +
                "+ * together with [AbstractSelect] as the select's value is taken amongst the Item ID." + LastChangesTest.newLine +
                "+ * * [Item]'s Property IDs are [String] values - the field names of given JPA bean." + LastChangesTest.newLine +
                "+ *" + LastChangesTest.newLine +
                "  * @param entity the entity type" + LastChangesTest.newLine +
                "  * @return the new container which can be assigned to a [Grid]" + LastChangesTest.newLine +
                "  */" + LastChangesTest.newLine +
                "@@ -279,9 +285,12 @@" + LastChangesTest.newLine +
                "  * An utility method which adds an item and sets item's caption." + LastChangesTest.newLine +
                "  * @param the Identification of the item to be created." + LastChangesTest.newLine +
                "  * @param caption the new caption" + LastChangesTest.newLine +
                "+ * @return the newly created item ID." + LastChangesTest.newLine +
                "  */" + LastChangesTest.newLine +
                " fun AbstractSelect.addItem(itemId: Any?, caption: String) = addItem(itemId).apply { setItemCaption(itemId, caption) }!!" + LastChangesTest.newLine +
                " " + LastChangesTest.newLine +
                "+" + LastChangesTest.newLine +
                "+" + LastChangesTest.newLine +
                " /**" + LastChangesTest.newLine +
                "  * Walks over this component and all descendants of this component, breadth-first." + LastChangesTest.newLine +
                "  * @return iterable which iteratively walks over this component and all of its descendants.").replaceAll("\r", ""));
        

        jenkins.assertLogContains("Last changes generated successfully!",build);

    }

    @Test
    public void shouldGenerateDiffFileOnSlaveNode() throws Exception {

        // given
        DirectorySCM scm = new DirectorySCM(".git",sampleRepoDir);
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
        LastChangesBuildAction action = build.getAction(LastChangesBuildAction.class);
        assertThat(action).isNotNull();
        LastChanges lastChanges = action.getBuildChanges();
        assertThat(lastChanges).isNotNull();
        assertThat(lastChanges).isNotNull();
        assertThat(lastChanges.getCommitInfo()).isNotNull();
        assertThat(lastChanges.getCommitInfo().getCommitMessage()).isEqualTo("Added javadoc\n");
        assertThat(lastChanges.getCommitInfo().getCommitId()).isEqualTo("bb8a132b314888f2e8bee83bf534fa3e3f2815f9");
        Assertions.assertThat(lastChanges.getChanges()).isEqualToIgnoringWhitespace(("diff --git a/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt b/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + LastChangesTest.newLine +
                "index 6d28c9b..bcc2ac0 100644" + LastChangesTest.newLine +
                "--- a/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + LastChangesTest.newLine +
                "+++ b/kotlinee-framework/src/main/java/com/github/kotlinee/framework/vaadin/VaadinUtils.kt" + LastChangesTest.newLine +
                "@@ -31,6 +31,12 @@" + LastChangesTest.newLine +
                " /**" + LastChangesTest.newLine +
                "  * Creates a container which lists all instances of given entity. To restrict the list to a particular entity only," + LastChangesTest.newLine +
                "  * simply call [JPAContainer.addContainerFilter] on the container produced." + LastChangesTest.newLine +
                "+ *" + LastChangesTest.newLine +
                "+ * Containers produced by this method have the following properties:" + LastChangesTest.newLine +
                "+ * * The container's [Item] IDs are not the entity instances themselves - instead, [Item] ID contains the value of the JPA entity ID. This is important when using the container" + LastChangesTest.newLine +
                "+ * together with [AbstractSelect] as the select's value is taken amongst the Item ID." + LastChangesTest.newLine +
                "+ * * [Item]'s Property IDs are [String] values - the field names of given JPA bean." + LastChangesTest.newLine +
                "+ *" + LastChangesTest.newLine +
                "  * @param entity the entity type" + LastChangesTest.newLine +
                "  * @return the new container which can be assigned to a [Grid]" + LastChangesTest.newLine +
                "  */" + LastChangesTest.newLine +
                "@@ -279,9 +285,12 @@" + LastChangesTest.newLine +
                "  * An utility method which adds an item and sets item's caption." + LastChangesTest.newLine +
                "  * @param the Identification of the item to be created." + LastChangesTest.newLine +
                "  * @param caption the new caption" + LastChangesTest.newLine +
                "+ * @return the newly created item ID." + LastChangesTest.newLine +
                "  */" + LastChangesTest.newLine +
                " fun AbstractSelect.addItem(itemId: Any?, caption: String) = addItem(itemId).apply { setItemCaption(itemId, caption) }!!" + LastChangesTest.newLine +
                " " + LastChangesTest.newLine +
                "+" + LastChangesTest.newLine +
                "+" + LastChangesTest.newLine +
                " /**" + LastChangesTest.newLine +
                "  * Walks over this component and all descendants of this component, breadth-first." + LastChangesTest.newLine +
                "  * @return iterable which iteratively walks over this component and all of its descendants.").replaceAll("\r", ""));

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
