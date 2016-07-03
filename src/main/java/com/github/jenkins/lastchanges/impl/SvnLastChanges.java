/**
 * Created by rmpestano on 6/5/16.
 */
package com.github.jenkins.lastchanges.impl;

import com.github.jenkins.lastchanges.exception.RepositoryNotFoundException;
import com.github.jenkins.lastchanges.model.CommitInfo;
import com.github.jenkins.lastchanges.model.LastChanges;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.io.SVNRepository;

import java.io.File;


public class SvnLastChanges {

    private LastChanges lastChanges;

    public SvnLastChanges(CommitInfo commitInfo, String changes) {
        lastChanges = new LastChanges(commitInfo, changes);
    }


    /**
     * @param path local svn repository path
     * @return underlying svn repository from location path
     */
    public static SVNRepository repository(String path) {
        if (path == null || path.isEmpty()) {
            throw new RepositoryNotFoundException("Svn repository path cannot be empty.");
        }

        File filePath = new File(path);

        if (!filePath.exists()) {
            throw new RepositoryNotFoundException("Svn repository does no exists at location " + path);
        }

        try {
            //SVNRepository svnRepository = SVNRepositoryFactory.create( SVNURL.parseURIDecoded( "https://subversion.assembla.com/svn/cucumber-json-files/trunk" ) );
            // ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager("rmpestano", "bigboss666");
            //svnRepository.setAuthenticationManager( authManager );
            //SVNRepositoryFactoryImpl.setup();
            FSRepositoryFactory.setup();
            SVNRepository svnRepository = FSRepositoryFactory.create(SVNURL.fromFile(filePath));
            //SVNRepository svnRepository = SVNRepositoryFactory.create(SVNURL.fromFile(filePath));
            svnRepository.testConnection();
            return svnRepository;
        } catch (Exception e) {
            throw new RepositoryNotFoundException("Could not find svn repository at " + path, e);

        }


    }


    /**
     * Creates an object containing commit info and git diff from last two commits on repository
     *
     * @param repository svn repository to get last changes
     * @return LastChanges
     */
    public static LastChanges of(SVNRepository repository) {
        return new LastChanges(null, "");
    }


    public LastChanges getLastChanges() {
        return lastChanges;
    }
}
