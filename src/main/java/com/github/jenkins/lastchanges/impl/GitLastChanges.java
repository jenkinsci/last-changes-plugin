/**
 * Created by rmpestano on 6/5/16.
 */
package com.github.jenkins.lastchanges.impl;

import com.github.jenkins.lastchanges.api.VCSChanges;
import com.github.jenkins.lastchanges.exception.GitDiffException;
import com.github.jenkins.lastchanges.exception.GitTreeNotFoundException;
import com.github.jenkins.lastchanges.exception.GitTreeParseException;
import com.github.jenkins.lastchanges.exception.RepositoryNotFoundException;
import com.github.jenkins.lastchanges.model.CommitInfo;
import com.github.jenkins.lastchanges.model.LastChanges;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class GitLastChanges implements VCSChanges<Repository, ObjectId> {


    private static GitLastChanges instance;


    private GitLastChanges() {
    }

    public static GitLastChanges getInstance() {
        if (instance == null) {
            instance = new GitLastChanges();
        }

        return instance;
    }


    /**
     * @param path local git repository path
     * @return underlying git repository from location path
     */
    public static Repository repository(String path) {
        if (path == null || path.isEmpty()) {
            throw new RepositoryNotFoundException("Git repository path cannot be empty.");
        }

        File repositoryPath = new File(path);

        if (!repositoryPath.exists()) {
            throw new RepositoryNotFoundException(String.format("Git repository path not found at location %s.", repositoryPath));
        }

        Repository repository = null;
        try {
            repository = new FileRepository(path);
        } catch (IOException e) {
            throw new RepositoryNotFoundException("Could not find git repository at " + path);
        }
        if (repository.isBare()) {
            throw new RepositoryNotFoundException(String.format("No git repository found at %s.", path));
        }

        return repository;

    }


    /**
     * Creates last changes from repository last two revisions
     *
     * @param repository git repository to get last changes
     * @return LastChanges commit info and git diff
     */
    @Override
    public LastChanges changesOf(Repository repository) {
        Git git = new Git(repository);
        try {
            String repositoryLocation = repository.getDirectory().getAbsolutePath();
            ObjectId head = resolveCurrentRevision(repository);
            ObjectId previousHead = null;
            try {
                previousHead = repository.resolve("HEAD~^{tree}");
                if (previousHead == null) {
                    throw new GitTreeNotFoundException(String.format("Could not find previous head of repository located at %s. Its your first commit?", repositoryLocation));
                }
            } catch (IOException e) {
                throw new GitTreeNotFoundException("Could not resolve previous head of repository located at " + repositoryLocation, e);
            }

            return changesOf(repository, head, previousHead);
        } finally {
            if (git != null) {
                git.close();
            }
            if (repository != null) {
                repository.close();
            }
        }

    }

    public ObjectId resolveCurrentRevision(Repository repository) {
        String repositoryLocation = repository.getDirectory().getAbsolutePath();
        try {
            return repository.resolve("HEAD^{tree}");
        } catch (IOException e) {
            throw new GitTreeNotFoundException("Could not resolve head of repository located at " + repositoryLocation, e);
        }

    }

    /**
     * Creates last changes by "diffing" two revisions
     *
     * @param repository git repository to get last changes
     * @return LastChanges commit info and git diff between revisions
     */
    @Override
    public LastChanges changesOf(Repository repository, ObjectId currentRevision, ObjectId previousRevision) {
        Git git = new Git(repository);
        try {
            ByteArrayOutputStream diffStream = new ByteArrayOutputStream();
            CommitInfo lastCommitInfo;
            CommitInfo oldCommitInfo;
            String repositoryLocation = repository.getDirectory().getAbsolutePath();
            DiffFormatter formatter = new DiffFormatter(diffStream);
            formatter.setRepository(repository);
            ObjectReader reader = repository.newObjectReader();

            lastCommitInfo = commitInfo(repository, currentRevision);
            oldCommitInfo = commitInfo(repository, previousRevision);

            // Create the tree iterator for each commit
            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            try {
                RevWalk revWalk = new RevWalk(repository);
                if (revWalk.parseAny(previousRevision) instanceof RevCommit) {
                    RevCommit revCommit = revWalk.parseCommit(previousRevision);
                    oldTreeIter.reset(reader, revCommit.getTree().getId());
                } else {
                    oldTreeIter.reset(reader, previousRevision);
                }
            } catch (Exception e) {
                throw new GitTreeParseException("Could not parse previous commit tree.", e);
            }
            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            try {
                RevWalk revWalk = new RevWalk(repository);
                if (revWalk.parseAny(currentRevision) instanceof RevCommit) {
                    RevCommit revCommit = revWalk.parseCommit(currentRevision);
                    newTreeIter.reset(reader, revCommit.getTree().getId());
                } else {
                    newTreeIter.reset(reader, currentRevision);
                }

            } catch (IOException e) {
                throw new GitTreeParseException("Could not parse current commit tree.", e);
            }
            try {
                for (DiffEntry change : git.diff().setOldTree(oldTreeIter).setNewTree(newTreeIter).call()) {
                    formatter.format(change);
                }
            } catch (Exception e) {
                throw new GitDiffException("Could not get last changes from repository located at " + repositoryLocation, e);
            }

            return new LastChanges(lastCommitInfo, oldCommitInfo, new String(diffStream.toByteArray(), Charset.forName("UTF-8")));
        } finally {
            if (git != null) {
                git.close();
            }
            if (repository != null) {
                repository.close();
            }
        }

    }


    @Override
    public ObjectId getLastTagRevision(Repository repository) {
        Git git = new Git(repository);
        List<Ref> tags = null;
        try {
            tags = git.tagList().call();

            final RevWalk walk = new RevWalk(repository);
            Collections.sort(tags, new Comparator<Ref>() {
                public int compare(Ref o1, Ref o2) {
                    java.util.Date d1 = null;
                    java.util.Date d2 = null;
                    try {
                        d1 = walk.parseCommit(o1.getObjectId()).getCommitterIdent().getWhen();
                        d2 = walk.parseCommit(o2.getObjectId()).getCommitterIdent().getWhen();
                        return d2.compareTo(d1);
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                    return 0;
                }
            });

            if (tags != null && !tags.isEmpty()) {
                Ref tag = tags.get(0);
                Ref peeledRef = repository.peel(tag);
                LogCommand log = git.log();
                if (peeledRef.getPeeledObjectId() != null) {
                    log.add(peeledRef.getPeeledObjectId());
                } else {
                    log.add(tag.getObjectId());
                }
                Iterable<RevCommit> logs = log.call();
                if (logs != null) {
                    return logs.iterator().next().getId();
                }
            }

            return null;

        } catch (Exception e) {
            throw new GitDiffException("Could not get last tag from repository located at " + repository.getDirectory().getAbsolutePath(), e);

        }
    }

    @Override
    public CommitInfo commitInfo(Repository repository, ObjectId commitId) {
        RevWalk revWalk = new RevWalk(repository);
        CommitInfo commitInfo = new CommitInfo();
        PersonIdent committerIdent = null;
        RevCommit commit = null;
        try {
            if (revWalk.parseAny(commitId) instanceof RevCommit) {
                commit = revWalk.parseCommit(commitId);
                committerIdent = commit.getCommitterIdent();
            } else if (revWalk.parseAny(commitId) instanceof RevTree) {

                RevCommit rootCommit = revWalk.parseCommit(repository.resolve(Constants.HEAD));
                revWalk.sort(RevSort.COMMIT_TIME_DESC);
                revWalk.markStart(rootCommit);
                //resolve commit from tree
                RevTree tree = revWalk.parseTree(commitId);
                for (RevCommit revCommit : revWalk) {
                    if (revCommit.getTree().getId().equals(tree.getId())) {
                        commit = revCommit;
                        committerIdent = commit.getCommitterIdent();
                        break;
                    }
                }

            }

            Date commitDate = committerIdent.getWhen();
            commitInfo.setCommitId(commit.getName())
                    .setCommitMessage(commit.getFullMessage())
                    .setCommitterName(committerIdent.getName())
                    .setCommitterEmail(committerIdent.getEmailAddress());
            TimeZone tz = committerIdent.getTimeZone() != null ? committerIdent.getTimeZone() : TimeZone.getDefault();
            commitInfo.setCommitDate(commitInfo.format(commitDate, tz) + " " + tz.getDisplayName());
        } catch (Exception e) {
            Logger.getLogger(GitLastChanges.class.getName()).warning(String.format("Could not get commit info from revision %s due to following error " + e.getMessage() + (e.getCause() != null ? " - " + e.getCause() : ""), commitId));
        } finally {
            revWalk.dispose();
        }
        return commitInfo;
    }

    @Override
    public List<CommitInfo> getCommitsBetweenRevisions(Repository gitRepository, ObjectId currentRevision, ObjectId previousRevision) {

        List<CommitInfo> commits = new ArrayList<>();
        try {
            Iterable<RevCommit> revCommits = new Git(gitRepository).log().addRange(previousRevision, currentRevision).call();

            for (RevCommit commit : revCommits) {
                if (commit != null) {
                    PersonIdent committerIdent = commit.getCommitterIdent();
                    CommitInfo commitInfo = new CommitInfo();
                    Date commitDate = committerIdent.getWhen();
                    commitInfo.setCommitId(commit.getName())
                            .setCommitMessage(commit.getFullMessage())
                            .setCommitterName(committerIdent.getName())
                            .setCommitterEmail(committerIdent.getEmailAddress());
                    TimeZone tz = committerIdent.getTimeZone() != null ? committerIdent.getTimeZone() : TimeZone.getDefault();
                    commitInfo.setCommitDate(commitInfo.format(commitDate, tz) + " " + tz.getDisplayName());
                    commits.add(commitInfo);
                }
            }
        }catch (Exception e) {
            Logger.getLogger(GitLastChanges.class.getName()).log(Level.WARNING, String.format("Could not get commits between current revision %s and previous revision %s.", currentRevision, previousRevision), e);
        }

        return commits;
    }


}
