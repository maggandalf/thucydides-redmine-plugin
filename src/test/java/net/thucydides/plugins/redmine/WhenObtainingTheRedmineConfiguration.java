package net.thucydides.plugins.redmine;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.thucydides.plugins.jira.service.SystemPropertiesJIRAConfiguration;
import net.thucydides.plugins.redmine.service.RedmineConfiguration;
import net.thucydides.plugins.redmine.service.SystemPropertiesRedmineConfiguration;

public class WhenObtainingTheRedmineConfiguration {

	private String originalApiAccessKey;
	private String originalUrl;
	private String originalIsWikiRenderer;
	private String originalUser;
	private String originalPassword;
	
	RedmineConfiguration redmineConfiguration;

	@Before
	public void saveSystemProperties() {
		originalApiAccessKey = System.getProperty("redmine.accessKey");
		originalUrl = System.getProperty("redmine.url");
		originalIsWikiRenderer = System.getProperty("redmine.wiki.renderer");
		originalUser = System.getProperty("redmine.user");
		originalPassword = System.getProperty("redmine.password");
		
		redmineConfiguration = new SystemPropertiesRedmineConfiguration();
	}

	@After
	public void restoreSystemProperties() {
		if (originalApiAccessKey != null) {
			System.setProperty("redmine.accessKey", originalApiAccessKey);
		} else {
			System.clearProperty("redmine.accessKey");
		}
		if (originalUrl != null) {
			System.setProperty("redmine.url", originalUrl);
		} else {
			System.clearProperty("redmine.url");
		}
		if (originalIsWikiRenderer != null) {
			System.setProperty("redmine.wiki.renderer", originalIsWikiRenderer);
		} else {
			System.clearProperty("redmine.wiki.renderer");
		}
		if (originalUser != null) {
			System.setProperty("redmine.user", originalUrl);
		} else {
			System.clearProperty("redmine.user");
		}
		if (originalPassword != null) {
			System.setProperty("redmine.password", originalPassword);
		} else {
			System.clearProperty("redmine.password");
		}
		
	}

	@Test
	public void access_key_should_be_specified_in_the_redmine_access_key_system_property() {
		System.setProperty("redmine.accessKey", "CAFE");
		assertThat(redmineConfiguration.getApiAccessKey(), is("CAFE"));
	}

	@Test
	public void redmine_url_should_be_specified_in_the_redmine_url_system_property() {
		System.setProperty("redmine.url", "http://myredmine");
		assertThat(redmineConfiguration.getRedmineUrl(), is("http://myredmine"));
	}
	
	@Test
	public void redmine_is_wiki_renderer_should_return_true_if_not_specified() {
		assertThat(redmineConfiguration.isWikiRenderedActive(), is(true));
	}
	
}
