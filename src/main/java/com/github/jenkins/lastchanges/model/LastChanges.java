package com.github.jenkins.lastchanges.model;

/**
 * Created by rmpestano on 7/3/16.
 */
public class LastChanges {

    private CommitInfo commitInfo; //information aboud head commit
    private String diff; //diff between head and ´head -1'

    public LastChanges(CommitInfo commitInfo, String diff) {
        this.commitInfo = commitInfo;
        this.diff = diff;
    }

    public CommitInfo getCommitInfo() {
        return commitInfo;
    }

    public String getDiff() {
        return diff;
    }
}
