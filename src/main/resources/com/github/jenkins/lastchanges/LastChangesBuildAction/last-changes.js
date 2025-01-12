window.jQueryJenkins = jQuery.noConflict();

jQueryJenkins(document).ready(function () {
    const lastChangesData = document.querySelector(".last-changes-data").dataset;
    const {
        buildChanges,
        format,
        matchWordsThreshold,
        matchingMaxComparisons,
        matching,
        currentRevisionCommitId,
        previousRevisionCommitId
    } = lastChangesData;
    const showFiles = lastChangesData.showFiles === "true";
    const synchronisedScroll = lastChangesData.synchronisedScroll === "true";

    if (jQueryJenkins('#main-panel').length) {
        jQueryJenkins('#main-panel').attr('class','col-sm-24 col-md-24 col-lg-24 col-xlg-24');
    }
    var sidePanelTD = document.getElementById('side-panel');
    if (sidePanelTD) {
        sidePanelTD.parentNode.removeChild(sidePanelTD);
    }

    if(buildChanges) {
        var diff2htmlUi = new Diff2HtmlUI({diff: buildChanges});

        diff2htmlUi.draw(`#${format}`, {
            inputFormat: 'json',
            outputFormat: format,
            showFiles,
            synchronisedScroll,
            matchWordsThreshold,
            matchingMaxComparisons,
            matching
        });
        diff2htmlUi.fileListCloseable(format, false);
        diff2htmlUi.highlightCode(`#${format}`);
    } else {
        jQueryJenkins('#line-by-line').append(`<p style="margin-top:150px;text-align:center;font-size:14px;">No changes between revision <span style="font-weight:600;text-decoration:underline">${currentRevisionCommitId}</span> and <span style="font-weight:600;text-decoration:underline">${previousRevisionCommitId}</span> </p>`)
        jQueryJenkins('#changes-info, .d2h-show2').hide();
    }

    document.querySelector(".d2h-hide2").addEventListener("click", () => {
        hideCommits();
    });
    document.querySelector(".d2h-show2").addEventListener("click", () => {
        showCommits();
    });
}); //end documentReady

function showCommits() {
    jQueryJenkins('.d2h-show2').hide();
    jQueryJenkins('.d2h-hide2, #commits').show();
}

function hideCommits() {
    jQueryJenkins('.d2h-show2').show();
    jQueryJenkins('.d2h-hide2, #commits').hide();
}
