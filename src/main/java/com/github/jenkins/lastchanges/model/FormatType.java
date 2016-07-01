package com.github.jenkins.lastchanges.model;

/**
 * Created by pestano on 20/03/16.
 */
public enum FormatType {

    LINE("line-by-line"),SIDE("side-by-side");

    public final String format;

    FormatType(String value) {
        this.format = value;
    }

    public String getFormat() {
        return format;
    }
}
