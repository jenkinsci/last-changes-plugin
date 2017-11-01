/**
 * Created by rmpestano on 6/5/16.
 */
package com.github.jenkins.lastchanges.impl;

import com.github.jenkins.lastchanges.api.VCSChanges;
import com.github.jenkins.lastchanges.exception.RepositoryNotFoundException;
import com.github.jenkins.lastchanges.model.CommitInfo;
import com.github.jenkins.lastchanges.model.LastChanges;
import hudson.EnvVars;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.scm.SubversionSCM;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationProvider;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnDiffGenerator;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.tmatesoft.svn.core.wc2.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;

public class SvnLastChanges implements VCSChanges<File, SVNRevision> {

    private static SvnLastChanges instance;

    public static SvnLastChanges getInstance() {
        if (instance == null) {
            instance = new SvnLastChanges();
        }
        return instance;
    }

    /**
     * Creates last changes from repository last two revisions
     *
     * @param repository svn repository to get last changes
     * @return LastChanges commit info and svn diff
     */
    @Override
    public LastChanges changesOf(File repository) {
        try {
            return changesOf(repository, SVNRevision.HEAD, SVNRevision.PREVIOUS);
        } catch (Exception e) {
            throw new RuntimeException("Could not retrieve last changes of svn repository located at " + repository + " due to following error: " + (e.getMessage() == null ? e.toString() : e.getMessage()) + (e.getCause() != null ? " - " + e.getCause() : ""), e);
        }
    }

    /**
     * Creates last changes from two revisions of repository
     *
     * @param repository svn repository to get last changes
     * @return LastChanges commit info and svn diff
     */
    @Override
    public LastChanges changesOf(File repository, SVNRevision currentRevision, SVNRevision previousRevision) {
        ByteArrayOutputStream diffStream = null;
        try {
            SvnOperationFactory operationFactory = new SvnOperationFactory();
            SvnDiff diff = operationFactory.createDiff();
            diff.setSingleTarget(
                    SvnTarget.fromFile(repository)
            );

            final SvnDiffGenerator diffGenerator = new SvnDiffGenerator();
            diffGenerator.setBasePath(new File(""));
            diffStream = new ByteArrayOutputStream();

            diff.setSources(SvnTarget.fromFile(repository, previousRevision),
                    SvnTarget.fromFile(repository, currentRevision));
            diff.setDiffGenerator(diffGenerator);
            diff.setOutput(diffStream);
            diff.run();

            CommitInfo lastCommitInfo = CommitInfo.Builder.buildFromSvn(repository, currentRevision);
            CommitInfo oldCommitInfo = CommitInfo.Builder.buildFromSvn(repository, previousRevision);


            return new LastChanges(lastCommitInfo, oldCommitInfo, new String(diffStream.toByteArray(), Charset.forName("UTF-8")));
        } catch (Exception e) {
            throw new RuntimeException("Could not retrieve last changes of svn repository located at " + repository + " due to following error: " + (e.getMessage() == null ? e.toString() : e.getMessage()) + (e.getCause() != null ? " - " + e.getCause() : ""), e);

        } finally {
            if (diffStream != null) {
                try {
                    diffStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

}
