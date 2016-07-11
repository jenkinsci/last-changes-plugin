package com.github.jenkins.lastchanges.api;

import com.github.jenkins.lastchanges.model.LastChanges;

/**
 * Created by rmpestano on 7/10/16.
 */
public interface VCSChanges<REPOSITORY, REVISION> {


    LastChanges lastChangesOf(REPOSITORY repository);

    LastChanges lastChangesOf(REPOSITORY repository, REVISION currentRevision, REVISION previousRevision);
}
