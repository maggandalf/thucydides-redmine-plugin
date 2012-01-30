package net.thucydides.plugins.redmine.service;

public interface RedmineConfiguration {

	String getApiAccessKey();
	String getRedmineUrl();
	String getRedmineUser();
	String getRedminePassword();
	boolean isWikiRenderedActive();
	
}
