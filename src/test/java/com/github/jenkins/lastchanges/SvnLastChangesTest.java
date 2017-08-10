package com.github.jenkins.lastchanges;

import com.github.jenkins.lastchanges.impl.SvnLastChanges;
import com.github.jenkins.lastchanges.model.LastChanges;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tmatesoft.svn.core.io.SVNRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

/**
 * Created by rmpestano on 6/5/16.
 */

@RunWith(JUnit4.class)
public class SvnLastChangesTest {


    final String svnRepoUrl = "https://subversion.assembla.com/svn/cucumber-json-files/trunk";

    @Test
    public void shouldInitRepository() {
        assertNotNull(SvnLastChanges.repository(svnRepoUrl));
    }

    @Test
    public void shouldGetLastChanges() {

            SVNRepository repository = SvnLastChanges.repository(svnRepoUrl);
            assertNotNull(repository);
            LastChanges lastChanges = SvnLastChanges.getInstance().changesOf(repository);
            assertNotNull(lastChanges);
            assertThat(lastChanges.getCurrentRevision()).isNotNull();
            assertThat(lastChanges.getDiff()).isNotEmpty();
            assertThat(lastChanges.getCurrentRevision().getCommitMessage()).isEqualTo("removes theme and css files");

    }



}