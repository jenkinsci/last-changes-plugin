package com.github.jenkins.lastchanges;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;

import com.github.jenkins.lastchanges.model.LastChanges;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class LastChangesUtil implements Serializable {

    private static final Logger LOG = Logger.getLogger(LastChangesPublisher.class.getName());

    private static final int DIFF_COMPRESS_THRESHOLD = Integer.parseInt(System.getProperty("lastchanges.diff.compress-threshold", "250"));

    public static String toHtmlDiff(LastChanges buildChanges, String buildName) {
        final StringWriter writer = new StringWriter();
        try (InputStream is = LastChangesUtil.class.getResourceAsStream("/htmlDiffTemplate")) {
            IOUtils.copy(is, writer, Charset.forName("UTF-8"));
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Could not generate html diff", e);
        }
        String htmlTemplate = writer.toString();
        boolean hasPreviousRevision = buildChanges.getPreviousRevision() != null;
        String htmlDiff = htmlTemplate.replace("[TITLE]", "Changes of build " + buildName)
            .replace("[PREV_REVISION]", hasPreviousRevision ? buildChanges.getPreviousRevision().getCommitId() : "")
            .replace("[PREV_AUTHOR]", (hasPreviousRevision && buildChanges.getPreviousRevision().getCommitterName() != null) ? buildChanges.getPreviousRevision().getCommitterName() : "")
            .replace("[PREV_EMAIL]", (hasPreviousRevision && buildChanges.getPreviousRevision().getCommitterEmail() != null) ? buildChanges.getPreviousRevision().getCommitterEmail() : "")
            .replace("[PREV_DATE]", (hasPreviousRevision && buildChanges.getPreviousRevision().getCommitDate() != null) ? buildChanges.getPreviousRevision().getCommitDate() : "")
            .replace("[PREV_MESSAGE]", (hasPreviousRevision && buildChanges.getPreviousRevision().getCommitterName() != null) ? buildChanges.getPreviousRevision().getCommitMessage() : "")
            .replace("[CURRENT_REVISION]", buildChanges.getCurrentRevision().getCommitId())
            .replace("[CURRENT_AUTHOR]", buildChanges.getCurrentRevision().getCommitterName() != null ? buildChanges.getCurrentRevision().getCommitterName() : "")
            .replace("[CURRENT_EMAIL]", buildChanges.getCurrentRevision().getCommitterEmail() != null ? buildChanges.getCurrentRevision().getCommitterEmail() : "")
            .replace("[CURRENT_DATE]", buildChanges.getCurrentRevision().getCommitDate() != null ? buildChanges.getCurrentRevision().getCommitDate() : "")
            .replace("[CURRENT_MESSAGE]", buildChanges.getCurrentRevision().getCommitMessage() != null ? buildChanges.getCurrentRevision().getCommitMessage() : "")
            .replace("[DIFF]", buildChanges.getEscapedDiff());
        return htmlDiff;
    }

    /**
     * @param diff
     * @return <code>true</code> If diff is bigger than DIFF_COMPRESS_THRESHOLD
     */
    public static boolean shouldCompressDiff(String diff) {
        return diff != null && diff.length() > 0 && ((diff.getBytes().length / 1024) > DIFF_COMPRESS_THRESHOLD);
    }

    public static byte[] compress(String uncompressedDiff) {
        GZIPOutputStream gzip = null;
        try (ByteArrayOutputStream baos =  new ByteArrayOutputStream()) {
            LOG.log(Level.INFO, "Compressing diff...");
            gzip = new GZIPOutputStream(baos);
            gzip.write(uncompressedDiff.getBytes(UTF_8));
            gzip.flush();
            gzip.close();
            LOG.log(Level.INFO, "Diff compressed.");
            return baos.toByteArray();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Could not compress diff.", e);
            return uncompressedDiff.getBytes(UTF_8);
        } 
    }

    public static String decompress(byte[] compressedDiff) {
        if (compressedDiff == null || compressedDiff.length == 0) {
            return "";
        }
        LOG.log(Level.INFO, "Decompressing diff...");
        StringBuilder outStr = new StringBuilder();
        try (final GZIPInputStream gzipInput = new GZIPInputStream(new ByteArrayInputStream(compressedDiff))) {
            BufferedReader bf = new BufferedReader(new InputStreamReader(gzipInput, "UTF-8"));
            String line;
            while ((line = bf.readLine()) != null) {
                outStr.append(line+"\n");
            }
            LOG.log(Level.INFO, "Diff decompressed.");
            return outStr.toString();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Could not decompress diff.", e);
            return "";
        }

    }
//    public static String decompress(byte[] compressedDiff) {
//        if (compressedDiff == null || compressedDiff.length == 0) {
//            return "";
//        }
//        LOG.log(Level.INFO, "Decompressing diff...");
//        try (final ByteArrayInputStream in = new ByteArrayInputStream(compressedDiff); final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
//            GzipCompressorOutputStream gzOut = new GzipCompressorOutputStream(out);
//            final byte[] buffer = new byte[2048];
//            int n = 0;
//            while (-1 != (n = in.read(buffer))) {
//                gzOut.write(buffer, 0, n);
//            }
//            return out.toString("UTF-8");
//        } catch (Exception e) {
//            LOG.log(Level.WARNING, "Could not decompress diff.", e);
//            return "";
//        }
//
//    }
}
