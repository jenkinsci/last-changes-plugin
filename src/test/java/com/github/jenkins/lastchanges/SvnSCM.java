package com.github.jenkins.lastchanges;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.scm.Messages;
import hudson.scm.RepositoryBrowser;
import hudson.scm.SCM;
import hudson.scm.SubversionSCM;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by rafael-pestano on 28/06/2016.
 */
public class SvnSCM extends SubversionSCM {

    private File sourceDir;
    private static String targetWorkspaceDirName;

    public SvnSCM(String targetWorkspaceDir, File sourceDir, List<ModuleLocation> locations) {
    	super(locations, null, null, null, null, null, null, null, false, false,null);
        this.targetWorkspaceDirName = targetWorkspaceDir;
        this.sourceDir = sourceDir;
    }

    @Override
    public boolean checkout(AbstractBuild build, Launcher launcher, FilePath workspace, BuildListener listener, File changelogFile) throws IOException, InterruptedException {
    	File workspaceDir = new File(workspace.toURI().getPath());
        FileUtils.copyDirectoryToDirectory(sourceDir, workspaceDir);
        if(targetWorkspaceDirName != null && !"".equals(targetWorkspaceDirName)){
            //rename dest dir
            Path oldName = new File(workspace.toURI().getPath()+"/"+sourceDir.getName()).toPath();
            Files.move(oldName, oldName.resolveSibling(targetWorkspaceDirName));
        }
    	return true;
    }
    
    
    @Override
    public hudson.scm.SubversionSCM.DescriptorImpl getDescriptor() {
    	// TODO Auto-generated method stub
    	return new DescriptorImpl();
    }
    
    @Extension(ordinal = Integer.MAX_VALUE)
    public static class DescriptorImpl extends hudson.scm.SubversionSCM.DescriptorImpl {

        public DescriptorImpl() {
            super(SvnSCM.class, RepositoryBrowser.class);
        }

        @Override public String getDisplayName() {
            return Messages.NullSCM_DisplayName();
        }

        @Override
        public SCM newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return new SvnSCM(targetWorkspaceDirName,null, new ArrayList<ModuleLocation>());
        }
    }


}
