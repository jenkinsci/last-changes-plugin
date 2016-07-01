package com.github.jenkins.lastchanges;

import com.github.jenkins.lastchanges.model.FormatType;
import hudson.model.Run;

import java.io.File;

public class LastChangesBuildAction extends LastChangesBaseAction {

    private final Run<?, ?> build;
    private LastChanges buildChanges;
    private FormatType format;

    public LastChangesBuildAction(Run<?, ?> build, LastChanges lastChanges, FormatType formatType) {
        this.build = build;
        buildChanges = lastChanges;
        this.format = formatType;
    }

    @Override
    protected String getTitle() {
        return "Last Changes of Build #"+this.build.getNumber();
    }

    @Override
    protected File dir() {
        return new File(build.getRootDir(), BASE_URL);
    }

    public LastChanges getBuildChanges() {
        return buildChanges;
    }

    public Run<?, ?> getBuild() {
        return build;
    }

    public FormatType getFormat() {
        return format;
    }

    public boolean isFormatByLine(){
        return format == null || FormatType.LINE.equals(format);
    }
}
