package com.github.jenkins.lastchanges;

import java.io.*;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.jenkins.lastchanges.impl.GitLastChanges;
import com.github.jenkins.lastchanges.impl.SvnLastChanges;
import com.github.jenkins.lastchanges.model.CommitChanges;
import com.github.jenkins.lastchanges.model.CommitInfo;
import org.apache.commons.io.IOUtils;

import com.github.jenkins.lastchanges.model.LastChanges;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationProvider;
import org.tmatesoft.svn.core.wc.SVNRevision;

import static com.github.jenkins.lastchanges.impl.GitLastChanges.repository;
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

    /**
     * Retrieve commits between two revisions
     *
     * @param currentRevision
     * @param previousRevision
     */
    public static List<CommitInfo> getCommitsBetweenRevisions(Repository gitRepository, boolean isGit, String currentRevision, String previousRevision, File svnRepository,  ISVNAuthenticationProvider svnAuthProvider) throws IOException {

        List<CommitInfo> commits = new ArrayList<>();
        if (isGit) {
            commits = GitLastChanges.getInstance().getCommitsBetweenRevisions(gitRepository, gitRepository.resolve(currentRevision),
                    gitRepository.resolve(previousRevision));
        } else {
            commits = SvnLastChanges.getInstance(svnAuthProvider).getCommitsBetweenRevisions(svnRepository, SVNRevision.create(Long.parseLong(currentRevision)),
                    SVNRevision.create(Long.parseLong(previousRevision)));
        }

        return commits;
    }

    /**
     *
     * Gets the commit changes of each commitInfo First we sort commits by date
     * and then call lastChanges of each commit with previous one
     *
     * @param commitInfoList list of commits between current and previous
     * revision
     *
     * @param oldestCommit is the first commit from previous tree (in
     * git)/revision(in svn) see {@link LastChanges}
     * @param svnAuthProvider
     * @return
     */
    public static List<CommitChanges> commitChanges(Repository gitRepository, boolean isGit, List<CommitInfo> commitInfoList, String oldestCommit, File svnRepository, ISVNAuthenticationProvider svnAuthProvider) {
        if (commitInfoList == null || commitInfoList.isEmpty()) {
            return null;
        }

        List<CommitChanges> commitChanges = new ArrayList<>();

        try {
            Collections.sort(commitInfoList, new Comparator<CommitInfo>() {
                @Override
                public int compare(CommitInfo c1, CommitInfo c2) {
                    try {
                        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT);
                        return format.parse(c1.getCommitDate()).compareTo(format.parse(c2.getCommitDate()));
                    } catch (ParseException e) {
                        LOG.severe(String.format("Could not parse commit dates %s and %s ", c1.getCommitDate(), c2.getCommitDate()));
                        return 0;
                    }
                }
            });

            for (int i = commitInfoList.size() - 1; i >= 0; i--) {
                LastChanges lastChanges = null;
                if (isGit) {
                    ObjectId previousCommit = gitRepository.resolve(commitInfoList.get(i).getCommitId() + "^1");
                    lastChanges = GitLastChanges.getInstance().
                            changesOf(gitRepository, gitRepository.resolve(commitInfoList.get(i).getCommitId()), previousCommit);
                } else {
                    if (i == 0) { //here we can't compare with (i -1) so we compare with first commit of oldest commit (retrieved in main diff)
                        //here we have the older commit from current tree (see LastChanges.java) which diff must be compared with oldestCommit which is currentRevision from previous tree
                        lastChanges = SvnLastChanges.getInstance(svnAuthProvider)
                                .changesOf(svnRepository, SVNRevision.parse(commitInfoList.get(i).getCommitId()), SVNRevision.parse(oldestCommit));
                    } else { //get changes comparing current commit (i) with previous one (i -1)
                        lastChanges = SvnLastChanges.getInstance(svnAuthProvider)
                                .changesOf(svnRepository, SVNRevision.parse(commitInfoList.get(i).getCommitId()), SVNRevision.parse(commitInfoList.get(i - 1).getCommitId()));
                    }
                }
                String diff = lastChanges != null ? lastChanges.getDiff() : "";
                commitChanges.add(new CommitChanges(commitInfoList.get(i), diff));
            }

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Could not get commit changes.", e);
        }

        return commitChanges;
    }
}
