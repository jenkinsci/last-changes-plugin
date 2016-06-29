package com.github.jenkins.lastchanges.model;

import com.github.jenkins.lastchanges.api.CommitInfo;
import org.apache.commons.lang3.StringEscapeUtils;

/**
 * Created by rafael-pestano on 29/06/2016.
 */
public class LastChanges {

    private CommitInfo commitInfo; //information aboud head commit
    private String diff; //diff between head and ´head -1'

    public LastChanges(CommitInfo commitInfo, String changes) {
        this.commitInfo = commitInfo;
        this.diff = changes;
    }

    public CommitInfo getCommitInfo() {
        return commitInfo;
    }

    public String getDiff() {
        return diff;
    }

    /**
     * separates the diff string into multiple lines
     * required by diff2html tool
     *
     * @return
     */
    public String getEscapedDiff(){
        if(diff != null){
            return StringEscapeUtils.escapeEcmaScript(diff);
        } else{
            return "";
        }
    }
}
