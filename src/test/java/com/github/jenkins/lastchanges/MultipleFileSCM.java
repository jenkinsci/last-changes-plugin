package com.github.jenkins.lastchanges;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.NullSCM;
import hudson.scm.SCMRevisionState;
import org.apache.commons.io.FileUtils;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * Created by rafael-pestano on 28/06/2016.
 */
public class MultipleFileSCM extends NullSCM {

    private final Collection<File> workspaceFiles;
    private String path;

    public MultipleFileSCM(String path, Collection<File> files) {
        this.path = path;
        this.workspaceFiles = files;
    }

    @Override
    public void checkout(Run<?,?> build, @Nonnull Launcher launcher, @Nonnull FilePath workspace, @Nonnull TaskListener listener, @CheckForNull File changelogFile, @CheckForNull SCMRevisionState baseline) throws IOException, InterruptedException {
        if(path == null){
            path = "/";
        }
        if(!path.startsWith("/")){
            path = "/"+path;
        }
        File workspaceDir = new File(workspace.toURI().getPath()+path);
        if(!workspaceDir.exists()){
            workspaceDir.mkdir();
        }
        for (File file : workspaceFiles) {
            if (file.isDirectory()) {
                FileUtils.copyDirectory(file, workspaceDir);
            } else {
                FileUtils.copyFileToDirectory(file, workspaceDir);
            }
        }
    }


}
