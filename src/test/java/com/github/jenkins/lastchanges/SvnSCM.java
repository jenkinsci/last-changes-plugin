package com.github.jenkins.lastchanges;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.scm.SubversionSCM;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Created by rafael-pestano on 28/06/2016.
 */
public class SvnSCM extends SubversionSCM {

    private final File sourceDir;
    private final String targetWorkspaceDirName;

    public SvnSCM(String targetWorkspaceDir, File sourceDir, List<ModuleLocation> locations) {
        super(locations, null, null, null, null, null, null, null, false, false, null, false);
        this.targetWorkspaceDirName = targetWorkspaceDir;
        this.sourceDir = sourceDir;
    }

    @Override
    public boolean checkout(AbstractBuild build, Launcher launcher, FilePath workspace, BuildListener listener, File changelogFile) throws IOException, InterruptedException {
        File workspaceDir = new File(workspace.toURI().getPath());
        FileUtils.copyDirectoryToDirectory(sourceDir, workspaceDir);
        if (targetWorkspaceDirName != null && !targetWorkspaceDirName.isEmpty()) {
            //rename dest dir
            Path oldName = new File(workspace.toURI().getPath() + "/" + sourceDir.getName()).toPath();
            Files.move(oldName, oldName.resolveSibling(targetWorkspaceDirName));
        }
        return true;
    }
}
