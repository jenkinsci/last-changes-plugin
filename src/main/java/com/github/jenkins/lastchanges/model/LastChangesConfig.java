package com.github.jenkins.lastchanges.model;

public class LastChangesConfig {
	
	private FormatType format;
	private MatchingType matching;
	public LastChangesConfig(FormatType format, MatchingType matching) {
		super();
		if(format == null){
			format = FormatType.LINE;
		}
		if(matching == null){
			matching = MatchingType.NONE;
		}
		
		this.format = format;
		this.matching = matching;
	}
	
	
	public FormatType format() {
		return format;
	}
	public MatchingType matching() {
		return matching;
	}
	
	
	public boolean isFormatByLine(){
        return FormatType.LINE.equals(format);
    }
	
	

}
