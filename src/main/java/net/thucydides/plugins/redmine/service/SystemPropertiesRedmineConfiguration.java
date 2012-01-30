package net.thucydides.plugins.redmine.service;

public class SystemPropertiesRedmineConfiguration implements
		RedmineConfiguration {

	private static final String REDMINE_API_ACCESS_KEY = "redmine.accessKey";
	private static final String REDMINE_URL = "redmine.url";
	private static final String REDMINE_WIKI_RENDERER = "redmine.wiki.renderer";
	private static final String REDMINE_USER = "redmine.user";
	private static final String REDMINE_PASSWORD = "redmine.password";
	
	@Override
	public boolean isWikiRenderedActive() {
        return Boolean.valueOf(System.getProperty(REDMINE_WIKI_RENDERER, "true"));
    }
	
	@Override
	public String getApiAccessKey() {
		return System.getProperty(REDMINE_API_ACCESS_KEY);
	}

	@Override
	public String getRedmineUrl() {
		return System.getProperty(REDMINE_URL);
	}

	@Override
	public String getRedmineUser() {
		return System.getProperty(REDMINE_USER);
	}

	@Override
	public String getRedminePassword() {
		return System.getProperty(REDMINE_PASSWORD);
	}

}
