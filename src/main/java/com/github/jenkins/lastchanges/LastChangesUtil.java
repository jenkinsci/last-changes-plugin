package com.github.jenkins.lastchanges;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;

import com.github.jenkins.lastchanges.model.LastChanges;

public class LastChangesUtil {
	
    private static Logger LOG = Logger.getLogger(LastChangesPublisher.class.getName());
	
	public static String toHtmlDiff(LastChanges buildChanges, String buildName) {
		final StringWriter writer = new StringWriter();
        try (InputStream is = LastChangesUtil.class.getResourceAsStream("/htmlDiffTemplate")){
			IOUtils.copy(is, writer, Charset.forName("UTF-8"));
        } catch (IOException e) {
        	LOG.log(Level.SEVERE, "Could not generate html diff",e);
        }
        String htmlTemplate = writer.toString();
        String htmlDiff = htmlTemplate.replace("[TITLE]", "Changes of build " + buildName)
                .replace("[PREV_REVISION]", buildChanges.getPreviousRevision() != null ? buildChanges.getPreviousRevision().getCommitId() : "")
                .replace("[PREV_AUTHOR]", buildChanges.getPreviousRevision() != null ? buildChanges.getPreviousRevision().getCommitterName() : "" )
                .replace("[PREV_EMAIL]", buildChanges.getPreviousRevision() != null ? buildChanges.getPreviousRevision().getCommitterEmail() : "" )
                .replace("[PREV_DATE]", buildChanges.getPreviousRevision() != null ? buildChanges.getPreviousRevision().getCommitDate() : "" )
                .replace("[PREV_MESSAGE]", buildChanges.getPreviousRevision() != null ? buildChanges.getPreviousRevision().getCommitMessage() : "" )
                .replace("[CURRENT_REVISION]", buildChanges.getCurrentRevision() != null ? buildChanges.getCurrentRevision().getCommitId() : "")
                .replace("[CURRENT_AUTHOR]", buildChanges.getCurrentRevision() != null ? buildChanges.getCurrentRevision().getCommitterName() : "" )
                .replace("[CURRENT_EMAIL]", buildChanges.getCurrentRevision() != null ? buildChanges.getCurrentRevision().getCommitterEmail() : "" )
                .replace("[CURRENT_DATE]", buildChanges.getCurrentRevision() != null ? buildChanges.getCurrentRevision().getCommitDate() : "" )
                .replace("[CURRENT_MESSAGE]", buildChanges.getCurrentRevision() != null ? buildChanges.getCurrentRevision().getCommitMessage() : "" )
        		.replace("[DIFF]", buildChanges.getEscapedDiff());
        return htmlDiff;
	}

}
