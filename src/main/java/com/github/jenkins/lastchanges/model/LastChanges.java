package com.github.jenkins.lastchanges.model;

import com.github.jenkins.lastchanges.api.CommitInfo;

/**
 * Created by rafael-pestano on 29/06/2016.
 */
public class LastChanges {

    private CommitInfo commitInfo; //information aboud head commit
    private String changes; //diff between head and ´head -1'

    public LastChanges(CommitInfo commitInfo, String changes) {
        this.commitInfo = commitInfo;
        this.changes = changes;
    }

    public CommitInfo getCommitInfo() {
        return commitInfo;
    }

    public String getChanges() {
        return changes;
    }
}
