package com.github.jenkins.lastchanges;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.file.Paths;

import static com.github.jenkins.lastchanges.LastChanges.lastChanges;
import static com.github.jenkins.lastchanges.LastChanges.repository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by rmpestano on 6/5/16.
 */

@RunWith(JUnit4.class)
public class LastChangesTest {
    File diffFile = new File("target/diff.txt");
    String gitRepoPath;

    @Before
    public void before() {
        if (diffFile.exists()) {
            diffFile.delete();
        }

        gitRepoPath = LastChangesTest.class.getResource("/git-sample-repo").getPath();
    }

    @Test
    public void shouldInitLastChanges() {
        assertNotNull(repository(gitRepoPath));
    }

    @Test
    public void shouldNotInitLastChangesWithBlankPath() {
        try {
            repository("");
        } catch (RuntimeException e) {
            assertEquals("Path cannot be empty.", e.getMessage());
        }
    }

    @Test
    public void shouldNotInitLastChangesWithNonExistingRepository() {
        String repoPath = Paths.get("").toAbsolutePath().toString();
        try {
            repository(repoPath);
        } catch (RuntimeException e) {
            assertEquals(String.format("No git repository found at %s.", repoPath), e.getMessage());
        }
    }


    @Test
    public void shouldWriteLastChanges() throws FileNotFoundException {
        lastChanges(repository(gitRepoPath), new FileOutputStream(diffFile));
        assertThat(diffFile).exists();
    }

}