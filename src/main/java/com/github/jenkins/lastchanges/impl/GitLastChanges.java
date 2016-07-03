/**
 * Created by rmpestano on 6/5/16.
 */
package com.github.jenkins.lastchanges.impl;

import com.github.jenkins.lastchanges.exception.*;
import com.github.jenkins.lastchanges.model.CommitInfo;

import com.github.jenkins.lastchanges.model.LastChanges;
import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;


public class GitLastChanges {

   LastChanges lastChanges;

    public GitLastChanges(CommitInfo commitInfo, String changes) {
        lastChanges = new LastChanges(commitInfo,changes);
    }

    public CommitInfo getCommitInfo() {
        return lastChanges.getCommitInfo();
    }

    public String getDiff() {
        return lastChanges.getDiff();
    }

    public String getEscapedDiff(){
        if(getDiff() != null){
            return StringEscapeUtils.escapeEcmaScript(getDiff());
        } else{
            return "";
        }
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
     * Creates an object containing commit info and git diff from last two commits on repository
     * @param repository git repository to get last changes
     * @return  LastChangesInfo
     */
    public static LastChanges of(Repository repository) {
        Git git = new Git(repository);
        try {
            ByteArrayOutputStream diffStream = new ByteArrayOutputStream();
            CommitInfo lastCommitInfo;
            String repositoryLocation = repository.getDirectory().getAbsolutePath();
            DiffFormatter formatter = new DiffFormatter(diffStream);
            formatter.setRepository(repository);
            ObjectId head = null;
            try {
                head = repository.resolve("HEAD^{tree}");
            } catch (IOException e) {
                throw new GitTreeNotFoundException("Could not resolve head of repository located at "+repositoryLocation, e);
            }
            try {
                lastCommitInfo = CommitInfo.Builder.buildCommitInfo(repository, head);
            } catch (Exception e) {
                throw new CommitInfoException("Could not get last commit information", e);
            }
            ObjectId previousHead = null;
            try {
                previousHead = repository.resolve("HEAD~^{tree}");
                if (previousHead == null) {
                    throw new GitTreeNotFoundException(String.format("Could not find previous head of repository located at %s. Its your first commit?",repositoryLocation));
                }
            } catch (IOException e) {
                throw new GitTreeNotFoundException("Could not resolve previous head of repository located at "+repositoryLocation, e);
            }
            ObjectReader reader = repository.newObjectReader();
            // Create the tree iterator for each commit
            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            try {
                oldTreeIter.reset(reader, previousHead);
            } catch (Exception e) {
                throw new GitTreeParseException("Could not parse previous commit tree.", e);
            }
            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            try {
                newTreeIter.reset(reader, head);
            } catch (IOException e) {
                throw new GitTreeParseException("Could not parse current commit tree.", e);
            }
            try {
                for (DiffEntry change : git.diff().setOldTree(oldTreeIter).setNewTree(newTreeIter).call()) {
                    formatter.format(change);
                }
            } catch (Exception e) {
                throw new GitDiffException("Could not get last changes of repository located at "+repositoryLocation, e);
            }

            return new LastChanges(lastCommitInfo,new String(diffStream.toByteArray(), Charset.forName("UTF-8")));
        } finally {
            if (git != null) {
                git.close();
            }
            if (repository != null) {
                repository.close();
            }
        }

    }


}
