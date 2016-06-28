package com.github.jenkins.lastchanges.exception;

/**
 * Created by rafael-pestano on 27/06/2016.
 */
public class GitTreeNotFoundException extends LastChangesException {

    public GitTreeNotFoundException() {
    }

    public GitTreeNotFoundException(String message) {
        super(message);
    }

    public GitTreeNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
