package com.github.jenkins.lastchanges;

import com.github.jenkins.lastchanges.exception.GitTreeNotFoundException;
import com.github.jenkins.lastchanges.impl.GitLastChanges;
import com.github.jenkins.lastchanges.model.LastChanges;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryBuilder;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

import static com.github.jenkins.lastchanges.impl.GitLastChanges.repository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

/**
 * Created by rmpestano on 6/5/16.
 */

@RunWith(JUnit4.class)
public class GitLastChangesTest {
    public static final String newLine = System.getProperty("line.separator");


    String gitRepoPath;


    @Before
    public void before() {
        gitRepoPath = GitLastChangesTest.class.getResource("/git-sample-repo").getFile();

        Locale.setDefault(Locale.ENGLISH);
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
    }

    @Test
    public void shouldInitRepository() {
        assertNotNull(repository(gitRepoPath));
    }

    @Test
    public void shouldNotInitRepositoryWithBlankPath() {
        try {
            repository("");
        } catch (RuntimeException e) {
            assertEquals("Git repository path cannot be empty.", e.getMessage());
        }
    }

    @Test
    public void shouldNotInitRepositoryWithNonExistingRepository() {
        String repoPath = Paths.get("").toAbsolutePath().toString();
        try {
            repository(repoPath);
        } catch (RuntimeException e) {
            assertEquals(String.format("No git repository found at %s.", repoPath), e.getMessage());
        }
    }


    @Test
    public void shouldGetLastChangesFromGitRepository() throws FileNotFoundException {
        LastChanges lastChanges = GitLastChanges.getInstance().changesOf(repository(gitRepoPath));
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
    }


    @Test
    public void shouldGetLastChangesFromInitialCommitGitRepo() throws FileNotFoundException {
        String repositoryLocation = GitLastChangesTest.class.getResource("/git-initial-commit-repo").getFile();
        File file = new File(repositoryLocation);
        try {
            GitLastChanges.getInstance().changesOf(repository(repositoryLocation));
            fail("Should not get here");
        } catch (GitTreeNotFoundException e) {
            assertThat(e.getMessage()).isEqualTo(String.format("Could not find previous head of repository located at %s. Its your first commit?", file.getAbsolutePath()));
        }
    }

    @Test
    public void shouldGetLastChangesFromSinceLastTag() throws FileNotFoundException {
        String repositoryLocation = GitLastChangesTest.class.getResource("/git-with-tags-repo").getFile();
        File file = new File(repositoryLocation);
        try {
            Repository repository = repository(repositoryLocation);

            ObjectId head = repository.resolve("HEAD^{tree}");
            ObjectId lastTagCommit = GitLastChanges.getInstance().getLastTagRevision(repository);
            LastChanges lastChanges = GitLastChanges.getInstance().changesOf(repository(repositoryLocation), head, lastTagCommit);
            assertThat(lastChanges).isNotNull();
            assertThat(lastChanges.getDiff()).isEqualToIgnoringWhitespace("diff --git a/pom.xml b/pom.xml"+newLine +
                    "index d93b4a4..0fcdc51 100644"+newLine +
                    "--- a/pom.xml"+newLine +
                    "+++ b/pom.xml"+newLine +
                    "@@ -11,7 +11,7 @@"+newLine +
                    " "+newLine +
                    "     <groupId>com.github.adminfaces</groupId>"+newLine +
                    "     <artifactId>admin-persistence</artifactId>"+newLine +
                    "-    <version>1.0.0-RC10</version>"+newLine +
                    "+    <version>1.0.0-RC11-SNAPSHOT</version>"+newLine +
                    " "+newLine +
                    "     <name>admin-persistence</name>"+newLine +
                    "     <url>https://github.com/adminfaces/admin-persistence</url>"+newLine);
            
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo(String.format("Could not find previous head of repository located at %s. Its your first commit?", file.getAbsolutePath()));
        }
    }


}