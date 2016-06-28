package com.github.jenkins.lastchanges.exception;

/**
 * Created by rafael-pestano on 27/06/2016.
 */
public class RepositoryNotFoundException extends LastChangesException {


    public RepositoryNotFoundException() {
    }

    public RepositoryNotFoundException(String message) {
        super(message);
    }

    public RepositoryNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
