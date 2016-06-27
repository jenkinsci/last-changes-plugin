package com.github.jenkins.lastchanges;

import com.google.common.io.Files;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.util.Collection;

;

public class LastChangesIT {
	
	@Rule
	public JenkinsRule jenkins = new JenkinsRule();


	
	
	
	@Test
	public void shouldGenerateDiffFile() throws Exception{
		//given
		FreeStyleProject project = jenkins.createFreeStyleProject("test");

		String repoPath = LastChangesTest.class.getResource("/git-sample-repo").getFile();

		Collection<File> files = FileUtils.listFiles(
				new File(repoPath),
				new RegexFileFilter("^(.*?)"),
				DirectoryFileFilter.DIRECTORY
		);


		File buildDir = project.getBuildDir();
		File gitDir = new File(buildDir.getAbsolutePath()+"/.git");
		gitDir.mkdirs();
		for (File file : files) {
			Files.copy(file,gitDir);
		}

		LastChangesPublisher publisher = new LastChangesPublisher();
		project.getPublishersList().add(publisher);
		project.save();

		//when
		FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
		
		//then
		jenkins.assertLogContains("Last changes generated successfully!",build);

	}
	
}
