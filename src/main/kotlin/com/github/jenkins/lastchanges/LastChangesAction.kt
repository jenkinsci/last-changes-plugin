package com.github.jenkins.lastchanges

import hudson.model.Action

/**
 * Created by rmpestano on 6/5/16.
 */
class LastChangesAction : Action {

    override fun getUrlName(): String? {
        return "last-changes"
    }

    override fun getIconFileName(): String? {
        return "/plugin/last-changes/git.png"
    }

    override fun getDisplayName(): String? {
        return "Last Changes"
    }



}