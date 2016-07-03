package com.github.jenkins.lastchanges;

import com.github.jenkins.lastchanges.impl.SvnLastChanges;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Locale;
import java.util.TimeZone;

import static org.junit.Assert.assertNotNull;

/**
 * Created by rmpestano on 6/5/16.
 */

@RunWith(JUnit4.class)
public class SvnLastChangesTest {
    public static final String newLine = System.getProperty("line.separator");


    String svnRepoPath;

    @Before
    public void before() {
        svnRepoPath = SvnLastChangesTest.class.getResource("/svn-sample-repo/").getFile();

        Locale.setDefault(Locale.ENGLISH);
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
    }

    @Test
    public void shouldInitRepository() {
        assertNotNull(SvnLastChanges.repository(svnRepoPath));
    }



}