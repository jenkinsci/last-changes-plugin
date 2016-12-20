/**
 * Created by rmpestano on 6/5/16.
 */
package com.github.jenkins.lastchanges.impl;

import com.github.jenkins.lastchanges.api.VCSChanges;
import com.github.jenkins.lastchanges.exception.RepositoryNotFoundException;
import com.github.jenkins.lastchanges.model.CommitInfo;
import com.github.jenkins.lastchanges.model.LastChanges;
import hudson.model.AbstractProject;
import hudson.scm.SubversionSCM;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationProvider;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.internal.wc2.ng.SvnDiffGenerator;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.tmatesoft.svn.core.wc2.SvnDiff;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnTarget;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class SvnLastChanges implements VCSChanges<SVNRepository, Long> {

    private static SvnLastChanges instance;

    public static SvnLastChanges getInstance() {
        if (instance == null) {
            instance = new SvnLastChanges();
        }
        return instance;
    }

    /**
     * @deprecated used for unit test only
     * @param path
     *            local svn repository path
     * @return underlying svn repository from location path
     * 
     * @see SvnLastChanges#repository(SubversionSCM, AbstractProject)
     */
    public static SVNRepository repository(String path) {
        if (path == null || path.isEmpty()) {
            throw new RepositoryNotFoundException("Svn repository path cannot be empty.");
        }

        try {
            SVNRepositoryFactoryImpl.setup();
            SVNRepository svnRepository = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(path));
            /*
             * ISVNAuthenticationManager authManager =
             * SVNWCUtil.createDefaultAuthenticationManager( "anonymous" ,
             * "anonymous"); svnRepository.setAuthenticationManager( authManager
             * );
             */

            // FSRepositoryFactory.setup(); //not working, throws svn: E180001:
            // Unable to open an ra_local session to URL
            // SVNURL svnurl = SVNURL.fromFile(filePath);
            // SVNRepository svnRepository = FSRepositoryFactory.create(svnurl);
            // SVNRepository svnRepository =
            // SVNRepositoryFactory.create(SVNURL.fromFile(filePath));

            return svnRepository;
        } catch (Exception e) {
            throw new RepositoryNotFoundException("Could not find svn repository at " + path, e);

        }

    }

    public static SVNRepository repository(SubversionSCM scm, AbstractProject<?, ?> rootProject) {
        
        String path = null;
        try {
            path = scm.getLocations()[0].getURL();
            ISVNAuthenticationProvider svnAuthProvider;
            try{
                svnAuthProvider = scm.createAuthenticationProvider(rootProject, scm.getLocations()[0]);
            } catch (NoSuchMethodError e) {
                //fallback for versions under 2.x of org.jenkins-ci.plugins:subversion
                svnAuthProvider = scm.getDescriptor().createAuthenticationProvider(rootProject);
            }
            ISVNAuthenticationManager svnAuthManager = SVNWCUtil.createDefaultAuthenticationManager();
            svnAuthManager.setAuthenticationProvider(svnAuthProvider);
            SVNClientManager svnClientManager = SVNClientManager.newInstance(null, svnAuthManager);
            return svnClientManager.createRepository(SVNURL.parseURIEncoded(path), false);
        } catch (Exception e) {
            throw new RepositoryNotFoundException("Could not find svn repository at " + path, e);
        }
    }
    

    /**
     * Creates last changes from repository last two revisions
     *
     * @param repository
     *            svn repository to get last changes
     * @return LastChanges commit info and svn diff
     */
    @Override
    public LastChanges changesOf(SVNRepository repository) {
         try {
            return changesOf(repository, repository.getLatestRevision(), repository.getLatestRevision() - 1);
        } catch (SVNException e) {
            throw new RuntimeException("Could not retrieve lastest revision of svn repository located at " + repository.getLocation().getPath() + " due to following error: "+e.getMessage() + (e.getCause() != null ? " - " + e.getCause() : ""), e);
        }
    }

    /**
     * Creates last changes from two revisions of repository
     *
     * @param repository
     *            svn repository to get last changes
     * @return LastChanges commit info and svn diff
     */
    @Override
    public LastChanges changesOf(SVNRepository repository, Long currentRevision, Long previousRevision) {
    	ByteArrayOutputStream diffStream = null;
        try {
            final SvnDiffGenerator diffGenerator = new SvnDiffGenerator();
            diffGenerator.setBasePath(new File(""));
            diffStream = new ByteArrayOutputStream();
            final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
            svnOperationFactory.setAuthenticationManager(repository.getAuthenticationManager());
            final SvnDiff diff = svnOperationFactory.createDiff();
            diff.setSources(SvnTarget.fromURL(repository.getLocation(), SVNRevision.create(previousRevision)),
                    SvnTarget.fromURL(repository.getLocation(), SVNRevision.create(currentRevision)));
            diff.setDiffGenerator(diffGenerator);
            diff.setOutput(diffStream);
            diff.run();

            CommitInfo commitInfo = CommitInfo.Builder.buildFromSvn(repository,currentRevision);

            return new LastChanges(commitInfo, new String(diffStream.toByteArray(), Charset.forName("UTF-8")));
        } catch (Exception e) {
            throw new RuntimeException("Could not retrieve last changes of svn repository located at " + repository.getLocation().getPath() + " due to following error: "+e.getMessage() + (e.getCause() != null ? " - " + e.getCause() : ""), e);

        }
        finally {
			if(diffStream != null) {
				try {
					diffStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
				
		}
    }

}
