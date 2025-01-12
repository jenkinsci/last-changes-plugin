window.jQueryJenkins = jQuery.noConflict();

jQueryJenkins(document).ready(function () {
    if (jQueryJenkins('#main-panel').length) {
        jQueryJenkins('#main-panel').attr('class','col-sm-24 col-md-24 col-lg-24 col-xlg-24');
    }

    const { commitChanges } = document.querySelector(".last-changes-commit-data").dataset;

    var sidePanelTD = document.getElementById('side-panel');
    if (sidePanelTD) {
        sidePanelTD.parentNode.removeChild(sidePanelTD);
    }

    var diff2htmlUi = new Diff2HtmlUI({diff: commitChanges});

    diff2htmlUi.draw('#side-by-side', {
        inputFormat: 'json',
        outputFormat: 'side-by-side',
        showFiles: true,
        synchronisedScroll: true,
        matchWordsThreshold: '0.25',
        matchingMaxComparisons: '1500',
        matching: 'lines'
    }
    );
    diff2htmlUi.fileListCloseable('#side-by-side', false);
    diff2htmlUi.highlightCode('#side-by-side');

}); //end documentReady
