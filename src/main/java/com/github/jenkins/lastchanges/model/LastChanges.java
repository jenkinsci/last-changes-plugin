package com.github.jenkins.lastchanges.model;

import org.apache.commons.lang3.StringEscapeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents the changes between two trees (in git) and two revisions (in svn).
 *
 * currentRevision gathers the information from latest commit on the newest tree (in git) and most recent revision (in svn)
 *
 * previousRevision has the information of the most recent commit in previous tree (in git) and most recent commit (in svn)
 *
 * diff is the differences between those trees (in git) or revisions (in svn)
 *
 * commits is the list of commits between those revisions/trees. Each commit has the commit information (as in current/previsions revisions)
 * as well as the diff compared to it's previous revision.
 *
 * Created by rmpestano on 7/3/16.
 */
public class LastChanges {

    private CommitInfo currentRevision; //information about head commit
    private CommitInfo previousRevision;
    private String diff;
    private List<CommitChanges> commits ;//commits between current and previous revisions along with their changes related to its previous commit
    private String lastCommitId; //id of last commit (note that current revisions points to tree in git)

    public LastChanges(CommitInfo current, CommitInfo previous, String diff) {
        this.currentRevision = current;
        this.previousRevision = previous;
        this.diff = diff;
        commits = new ArrayList<>();
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
            return StringEscapeUtils.escapeEcmaScript(diff);
        } else {
            return "";
        }
    }

    public void addCommits(List<CommitChanges> commitChanges) {
        commits.addAll(commitChanges);
    }

    public void addCommit(CommitChanges commitchange) {
        commits.add(commitchange);
    }

    public List<CommitChanges> getCommits() {
        return commits;
    }

    public String getLastCommitId() {
        return lastCommitId;
    }

    public void setLastCommitId(String lastCommit) {
        this.lastCommitId = lastCommit;
    }

    public Integer getNumCommits() {
        return commits == null ? 0 : commits.size();
    }
}
