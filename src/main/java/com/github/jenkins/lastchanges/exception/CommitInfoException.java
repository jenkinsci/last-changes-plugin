package com.github.jenkins.lastchanges.exception;

/**
 * Created by rafael-pestano on 27/06/2016.
 */
public class CommitInfoException extends LastChangesException {

    public CommitInfoException() {
    }

    public CommitInfoException(String message) {
        super(message);
    }

    public CommitInfoException(String message, Throwable cause) {
        super(message, cause);
    }
}
