package com.github.jenkins.lastchanges;

import hudson.FilePath;
import hudson.model.Action;
import hudson.model.DirectoryBrowserSupport;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;

public abstract class LastChangesBaseAction implements Action {

    protected static final String BASE_URL = "last-changes";

    public String getUrlName() {
        return BASE_URL;
    }

    public String getDisplayName() {
        return "View Last Changes";
    }

    public String getIconFileName() {
        return "/plugin/last-changes/git.png";
    }

    public void doDynamic(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        //System.setProperty("hudson.model.DirectoryBrowserSupport.CSP", "sandbox allow-same-origin allow-scripts; script-src 'self' 'unsafe-inline'; default-src 'self'; img-src 'self'; style-src 'self';");
        DirectoryBrowserSupport dbs = new DirectoryBrowserSupport(this, new FilePath(dir()), getTitle(), getUrlName(),
                false);

        dbs.generateResponse(req, rsp, this);
    }

    protected abstract String getTitle();

    protected abstract File dir();

}
