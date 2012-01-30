package net.thucydides.plugins.redmine.guice;

import net.thucydides.core.util.EnvironmentVariables;
import net.thucydides.core.util.SystemEnvironmentVariables;
import net.thucydides.core.webdriver.Configuration;
import net.thucydides.core.webdriver.SystemPropertiesConfiguration;
import net.thucydides.plugins.jira.model.IssueTracker;
import net.thucydides.plugins.jira.workflow.ClasspathWorkflowLoader;
import net.thucydides.plugins.jira.workflow.WorkflowLoader;
import net.thucydides.plugins.redmine.service.RedmineConfiguration;
import net.thucydides.plugins.redmine.service.RedmineIssueTracker;
import net.thucydides.plugins.redmine.service.SystemPropertiesRedmineConfiguration;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class ThucydidesRedmineModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(EnvironmentVariables.class).to(SystemEnvironmentVariables.class);
		bind(RedmineConfiguration.class).to(SystemPropertiesRedmineConfiguration.class);
		bind(Configuration.class).to(SystemPropertiesConfiguration.class);
		bind(IssueTracker.class).to(RedmineIssueTracker.class);
		bind(WorkflowLoader.class).to(ClasspathWorkflowLoader.class);
		bindConstant().annotatedWith(Names.named("defaultWorkflow")).to("default-workflow.groovy");
	}

}
