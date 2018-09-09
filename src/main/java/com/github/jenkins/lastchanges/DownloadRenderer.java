package com.github.jenkins.lastchanges;

import com.github.jenkins.lastchanges.model.LastChanges;
import java.io.ByteArrayInputStream;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

public class DownloadRenderer implements Serializable {

    private final String buildName;
    private final LastChanges buildChanges;

    public DownloadRenderer(LastChanges buildChanges, String buildName) {
        this.buildChanges = buildChanges;
        this.buildName = buildName;
    }

    /**
     *
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException
     */
    public void doIndex(StaplerRequest request, StaplerResponse response)
            throws IOException, ServletException {
        String fileName = buildName + ".diff";
        try (final InputStream is = new ByteArrayInputStream(buildChanges.getDiff().getBytes())) {
            response.addHeader("Content-Disposition", "attachment; filename=" + fileName);
            response.serveFile(request, is, 0l, 0l, -1l, fileName);
        }
    }

}
