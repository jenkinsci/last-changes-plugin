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
        boolean hasPreviousRevision = buildChanges.getPreviousRevision() != null;
        String htmlDiff = htmlTemplate.replace("[TITLE]", "Changes of build " + buildName)
                .replace("[PREV_REVISION]", hasPreviousRevision ? buildChanges.getPreviousRevision().getCommitId() : "")
                .replace("[PREV_AUTHOR]", (hasPreviousRevision && buildChanges.getPreviousRevision().getCommitterName() != null) ? buildChanges.getPreviousRevision().getCommitterName() : "" )
                .replace("[PREV_EMAIL]", (hasPreviousRevision && buildChanges.getPreviousRevision().getCommitterEmail() != null) ? buildChanges.getPreviousRevision().getCommitterEmail() : "" )
                .replace("[PREV_DATE]", (hasPreviousRevision && buildChanges.getPreviousRevision().getCommitDate() != null) ? buildChanges.getPreviousRevision().getCommitDate() : "" )
                .replace("[PREV_MESSAGE]", (hasPreviousRevision && buildChanges.getPreviousRevision().getCommitterName() != null) ? buildChanges.getPreviousRevision().getCommitMessage() : "" )
                .replace("[CURRENT_REVISION]", buildChanges.getCurrentRevision().getCommitId())
                .replace("[CURRENT_AUTHOR]", buildChanges.getCurrentRevision().getCommitterName() != null ? buildChanges.getCurrentRevision().getCommitterName() : "" )
                .replace("[CURRENT_EMAIL]", buildChanges.getCurrentRevision().getCommitterEmail() != null ? buildChanges.getCurrentRevision().getCommitterEmail() : "" )
                .replace("[CURRENT_DATE]", buildChanges.getCurrentRevision().getCommitDate() != null ? buildChanges.getCurrentRevision().getCommitDate() : "" )
                .replace("[CURRENT_MESSAGE]", buildChanges.getCurrentRevision().getCommitMessage() != null ? buildChanges.getCurrentRevision().getCommitMessage() : "" )
        		.replace("[DIFF]", buildChanges.getEscapedDiff());
        return htmlDiff;
	}

}
