package com.github.jenkins.lastchanges.exception;

/**
 * Created by rafael-pestano on 27/06/2016.
 */
public class GitTreeParseException extends LastChangesException {


    public GitTreeParseException() {
    }

    public GitTreeParseException(String message) {
        super(message);
    }

    public GitTreeParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
