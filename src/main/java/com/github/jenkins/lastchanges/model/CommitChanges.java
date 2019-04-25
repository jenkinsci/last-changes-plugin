package com.github.jenkins.lastchanges.model;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

import com.github.jenkins.lastchanges.LastChangesUtil;

import java.io.Serializable;

/**
 *
 * This class gathers the commit information along with the changes related to its previous commit.
 */
public class CommitChanges implements Serializable {

    private CommitInfo commitInfo;
    private String changes;
    private byte[] compressedChanges;

    public CommitChanges(CommitInfo commitInfo, String changes) {
        this.commitInfo = commitInfo;
        if (LastChangesUtil.shouldCompressDiff(changes)) {
            this.changes = null;
            this.compressedChanges = LastChangesUtil.compress(changes);
        } else {
            this.changes = changes;
            this.compressedChanges = null;
        }
    }

    @Whitelisted
    public CommitInfo getCommitInfo() {
        return commitInfo;
    }

	@Whitelisted
	public String getChanges() {
		if (changes == null) {
			return LastChangesUtil.decompress(compressedChanges);
		} else {
			return changes;
		}
	}

    public String getEscapedDiff() {
    	String changes = getChanges();
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
