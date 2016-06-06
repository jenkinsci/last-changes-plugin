package com.github.jenkins.lastchanges

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import java.io.FileDescriptor
import java.nio.file.Paths

/**
 * Created by rmpestano on 6/5/16.
 */

class LastChangesTest {
    val diffFile = File("target/diff.txt")
    val currentGitRepo = Paths.get(".git").toAbsolutePath().toString();

    @Before
    fun before() {
        if (diffFile.exists())
            diffFile.delete()
    }

    @Test
    fun shouldInitLastChanges() {
        assertNotNull(repo(currentGitRepo))
    }

    @Test
    fun shouldNotInitLastChangesWithBlankPath() {
        try {
            repo("")
        } catch(e: RuntimeException) {
            assertEquals("Path cannot be empty.", e.message)
        }
    }

    @Test
    fun shouldNotInitLastChangesWithNonExistingRepository() {
        val repoPath = Paths.get("").toAbsolutePath().toString()
        try {
            repo(repoPath)
        } catch(e: RuntimeException) {
            assertEquals(String.format("No git repository found at %s.", repoPath), e.message)
        }
    }


    @Test
    fun shouldWriteLastChanges() {
        lastChanges(repo(currentGitRepo), FileOutputStream(diffFile))
        assertThat(diffFile).exists();
    }
}