package com.github.jenkins.lastchanges.api;

import com.github.jenkins.lastchanges.model.LastChanges;

/**
 * Created by rmpestano on 7/10/16.
 */
public interface VCSChanges<REPOSITORY, REVISION> {


    LastChanges changesOf(REPOSITORY repository);

    LastChanges changesOf(REPOSITORY repository, REVISION currentRevision, REVISION previousRevision);

    REVISION getLastTagRevision(REPOSITORY repository);
}
