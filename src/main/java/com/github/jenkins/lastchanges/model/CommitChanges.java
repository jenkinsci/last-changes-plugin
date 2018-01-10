package com.github.jenkins.lastchanges.model;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

import java.io.Serializable;

/**
 *
 * This class gathers the commit information along with the changes related to its previous commit.
 */
public class CommitChanges implements Serializable {

    private CommitInfo commitInfo;
    private String changes;

    public CommitChanges(CommitInfo commitInfo, String changes) {
        this.commitInfo = commitInfo;
        this.changes = changes;
    }

    @Whitelisted
    public CommitInfo getCommitInfo() {
        return commitInfo;
    }

    @Whitelisted	
    public String getChanges() {
        return changes;
    }

    public String getEscapedDiff() {
        if (changes != null) {
            return StringEscapeUtils.escapeEcmaScript(changes);
        } else {
            return "";
        }
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof CommitChanges == false) {
            return false;
        }
        CommitChanges other = (CommitChanges) obj;
        if(commitInfo == null || other.commitInfo == null) {
            return false;
        }
        return commitInfo.equals(other.commitInfo);
    }
}
