package net.thucydides.plugins.redmine;

import static ch.lambdaj.Lambda.convert;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.thucydides.core.ThucydidesSystemProperty;
import net.thucydides.core.model.Stories;
import net.thucydides.core.model.Story;
import net.thucydides.core.model.TestOutcome;
import net.thucydides.core.model.TestResult;
import net.thucydides.core.model.ReportNamer.ReportType;
import net.thucydides.core.steps.ExecutedStepDescription;
import net.thucydides.core.steps.StepFailure;
import net.thucydides.core.steps.StepListener;
import net.thucydides.core.util.EnvironmentVariables;

import net.thucydides.plugins.jira.TestResultTally;
import net.thucydides.plugins.jira.model.IssueComment;
import net.thucydides.plugins.jira.model.IssueTracker;
import net.thucydides.plugins.jira.model.TestResultComment;
import net.thucydides.plugins.jira.service.NoSuchIssueException;
import net.thucydides.plugins.jira.workflow.ClasspathWorkflowLoader;
import net.thucydides.plugins.jira.workflow.Workflow;
import net.thucydides.plugins.jira.workflow.WorkflowLoader;
import net.thucydides.plugins.redmine.guice.Injectors;
import net.thucydides.plugins.redmine.service.RedmineConfiguration;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.lambdaj.function.convert.Converter;

public class RedmineListener implements StepListener {

	private final IssueTracker issueTracker;
	private final EnvironmentVariables environmentVariables;
	private final RedmineConfiguration redmineConfiguration;

	private final TestResultTally resultTally;

	private Workflow workflow;
	WorkflowLoader workflowLoader;

	private Class<?> currentTestCase;
	public Story currentStory;

	private final String projectPrefix;

	private static final Logger LOGGER = LoggerFactory
			.getLogger(RedmineListener.class);

	public RedmineListener(IssueTracker issueTracker,
			EnvironmentVariables environmentVariables,
			WorkflowLoader workflowLoader) {
		this.issueTracker = issueTracker;
		this.environmentVariables = environmentVariables;
		this.redmineConfiguration = Injectors.getInjector().getInstance(
				RedmineConfiguration.class);
		this.projectPrefix = environmentVariables
				.getProperty(ThucydidesSystemProperty.JIRA_PROJECT
						.getPropertyName());
		this.workflowLoader = workflowLoader;
		this.resultTally = new TestResultTally();
		this.workflow = this.workflowLoader.load();
	}

	protected boolean shouldUpdateIssues() {

		String redmineUrl = environmentVariables.getProperty("redmine.url");
		String reportUrl = environmentVariables
				.getProperty(ThucydidesSystemProperty.PUBLIC_URL
						.getPropertyName());
		LOGGER.info("REDMINE LISTENER STATUS");
		LOGGER.info("REDMINE URL = {} ", redmineUrl);
		LOGGER.info("REPORT URL = {} ", reportUrl);
		LOGGER.info("WORKFLOW ACTIVE = {} ", workflow.isActive());
		if (workflow.isActive()) {
			LOGGER.info("WORKFLOW TRANSITIONS = {}", workflow.getTransitions());
		}

		return !(StringUtils.isEmpty(redmineUrl) || StringUtils
				.isEmpty(reportUrl));
	}

	protected boolean shouldUpdateWorkflow() {
		Boolean workflowUpdatesEnabled = Boolean
				.valueOf(environmentVariables
						.getProperty(ClasspathWorkflowLoader.ACTIVATE_WORKFLOW_PROPERTY));
		return (workflowUpdatesEnabled);
	}

	public RedmineListener() {
		this(Injectors.getInjector().getInstance(IssueTracker.class), Injectors
				.getInjector().getInstance(EnvironmentVariables.class),
				Injectors.getInjector().getInstance(WorkflowLoader.class));
	}

	protected IssueTracker getIssueTracker() {
		return issueTracker;
	}

	protected Workflow getWorkflow() {
		return workflow;
	}

	public void testSuiteStarted(Class<?> testCase) {
		this.currentTestCase = testCase;
		this.currentStory = null;
	}

	public void testSuiteStarted(Story story) {
		this.currentStory = story;
		this.currentTestCase = null;
	}

	public void testSuiteFinished() {
		if (shouldUpdateIssues()) {
			Set<String> issues = resultTally.getIssues();
			updateIssueStatus(issues);
		}
	}

	public void testStarted(String description) {
	}

	public void testFinished(TestOutcome result) {
		if (shouldUpdateIssues()) {
			List<String> issues = addPrefixesIfRequired(stripInitialHashesFrom(issueReferencesIn(result)));
			tallyResults(result, issues);
		}
	}

	private List<String> addPrefixesIfRequired(final List<String> issueNumbers) {
		return convert(issueNumbers, toIssueNumbersWithPrefixes());
	}

	private Converter<String, String> toIssueNumbersWithPrefixes() {
		return new Converter<String, String>() {
			public String convert(String issueNumber) {
				if (StringUtils.isEmpty(projectPrefix)) {
					return issueNumber;
				}
				if (issueNumber.startsWith(projectPrefix)) {
					return issueNumber;
				}
				return projectPrefix + "-" + issueNumber;
			}
		};
	}

	private void tallyResults(TestOutcome result, List<String> issues) {
		for (String issue : issues) {
			resultTally.recordResult(issue, result);
		}
	}

	private Set<String> issueReferencesIn(TestOutcome result) {
		return result.getIssues();
	}

	private List<String> stripInitialHashesFrom(final Set<String> issueNumbers) {
		List<String> issues = new ArrayList<String>();
		if (issueNumbers != null) {
			for (String issueNumber : issueNumbers) {
				if (issueNumber.startsWith("#")) {
					issues.add(issueNumber.substring(1));
				} else {
					issues.add(issueNumber);
				}
			}
		}
		return issues;
	}

	private void updateIssueStatus(Set<String> issues) {
		for (String issue : issues) {
			logIssueTracking(issue);
			if (!dryRun()) {
				updateIssue(issue, resultTally.getTestOutcomesForIssue(issue));
			}
		}
	}

	private void updateIssue(String issueId, List<TestOutcome> testOutcomes) {
		try {
			TestResultComment testResultComment = newOrUpdatedCommentFor(
					issueId, testOutcomes);
			if (getWorkflow().isActive() && shouldUpdateWorkflow()) {
				updateIssueStatusFor(issueId,
						testResultComment.getOverallResult());
			}
		} catch (NoSuchIssueException e) {
			LOGGER.error("No JIRA issue found with ID {}", issueId);
		}
	}

	private TestResultComment newOrUpdatedCommentFor(final String issueId,
			List<TestOutcome> testOutcomes) {
		LOGGER.info("Updating comments for issue {}", issueId);

		List<IssueComment> comments = issueTracker.getCommentsFor(issueId);
		IssueComment existingComment = findExistingThucydidesCommentIn(comments);
		String testRunNumber = environmentVariables.getProperty("build.id");
		TestResultComment testResultComment;

		if (existingComment == null) {
			testResultComment = TestResultComment.comment()
					.withResults(testOutcomes).withReportUrl(linkToReport())
					.withTestRun(testRunNumber).asComment();

			issueTracker.addComment(issueId, testResultComment.asText());
		} else {
			testResultComment = TestResultComment
					.fromText(existingComment.getText())
					.withUpdatedTestResults(testOutcomes)
					.withUpdatedReportUrl(linkToReport())
					.withUpdatedTestRunNumber(testRunNumber);

			IssueComment updatedComment = new IssueComment(
					existingComment.getId(), testResultComment.asText(),
					existingComment.getAuthor());
			issueTracker.updateComment(updatedComment);

		}
		return testResultComment;
	}

	private IssueComment findExistingThucydidesCommentIn(
			List<IssueComment> comments) {
		for (IssueComment comment : comments) {
			if (comment.getText().contains("Thucydides Test Results")) {
				return comment;
			}
		}
		return null;
	}

	private void updateIssueStatusFor(final String issueId,
			final TestResult testResult) {
		LOGGER.info("Updating status for issue {} with test result {}",
				issueId, testResult);
		String currentStatus = issueTracker.getStatusFor(issueId);

		LOGGER.info("Issue {} currently has status '{}'", issueId,
				currentStatus);

		List<String> transitions = getWorkflow().getTransitions()
				.forTestResult(testResult).whenIssueIs(currentStatus);
		LOGGER.info("Found transitions: {}", transitions);

		for (String transition : transitions) {
			issueTracker.doTransition(issueId, transition);
		}
	}


	private String linkToReport() {
		String reportUrl = environmentVariables
				.getProperty(ThucydidesSystemProperty.PUBLIC_URL
						.getPropertyName());
		String reportName = Stories
				.reportFor(storyUnderTest(), ReportType.HTML);
		return formatTestResultsLink(reportUrl, reportName);
	}

	public String formatTestResultsLink(String reportUrl, String reportName) {
		if (isWikiRenderedActive()) {
			return "[Thucydides Test Results|" + reportUrl + "/" + reportName
					+ "]";
		} else {
			return "Thucydides Test Results: " + reportUrl + "/" + reportName;
		}
	}

	private boolean isWikiRenderedActive() {
		return redmineConfiguration.isWikiRenderedActive();
	}

	private Story storyUnderTest() {
		if (currentTestCase != null) {
			return Stories.findStoryFrom(currentTestCase);
		} else {
			return currentStory;
		}
	}

	private void logIssueTracking(final String issueId) {
		if (dryRun()) {
			LOGGER.info("--- DRY RUN ONLY: REDMINE WILL NOT BE UPDATED ---");
		}
		LOGGER.info("Updating Redmine issue: " + issueId);
		LOGGER.info("Redmine server: " + issueTracker.toString());
	}

	private boolean dryRun() {
		return Boolean.valueOf(environmentVariables
				.getProperty("thucydides.skip.redmine.updates"));
	}

	public void stepStarted(ExecutedStepDescription description) {
	}

	public void skippedStepStarted(ExecutedStepDescription description) {
	}

	public void stepFailed(StepFailure failure) {
	}

	public void stepIgnored() {
	}

	public void stepPending() {
	}

	public void stepFinished() {
	}

	public void testFailed(Throwable cause) {
	}

	public void testIgnored() {
	}

	public void notifyScreenChange() {
	}

}
