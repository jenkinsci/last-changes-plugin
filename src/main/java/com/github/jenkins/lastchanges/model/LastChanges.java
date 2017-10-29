package com.github.jenkins.lastchanges.model;

import org.apache.commons.lang3.StringEscapeUtils;

/**
 * Created by rmpestano on 7/3/16.
 */
public class LastChanges {

    private CommitInfo currentRevision; //information aboud head commit
    private CommitInfo previousRevision;
    private String diff; //diff between head and ´head -1'

    public LastChanges(CommitInfo current, CommitInfo previous, String diff) {
        this.currentRevision = current;
        this.previousRevision = previous;
        this.diff = diff;
    }

    public CommitInfo getCurrentRevision() {
        return currentRevision;
    }

    public CommitInfo getPreviousRevision() {
        return previousRevision;
    }

    public String getDiff() {
        return diff;
    }

    public String getEscapedDiff() {
        if (diff != null) {
            return StringEscapeUtils.escapeEcmaScript(getDiff());
        } else {
            return "";
        }
    }
}
