package com.github.jenkins.lastchanges;

import com.github.jenkins.lastchanges.impl.SvnLastChanges;
import com.github.jenkins.lastchanges.model.LastChanges;
import hudson.scm.CredentialsSVNAuthenticationProviderImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationProvider;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNAuthenticationManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

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

    public static final String newLine = System.getProperty("line.separator");



    String svnRepoPath;
    String svnWithTagsRepoPath;

    @Before
    public void before() {
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
    public void shouldGetLastChanges() {
        File repository = new File(svnRepoPath);
        assertThat(repository).exists();
        LastChanges lastChanges = SvnLastChanges.getInstance().changesOf(repository);
        assertNotNull(lastChanges);
        assertThat(lastChanges.getCurrentRevision()).isNotNull();
        assertThat(lastChanges.getDiff()).isNotEmpty();
        assertThat(lastChanges.getCurrentRevision().getCommitMessage()).isEqualTo("removes theme and css files");
    }

    @Test
    public void shouldGetLastChangesFromLatestTag() throws SVNException {
        String pass = System.getProperty("PASS");
        if(pass == null || "".equals(pass)) {
            return;
        }
        File repository = new File(svnWithTagsRepoPath);
        assertThat(repository).exists();
        BasicAuthenticationManager basicAuthenticationManager = new BasicAuthenticationManager("rmpestano@gmail.com",pass);

        SVNRevision lastTagRevision = SvnLastChanges.getInstance().setSvnAuthManager(basicAuthenticationManager).getLastTagRevision(repository);
        LastChanges lastChanges = SvnLastChanges.getInstance().changesOf(repository, SVNRevision.HEAD,lastTagRevision);
        assertNotNull(lastChanges);
        assertThat(lastChanges.getCurrentRevision()).isNotNull();
        assertThat(lastChanges.getDiff()).isNotEmpty().isEqualToIgnoringWhitespace("Index: target/test-classes/svn-with-tags-repo/README.adoc\n" +
                "==================================================================="+newLine +
                "--- target/test-classes/svn-with-tags-repo/README.adoc\t(revision 5)"+newLine +
                "+++ target/test-classes/svn-with-tags-repo/README.adoc\t(revision 6)"+newLine +
                "@@ -1,4 +1,4 @@"+newLine +
                "-= Admin Persistence TEST SVN REPO"+newLine +
                "+= Admin Persistence"+newLine +
                " :page-layout: base"+newLine +
                " :source-language: java"+newLine +
                " :icons: font"+newLine);
        assertThat(lastChanges.getCurrentRevision().getCommitMessage()).isEqualTo("perf");
    }


}