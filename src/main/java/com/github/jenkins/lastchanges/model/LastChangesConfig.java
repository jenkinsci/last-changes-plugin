package com.github.jenkins.lastchanges.model;

public class LastChangesConfig {

    private FormatType format = FormatType.LINE;
    private MatchingType matching = MatchingType.NONE;
    private String matchWordsThreshold = "0.25";
    private String matchingMaxComparisons = "1000";
    private Boolean showFiles = Boolean.TRUE;
    private Boolean synchronisedScroll = Boolean.TRUE;
    private String endRevision = "";//by default it is previous repository revision
    private Boolean lastSuccessFulBuild = Boolean.FALSE;


    public LastChangesConfig() {
    }

    public LastChangesConfig(String endRevision, Boolean lastSuccessFulBuild, FormatType format, MatchingType matching, Boolean showFiles, Boolean synchronisedScroll, String matchWordsThreshold, String matchingMaxComparisons) {
        super();

        if (endRevision != null) {
            this.endRevision = endRevision;
        }

        if (lastSuccessFulBuild != null) {
            this.lastSuccessFulBuild = lastSuccessFulBuild;
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

    public String getEndRevision() {
        return endRevision;
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

    public Boolean lastSuccessFulBuild() {
         return lastSuccessFulBuild;
    }
}
