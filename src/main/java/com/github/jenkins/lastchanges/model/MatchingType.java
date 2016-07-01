package com.github.jenkins.lastchanges.model;

/**
 * Created by pestano on 20/03/16.
 */
public enum MatchingType {

    NONE("none"), LINE("lines"),WORD("words");

    public final String matching;

    MatchingType(String value) {
        this.matching = value;
    }

    public String getMatching() {
        return matching;
    }
}
