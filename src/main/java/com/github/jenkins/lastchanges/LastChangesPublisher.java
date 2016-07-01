/*
 * The MIT License
 *
 * Copyright 2016 rmpestano.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.jenkins.lastchanges;

import static com.github.jenkins.lastchanges.LastChanges.repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import com.github.jenkins.lastchanges.model.FormatType;
import com.github.jenkins.lastchanges.model.LastChangesConfig;
import com.github.jenkins.lastchanges.model.MatchingType;

import hudson.CopyOnWrite;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

/**
 * @author rmpestano
 */
public class LastChangesPublisher extends Recorder implements SimpleBuildStep {

	private LastChangesProjectAction   lastChangesProjectAction;

	private static final String		   GIT_DIR	  = "/.git";

	@Extension
	public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

	@DataBoundConstructor
	public LastChangesPublisher() {
	}

	@Override
	public BuildStepDescriptor<hudson.tasks.Publisher> getDescriptor() {
		return DESCRIPTOR;
	}

	@Override
	public Action getProjectAction(AbstractProject<?, ?> project) {
		if (lastChangesProjectAction == null) {
			lastChangesProjectAction = new LastChangesProjectAction(project);
		}
		return lastChangesProjectAction;
	}

	@Override
	public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {

		FilePath workspaceTargetDir = getMasterWorkspaceDir(build);// here we're are going to generate pretty/rich diff html from diff file (always on master)

		File gitRepoSourceDir = new File(workspace.getRemote() + GIT_DIR);//sometimes on slave

		File gitRepoTargetDir = new File(workspaceTargetDir.getRemote());//always on master

		try {
			//workspace can be on slave so copy git resources to master
			FileUtils.copyDirectoryToDirectory(gitRepoSourceDir, gitRepoTargetDir);
			//workspace.copyRecursiveTo("**/*", workspaceTargetDir);//not helps because it can't copy .git dir

			LastChanges lastChanges = LastChanges.of(repository(gitRepoTargetDir.getPath() + GIT_DIR));
			listener.hyperlink("../" + build.getNumber() + "/" + LastChangesBaseAction.BASE_URL, "Last changes generated successfully!");
			listener.getLogger().println("");
			build.addAction(new LastChangesBuildAction(build, lastChanges, DESCRIPTOR.buildConfig()));
		} catch (Exception e) {
			listener.error("Last Changes NOT published due to the following error: " + e.getMessage());
		}
		//always success (only warn when no diff was generated)

		build.setResult(Result.SUCCESS);

	}

	/**
	 * mainly for findbugs be happy
	 *
	 * @param build
	 * @return
	 */
	private FilePath getMasterWorkspaceDir(Run<?, ?> build) {
		if (build != null && build.getRootDir() != null) {
			return new FilePath(build.getRootDir());
		} else {
			return new FilePath(Paths.get("").toFile());
		}
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
		
		@CopyOnWrite
		private volatile FormatType	format;
		
		@CopyOnWrite
		private volatile MatchingType	matching;

		public DescriptorImpl(Class<? extends Publisher> clazz) {
			super(clazz);
			load();
		}

		public DescriptorImpl() {
			this(LastChangesPublisher.class);
		}

		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			// Indicates that this builder can be used with all kinds of project types
			return true;
		} 

		/**
		 * This human readable name is used in the configuration screen.
		 */
		public String getDisplayName() {
			return "Publish Last Changes";
		}

		public ListBoxModel doFillFormatItems() {
			ListBoxModel items = new ListBoxModel();
			for (FormatType formatType : FormatType.values()) {
				if(formatType.equals(format)){
					items.add(new ListBoxModel.Option(formatType.getFormat(), formatType.name(), true));
				} else{
					items.add(formatType.getFormat(), formatType.name());
				}
			}
			return items;
		}
		
		public ListBoxModel doFillMatchingItems() {
			ListBoxModel items = new ListBoxModel();
			for (MatchingType matchingType : MatchingType.values()) {
				if(matchingType.equals(matching)){
					items.add(new ListBoxModel.Option(matchingType.getMatching(), matchingType.name(), true));
				} else{
					items.add(matchingType.getMatching(), matchingType.name());
				}
			}
			return items;
		}


		 @Override
		public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
			JSONObject lastChangesConfig = json.getJSONObject("last-changes");
			if(lastChangesConfig != null){
				String formatConfig = lastChangesConfig.getString("format");
				if(formatConfig != null){
					this.format = FormatType.valueOf(formatConfig);
				}
				
				String matchingConfig = lastChangesConfig.getString("matching");
				if(matchingConfig != null){
					this.matching = MatchingType.valueOf(matchingConfig);
				}
			}
		    save();
		    return true;
		} 
		 
		 public LastChangesConfig buildConfig() {
			 return new LastChangesConfig(format, matching);
			
		}

	}

}
