package com.github.jenkins.lastchanges;

import com.github.jenkins.lastchanges.model.CommitChanges;
import hudson.model.Action;
import org.kohsuke.stapler.ForwardToView;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.io.Serializable;

public class CommitRenderer implements Serializable {

  private final Action action;
  private final CommitChanges commit;

  public CommitRenderer(Action lastChangesBuildAction, CommitChanges commit) {
    action = lastChangesBuildAction;
    this.commit = commit;
  }

  /**
   * This method will be called when there are no remaining URL tokens to
   * process after {@link LastChangesBuildAction} has handled the initial
   * `/commit/commitId` prefix.  It renders the `commit.jelly`
   * template inside of the Jenkins UI.
   *
   * @param request request
   * @param response response
   * @throws IOException ioException
   * @throws ServletException servletException
   */
  public void doIndex(StaplerRequest2 request, StaplerResponse2 response)
    throws IOException, ServletException {
    ForwardToView forward = new ForwardToView(action, "commit.jelly")
      .with("commit", commit);
    forward.generateResponse(request, response, action);
  }

}
