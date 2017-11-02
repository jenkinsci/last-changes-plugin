/**
 * Created by rmpestano on 6/5/16.
 */
package com.github.jenkins.lastchanges.impl;

import com.github.jenkins.lastchanges.api.VCSChanges;
import com.github.jenkins.lastchanges.model.CommitInfo;
import com.github.jenkins.lastchanges.model.LastChanges;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnDiffGenerator;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.SvnDiff;
import org.tmatesoft.svn.core.wc2.SvnList;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnTarget;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SvnLastChanges implements VCSChanges<File, SVNRevision> {

    private static SvnLastChanges instance;
    private static final Logger LOG = Logger.getLogger(SvnLastChanges.class.getName());

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
            LOG.log(Level.SEVERE,"Could not retrieve last changes of svn repository located at " + repository + " due to following error: " + (e.getMessage() == null ? e.toString() : e.getMessage()) + (e.getCause() != null ? " - " + e.getCause() : ""), e);
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

    @Override
    public SVNRevision getLastTagRevision(File repository) {
        SvnOperationFactory operationFactory = new SvnOperationFactory();
        SvnList listOperation = operationFactory.createList();
        listOperation.setSingleTarget(SvnTarget.fromFile(repository));
        listOperation.setDepth(SVNDepth.IMMEDIATES);
        listOperation.setEntryFields(SVNDirEntry.DIRENT_ALL);
        try {
            Collection<SVNDirEntry> run = listOperation.run(new ArrayList<SVNDirEntry>());

            SVNDirEntry tags = null;
            for (SVNDirEntry svnDirEntry : run) {
                if (svnDirEntry.getName().equalsIgnoreCase("tags")) {
                    tags = svnDirEntry;
                    break;
                }
            }
            if (tags != null) {
                SVNDirEntry latestTag = findLastTag(tags);
                if(latestTag != null) {
                    return SVNRevision.create(latestTag.getRevision());
                } else {
                    throw new RuntimeException(String.format("Last tag not found on repository %s", repository.toPath().toAbsolutePath()));
                }
            } else {
                throw new RuntimeException(String.format("Tags branch not found on repository %s. Make sure your repository have the 'tags' directory.", repository.toPath().toAbsolutePath()));
            }


        }catch (Exception e) {
            LOG.log(Level.SEVERE,"Could not retrieve last tag revision of svn repository located at " + repository + " due to following error: " + (e.getMessage() == null ? e.toString() : e.getMessage()) + (e.getCause() != null ? " - " + e.getCause() : ""), e);
            throw new RuntimeException(String.format("Could not retrieve latest tag revision on repository %s due to following error: %s.", repository.toPath().toAbsolutePath(),e.getMessage() == null ? e.toString():e.getMessage()));
        }

    }

    private SVNDirEntry findLastTag(SVNDirEntry tagsDir) throws SVNException {
        SvnOperationFactory operationFactory = new SvnOperationFactory();
        SvnList listOperation = operationFactory.createList();
        listOperation.setSingleTarget(SvnTarget.fromURL(tagsDir.getURL()));
        Collection<SVNDirEntry> run = listOperation.run(new ArrayList<SVNDirEntry>());
        SVNDirEntry mostRecentTag = null;
        for (SVNDirEntry dirEntry : run) {
            if(mostRecentTag == null || mostRecentTag.getRevision() < dirEntry.getRevision()) {
                mostRecentTag = dirEntry;
            }
        }

        return mostRecentTag;

    }

}
