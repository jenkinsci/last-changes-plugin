package com.github.jenkins.lastchanges;

import com.github.jenkins.lastchanges.impl.SvnLastChanges;
import com.github.jenkins.lastchanges.model.LastChanges;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tmatesoft.svn.core.io.SVNRepository;

import java.io.File;
import java.util.Locale;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

/**
 * Created by rmpestano on 6/5/16.
 */

@RunWith(JUnit4.class)
public class SvnLastChangesTest {


    String svnRepoPath;

     @Before
    public void before() {
        svnRepoPath = SvnLastChangesTest.class.getResource("/svn-sample-repo/").getFile();
        Locale.setDefault(Locale.ENGLISH);
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
        File file = new File(svnRepoPath+"/svn");
        boolean renamed = file.renameTo(new File(svnRepoPath+"/.svn"));
    }

    @Test
    public void shouldGetLastChanges() {
            File repository = new File(svnRepoPath);
            assertThat(repository).exists();
            LastChanges lastChanges = SvnLastChanges.getInstance().changesOf(repository);
            assertNotNull(lastChanges);
            assertThat(lastChanges.getCurrentRevision()).isNotNull();
            assertThat(lastChanges.getDiff()).isNotEmpty();
            assertThat(lastChanges.getCurrentRevision().getCommitMessage()).isEqualTo("removes theme and css files");

    }



}