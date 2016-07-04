/**
 * Created by rmpestano on 6/5/16.
 */
package com.github.jenkins.lastchanges.impl;

import com.github.jenkins.lastchanges.exception.RepositoryNotFoundException;
import com.github.jenkins.lastchanges.model.CommitInfo;
import com.github.jenkins.lastchanges.model.LastChanges;
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnDiffGenerator;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.*;
import org.tmatesoft.svn.core.wc2.SvnDiff;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnTarget;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Iterator;


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

        try {
            SVNRepositoryFactoryImpl.setup();
            SVNRepository svnRepository = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(path));
            /*ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager( "anonymous" , "anonymous");
            svnRepository.setAuthenticationManager( authManager );*/
            //SVNURL svnurl = SVNURL.fromFile(filePath); //not working
            //SVNRepository svnRepository = FSRepositoryFactory.create(svnurl);
            //SVNRepository svnRepository = SVNRepositoryFactory.create(SVNURL.fromFile(filePath));

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
        try {
            final SvnDiffGenerator diffGenerator = new SvnDiffGenerator();
            diffGenerator.setBasePath(new File(""));
            ByteArrayOutputStream diffStream = new ByteArrayOutputStream();
            final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
            final SvnDiff diff = svnOperationFactory.createDiff();
            diff.setSources(SvnTarget.fromURL(repository.getLocation(), SVNRevision.create(repository.getLatestRevision())), SvnTarget.fromURL(repository.getLocation(), SVNRevision.create(repository.getLatestRevision() - 1)));
            diff.setDiffGenerator(diffGenerator);
            diff.setOutput(diffStream);
            diff.run();

            CommitInfo commitInfo = CommitInfo.Builder.buildFromSvn(repository);

            return new LastChanges(commitInfo, new String(diffStream.toByteArray(), Charset.forName("UTF-8")));
        }catch (Exception e){
            throw new RuntimeException("Could not retrieve last changes of svn repository located at "+repository.getLocation().getPath(),e);

        }
    }


    public LastChanges getLastChanges() {
        return lastChanges;
    }
}
