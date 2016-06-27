/**
 * Created by rmpestano on 6/5/16.
 */
package com.github.jenkins.lastchanges;

import com.github.jenkins.lastchanges.api.CommitInfo;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import com.github.jenkins.lastchanges.exception.*;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;


public class LastChanges {

    /**
     * @param path local git repository path
     * @return underlying git repository from location path
     */
    public static Repository repository(String path) {
        if (path == null || path.isEmpty()) {
            throw new RuntimeException("Path cannot be empty.");
        }

        File repositoryPath = new File(path);

        if (!repositoryPath.exists()) {
            throw new RuntimeException(String.format("Repo path not found at location %s.", repositoryPath));
        }

        Repository repository = null;
        try {
            repository = new FileRepository(path);
        } catch (IOException e) {
            throw new RuntimeException("Could not find git repository at " + path);
        }
        if (repository.isBare()) {
            throw new RuntimeException(String.format("No git repository found at %s.", path));
        }

        return repository;

    }


    /**
     * Writes git diff from latest changes (last two commits) to out
     *
     * @param repository git repository to get last changes
     * @param target     output stream to write changes
     */
    public static void lastChanges(Repository repository, OutputStream target) {
        Git git = new Git(repository);
        try {
            DiffFormatter formatter = new DiffFormatter(target);
            formatter.setRepository(repository);
            ObjectId head = null;
            try {
                head = repository.resolve("HEAD^{tree}");
            } catch (IOException e) {
                throw new GitTreeNotFoundException("Could not resolve repository head.", e);
            }
            try {
                CommitInfo lastCommitInfo = CommitInfo.Builder.buildCommitInfo(repository, head);
                if (lastCommitInfo != null) {
                    target.write(lastCommitInfo.toString().getBytes("UTF-8"));
                }
            } catch (Exception e) {
                throw new CommitInfoException("Could not get commit information", e);
            }
            ObjectId previousHead = null;
            try {
                previousHead = repository.resolve("HEAD~^{tree}");
                if (previousHead == null) {
                    throw new GitTreeNotFoundException("Could not find previous repository head. Its your first commit?");
                }
            } catch (IOException e) {
                throw new GitTreeNotFoundException("Could not resolve previous repository head.", e);
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
                throw new GitDiffException("Could not get repository changes.", e);
            }
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
