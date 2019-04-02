package com.github.jenkins.lastchanges;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;

public class LastChangesUtil {
	
    private static Logger LOG = Logger.getLogger(LastChangesPublisher.class.getName());
	
	public static String toHtmlDiff(String escapedDiff, String buildName) {
		final StringWriter writer = new StringWriter();
        try (InputStream is = LastChangesUtil.class.getResourceAsStream("/htmlDiffTemplate")){
			IOUtils.copy(is, writer, Charset.forName("UTF-8"));
        } catch (IOException e) {
        	LOG.log(Level.SEVERE, "Could not generate html diff",e);
        }
        String htmlTemplate = writer.toString();
        String htmlDiff = htmlTemplate.replace("[TITLE]", "Diff of build " + buildName)
        		.replace("[DIFF]", escapedDiff);
        return htmlDiff;
	}

}
