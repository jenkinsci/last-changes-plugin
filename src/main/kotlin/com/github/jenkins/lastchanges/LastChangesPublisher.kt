package com.github.jenkins.lastchanges

import hudson.FilePath
import hudson.Launcher
import hudson.model.Run
import hudson.model.TaskListener
import hudson.tasks.BuildStepMonitor
import hudson.tasks.Recorder
import jenkins.tasks.SimpleBuildStep

/**
 * Created by rafael-pestano on 06/06/2016.
 */

public class LastChangesPublisher : Recorder(), SimpleBuildStep {

    override fun getRequiredMonitorService(): BuildStepMonitor? {
        throw UnsupportedOperationException()
    }

    override fun perform(run: Run<*, *>, workspace: FilePath, launcher: Launcher, listener: TaskListener) {
        throw UnsupportedOperationException()
    }

}