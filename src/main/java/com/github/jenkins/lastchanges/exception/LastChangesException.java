package com.github.jenkins.lastchanges.exception;

/**
 * Created by rafael-pestano on 28/06/2016.
 */
public class LastChangesException extends RuntimeException {

    public LastChangesException() {
    }

    public LastChangesException(String message) {
        super(message);
    }

    public LastChangesException(String message, Throwable cause) {
        super(message, cause);
    }
}
