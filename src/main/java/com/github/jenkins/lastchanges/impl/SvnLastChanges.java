/**
 * Created by rmpestano on 6/5/16.
 */
package com.github.jenkins.lastchanges.impl;

import com.github.jenkins.lastchanges.exception.*;
import com.github.jenkins.lastchanges.model.CommitInfo;
import com.github.jenkins.lastchanges.model.LastChanges;
import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.ISVNOptions;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;


public class SvnLastChanges {

   LastChanges lastChanges;

    public SvnLastChanges(CommitInfo commitInfo, String changes) {
        lastChanges = new LastChanges(commitInfo,changes);
    }

    public CommitInfo getCommitInfo() {
        return lastChanges.getCommitInfo();
    }

    public String getDiff() {
        return lastChanges.getDiff();
    }

    public String getEscapedDiff(){
        if(getDiff() != null){
            return StringEscapeUtils.escapeEcmaScript(getDiff());
        } else{
            return "";
        }
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

        if(!filePath.exists()){
            throw new RepositoryNotFoundException("Svn repository does no exists at location "+path);
        }

        try{
            //SVNRepository svnRepository = SVNRepositoryFactory.create( SVNURL.parseURIDecoded( "https://subversion.assembla.com/svn/cucumber-json-files/trunk" ) );
           // ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager("rmpestano", "bigboss666");
            //svnRepository.setAuthenticationManager( authManager );
            //SVNRepositoryFactoryImpl.setup();
            FSRepositoryFactory.setup();
            SVNRepository svnRepository = FSRepositoryFactory.create(SVNURL.fromFile(filePath));
            //SVNRepository svnRepository = SVNRepositoryFactory.create(SVNURL.fromFile(filePath));
            svnRepository.testConnection();
            return svnRepository;
        }catch (Exception e){
           throw new RepositoryNotFoundException("Could not find svn repository at " + path,e);

        }


    }



    /**
     * Creates an object containing commit info and git diff from last two commits on repository
     * @param repository svn repository to get last changes
     * @return  LastChanges
     */
    public static LastChanges of(SVNRepository repository) {
         return new LastChanges(null,"");
    }


}
