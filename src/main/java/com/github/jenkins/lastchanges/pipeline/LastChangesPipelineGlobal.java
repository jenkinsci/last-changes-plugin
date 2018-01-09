package com.github.jenkins.lastchanges.pipeline;

import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;


public class LastChangesPipelineGlobal implements Serializable {
    private org.jenkinsci.plugins.workflow.cps.CpsScript script;

    public LastChangesPipelineGlobal(CpsScript script) {
        this.script = script;
    }

    @Whitelisted
    public LastChangesPublisherScript getLastChangesPublisher(String since,
                                                              String format,
                                                              String matching,
                                                              Boolean showFiles,
                                                              Boolean synchronisedScroll,
                                                              String matchWordsThreshold,
                                                              String matchingMaxComparisons,
                                                              String specificRevision,
                                                              String vcsDir,
                                                              String specificBuild) {

        Map<String, Object> stepVariables = new LinkedHashMap<>();
        stepVariables.put("since", since);
        stepVariables.put("format", format);
        stepVariables.put("matching", matching);
        stepVariables.put("showFiles", showFiles);
        stepVariables.put("synchronisedScroll", synchronisedScroll);
        stepVariables.put("matchWordsThreshold", matchWordsThreshold);
        stepVariables.put("matchingMaxComparisons", matchingMaxComparisons);
        stepVariables.put("specificRevision", specificRevision);
        stepVariables.put("vcsDir", vcsDir);
        stepVariables.put("specificBuild", specificBuild);

        LastChangesPublisherScript publisher = (LastChangesPublisherScript) this.script.invokeMethod("getLastChangesPublisher", stepVariables);
        publisher.setCpsScript(this.script);
        return publisher;
    }
}