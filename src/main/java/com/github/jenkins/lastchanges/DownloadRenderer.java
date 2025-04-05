package com.github.jenkins.lastchanges;

import com.github.jenkins.lastchanges.model.LastChanges;
import java.io.ByteArrayInputStream;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import static java.nio.charset.StandardCharsets.UTF_8;

public class DownloadRenderer implements Serializable {

    private final String buildName;
    private final LastChanges buildChanges;
    private final boolean isHtml;

    public DownloadRenderer(LastChanges buildChanges, String buildName, boolean isHtml) {
        this.buildChanges = buildChanges;
        this.buildName = buildName;
        this.isHtml = isHtml;
    }

    /**
     *
     * @param request request
     * @param response response
     * @throws IOException ioException
     * @throws ServletException servletException
     */
    public void doIndex(StaplerRequest2 request, StaplerResponse2 response)
            throws IOException, ServletException {
        String fileName = buildName + (isHtml ? ".html" : ".diff");

        InputStream is = null;
        try {
            if (isHtml) {
                String htmlDiff = LastChangesUtil.toHtmlDiff(buildChanges, buildName);
                is = new ByteArrayInputStream(htmlDiff.getBytes(UTF_8));
            } else {
                is = new ByteArrayInputStream(buildChanges.getDiff().getBytes(UTF_8));
            }
                
            response.addHeader("Content-Disposition", "attachment; filename=" + fileName);
            response.serveFile(request, is, 0l, 0l, -1l, fileName);
        } catch (Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Could not download diff for build " + buildName, e);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

}
