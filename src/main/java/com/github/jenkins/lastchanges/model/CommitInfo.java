package com.github.jenkins.lastchanges.model;

import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.io.SVNRepository;

import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.logging.Logger;

/**
 * Created by rmpestano on 6/26/16.
 */
public class CommitInfo {

    private static final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT);
    private static final String newLine = System.getProperty("line.separator");

    private String commitId;
    private String commitMessage;
    private String commiterName;
    private String commiterEmail;
    private String commitDate;

    private CommitInfo() {
    }

    public String getCommiterName() {
        return commiterName;
    }

    public String getCommitDate() {
        return commitDate;
    }

    public String getCommitId() {
        return commitId;
    }

    public String getCommiterEmail() {
        return commiterEmail;
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder().
                append("Commit: ").append(commitId).append(newLine).
                append("Author: " + commiterName).append(newLine).
                append("E-mail: ").append(commiterEmail).append(newLine).
                append("Date: ").append(commitDate).append(newLine).
                append("Message: ").append(commitMessage).append(newLine).append(newLine);

        return sb.toString();
    }


    public static class Builder {

        public static CommitInfo buildFromSvn(SVNRepository repository,long revision ) throws SVNException {
            CommitInfo commitInfo = new CommitInfo();
            try {
                Collection<SVNLogEntry> entries = repository.log(new String[]{""}, null, revision, revision, true, true);
                Iterator<SVNLogEntry> iterator = entries.iterator();
                SVNLogEntry logEntry = iterator.next();
                TimeZone tz = TimeZone.getDefault();
                dateFormat.setTimeZone(tz);
                commitInfo.commitDate = dateFormat.format(logEntry.getDate()) + " " + tz.getDisplayName();
                commitInfo.commiterName = logEntry.getAuthor();
                commitInfo.commitId = logEntry.getRevision() + "";
                commitInfo.commitMessage = logEntry.getMessage();
            }catch (Exception e){
                Logger.getLogger(CommitInfo.class.getName()).warning(String.format("Could not get commit info from revision %d due to following error "+e.getMessage() + (e.getCause() != null ? " - " + e.getCause() : ""),revision));
            }
            return commitInfo;
        }

        public static CommitInfo buildFromGit(Repository repository, ObjectId commitId) {
            RevWalk walk = new RevWalk(repository);

            try {
                ObjectId lastCommitId = repository.resolve(Constants.HEAD);
                RevWalk revWalk = new RevWalk(repository);
                RevCommit commit = revWalk.parseCommit(lastCommitId);
                CommitInfo commitInfo = new CommitInfo();
                PersonIdent committerIdent = commit.getCommitterIdent();
                Date commitDate = committerIdent.getWhen();
                commitInfo.commitId = lastCommitId.getName();
                commitInfo.commitMessage = commit.getFullMessage();
                commitInfo.commiterName = committerIdent.getName();
                commitInfo.commiterEmail = committerIdent.getEmailAddress();
                TimeZone tz = committerIdent.getTimeZone() != null ? committerIdent.getTimeZone() : TimeZone.getDefault();
                dateFormat.setTimeZone(tz);
                commitInfo.commitDate = dateFormat.format(commitDate) + " " + tz.getDisplayName();
                return commitInfo;
            } catch (Exception e) {
                throw new RuntimeException("Could not get commit info for commit id: " + commitId, e);

            } finally {
                if (walk != null) {
                    walk.dispose();
                }
            }

        }
    }

}
