package com.github.jenkins.lastchanges.model;

public class LastChangesConfig {

    private FormatType format = FormatType.LINE;
    private MatchingType matching = MatchingType.NONE;
    private String matchWordsThreshold = "0.25";
    private String matchingMaxComparisons = "1000";
    private Boolean showFiles = Boolean.TRUE;
    private Boolean synchronisedScroll = Boolean.TRUE;
    private SinceType since = SinceType.PREVIOUS_REVISION;//by default it is current revision -1
    private String specificRevision;//diff against a specific revision


    public LastChangesConfig() {
    }

    public LastChangesConfig(SinceType since, String specificRevision, FormatType format, MatchingType matching, Boolean showFiles, Boolean synchronisedScroll, String matchWordsThreshold, String matchingMaxComparisons) {
        super();

        if (since != null) {
            this.since = since;
        }

        if (specificRevision != null) {
            this.specificRevision = specificRevision;
        }

        if (format != null) {
            this.format = format;
        }

        if (matching != null) {
            this.matching = matching;
        }

        if (showFiles != null) {
            this.showFiles = showFiles;
        }

        if (synchronisedScroll != null) {
            this.synchronisedScroll = synchronisedScroll;
        }

        if (matchingMaxComparisons != null) {
            try {
                this.matchingMaxComparisons = String.valueOf(Double.parseDouble(matchingMaxComparisons));
            } catch (NumberFormatException e) {
                //invalid number stay with default
            }
        }

        if (matchWordsThreshold != null) {
            try {
                this.matchWordsThreshold = String.valueOf(Integer.parseInt(matchWordsThreshold));
            } catch (NumberFormatException e) {
                //invalid number stay with default
            }
        }

    }

    public FormatType format() {
        return format;
    }

    public SinceType since() {
        return since;
    }

    public MatchingType matching() {
        return matching;
    }

    public String showFiles() {
        return showFiles.toString();
    }

    public String synchronisedScroll() {
        return synchronisedScroll.toString();
    }

    public String matchingMaxComparisons() {
        return matchingMaxComparisons;
    }

    public String matchWordsThreshold() {
        return matchWordsThreshold;
    }

    public String specificRevision() {
        return specificRevision;
    }

}
