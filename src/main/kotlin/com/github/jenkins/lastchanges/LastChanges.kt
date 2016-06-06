/**
 * Created by rmpestano on 6/5/16.
 */
package com.github.jenkins.lastchanges;

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Paths

/**
 * @param path local git repository path
 * @return underlying git repository from location path
 */
fun repo(path: String): Repository {
    if (path == null || path.isBlank()) {
        throw RuntimeException("Path cannot be empty.")
    }
    val repoPath = Paths.get(path);
    if (!Files.exists(repoPath)) {
        throw RuntimeException(String.format("Repo path not found at location %s.", repoPath))
    }
    val repo = FileRepository(path)
    if (repo.isBare()) {
        throw RuntimeException(String.format("No git repository found at %s.", path))
    }
    return repo
}

/**
 * Writes git diff from latest changes (last two commits) to out
 *
 * @param repo git repository to get last changes
 * @param out output stream to write changes
 *
 */
fun lastChanges(repo: Repository, out: OutputStream): Unit {
    val git = Git(repo);
    val formatter = DiffFormatter(out);
    formatter.setRepository(repo);
    val head = repo.resolve("HEAD^{tree}")
    val previousHead = repo.resolve("HEAD~^{tree}")
    val reader = repo.newObjectReader()
    // Create the tree iterator for each commit
    val oldTreeIter = CanonicalTreeParser()
    try {
        oldTreeIter.reset(reader, previousHead)
    }catch(e: NullPointerException){
        throw RuntimeException("Previous commit not found.")
    }
    val newTreeIter = CanonicalTreeParser();
    newTreeIter.reset(reader, head);
    for (change in git.diff().setOldTree(oldTreeIter).setNewTree(newTreeIter).call()) {
        formatter.format(change)
    }
}


fun main(args: Array<String>) {
    lastChanges(repo(Paths.get(".git").toAbsolutePath().toString()), FileOutputStream(File("git-diff.txt")))
}