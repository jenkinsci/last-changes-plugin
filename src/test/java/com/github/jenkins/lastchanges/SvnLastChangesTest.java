package com.github.jenkins.lastchanges;

import com.github.jenkins.lastchanges.impl.SvnLastChanges;
import com.github.jenkins.lastchanges.model.LastChanges;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.wc.SVNRevision;

import java.io.File;
import java.util.Locale;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Created by rmpestano on 6/5/16.
 */

class SvnLastChangesTest {

    private static final String newLine = System.lineSeparator();

    String svnRepoPath;
    String svnWithTagsRepoPath;

    @BeforeEach
    void before() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
        Locale.setDefault(Locale.ENGLISH);
        svnRepoPath = SvnLastChangesTest.class.getResource("/svn-sample-repo/").getFile();
        File file = new File(svnRepoPath + "svn");
        file.renameTo(new File(svnRepoPath + ".svn"));

        svnWithTagsRepoPath = SvnLastChangesTest.class.getResource("/svn-with-tags-repo/").getFile();
        file = new File(svnWithTagsRepoPath + "svn");
        file.renameTo(new File(svnWithTagsRepoPath + ".svn"));
    }

    @Test
    void shouldGetLastChanges() {
        String pass = System.getProperty("PASS");
        assumeTrue(pass != null);
        assumeFalse(pass.isEmpty());
        File repository = new File(svnRepoPath);
        assertThat(repository).exists();
        BasicAuthenticationManager basicAuthenticationManager = new BasicAuthenticationManager("rmpestano@gmail.com", pass);
        LastChanges lastChanges = SvnLastChanges.getInstance().setSvnAuthManager(basicAuthenticationManager).changesOf(repository);
        assertNotNull(lastChanges);
        assertThat(lastChanges.getCurrentRevision()).isNotNull();
        assertThat(lastChanges.getDiff()).isNotEmpty();
        assertThat(lastChanges.getCurrentRevision().getCommitMessage()).isEqualTo("removes theme and css files");
    }

    @Test
    void shouldGetLastChangesFromLatestTag() {
        String pass = System.getProperty("PASS");
        assumeTrue(pass != null);
        assumeFalse(pass.isEmpty());
        File repository = new File(svnWithTagsRepoPath);
        assertThat(repository).exists();
        BasicAuthenticationManager basicAuthenticationManager = new BasicAuthenticationManager("rmpestano@gmail.com", pass);

        SVNRevision lastTagRevision = SvnLastChanges.getInstance().setSvnAuthManager(basicAuthenticationManager).getLastTagRevision(repository);
        LastChanges lastChanges = SvnLastChanges.getInstance().changesOf(repository, SVNRevision.HEAD, lastTagRevision);
        assertNotNull(lastChanges);
        assertThat(lastChanges.getCurrentRevision()).isNotNull();
        assertThat(lastChanges.getDiff()).isNotEmpty().isEqualToIgnoringWhitespace("Index: target/test-classes/svn-with-tags-repo/README.adoc\n" +
                "===================================================================" + newLine +
                "--- target/test-classes/svn-with-tags-repo/README.adoc\t(revision 5)" + newLine +
                "+++ target/test-classes/svn-with-tags-repo/README.adoc\t(revision 6)" + newLine +
                "@@ -1,4 +1,4 @@" + newLine +
                "-= Admin Persistence TEST SVN REPO" + newLine +
                "+= Admin Persistence" + newLine +
                " :page-layout: base" + newLine +
                " :source-language: java" + newLine +
                " :icons: font" + newLine);
        assertThat(lastChanges.getCurrentRevision().getCommitMessage()).isEqualTo("perf");
    }
}
