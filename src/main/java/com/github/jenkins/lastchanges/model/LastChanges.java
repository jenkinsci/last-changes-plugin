package com.github.jenkins.lastchanges.model;

import com.github.jenkins.lastchanges.LastChangesUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

/**
 * This class represents the changes between two trees (in git) and two revisions (in svn).
 *
 * currentRevision gathers the information from latest commit on the newest tree (in git) and most recent revision (in
 * svn)
 *
 * previousRevision has the information of the most recent commit in previous tree (in git) and most recent commit (in
 * svn)
 *
 * diff is the differences between those trees (in git) or revisions (in svn)
 *
 * commits is the list of commits between those revisions/trees. Each commit has the commit information (as in
 * current/previous revisions) as well as the diff compared to it's previous revision.
 *
 * Created by rmpestano on 7/3/16.
 */
public class LastChanges implements Serializable {

    private final CommitInfo currentRevision; //information about head commit
    private final CommitInfo previousRevision;
    private final String diff;
    private final byte[] compressedDiff;
    private final List<CommitChanges> commits;//commits between current and previous revisions along with their changes related to its previous commit

    public LastChanges(CommitInfo current, CommitInfo previous, String diff) {
        this.currentRevision = current;
        this.previousRevision = previous;
        if (LastChangesUtil.shouldCompressDiff(diff)) {
            this.diff = null;
            compressedDiff = LastChangesUtil.compress(diff);
        } else {
            this.diff = diff;
            compressedDiff = null;
        }
        commits = new ArrayList<>();
    }

    @Whitelisted
    public CommitInfo getCurrentRevision() {
        return currentRevision;
    }

    @Whitelisted
    public CommitInfo getPreviousRevision() {
        return previousRevision;
    }

    @Whitelisted
    public String getDiff() {
        if(diff == null) {
            return LastChangesUtil.decompress(compressedDiff);
        } else {
            return diff;
        }
    }

    @Whitelisted
    public String getEscapedDiff() {
        String diff = getDiff();
        if (diff != null) {
            return StringEscapeUtils.escapeEcmaScript(diff);
        } else {
            return "";
        }
    }

    public void addCommits(List<CommitChanges> commitChanges) {
        if (commitChanges != null) {
            commits.addAll(commitChanges);
        }
    }

    public void addCommit(CommitChanges commitchange) {
        if (commitchange != null) {
            commits.add(commitchange);
        }
    }

    @Whitelisted
    public List<CommitChanges> getCommits() {
        return commits;
    }

    public Integer getNumCommits() {
        return commits == null ? 0 : commits.size();
    }
}
