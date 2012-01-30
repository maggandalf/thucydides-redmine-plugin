package net.thucydides.plugins.redmine.integration;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import net.thucydides.plugins.jira.model.IssueComment;
import net.thucydides.plugins.jira.model.IssueTracker;
import net.thucydides.plugins.redmine.service.RedmineConfiguration;
import net.thucydides.plugins.redmine.service.RedmineIssueTracker;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

public class WhenUpdateingIssuesUsingTheRedmineTracker {

	IssueTracker tracker;

	private static final String REDMINE_WEBSERVICE_URL = "http://demo.redmine.org";
	private static final String LOGIN = "thucyint";
	private static final String PASSWORD = "robin";


	private int issueKey;

	private IssueHarness testIssueHarness;

	@Mock
	RedmineConfiguration configuration;

	@Mock
	Logger logger;

	@Before
	public void prepareIssueTracker() throws Exception {
		MockitoAnnotations.initMocks(this);

		when(configuration.getRedmineUser()).thenReturn(LOGIN);
		when(configuration.getRedminePassword()).thenReturn(PASSWORD);
		when(configuration.getRedmineUrl()).thenReturn(REDMINE_WEBSERVICE_URL);

		testIssueHarness = new IssueHarness(REDMINE_WEBSERVICE_URL, LOGIN,
				PASSWORD);
		issueKey = testIssueHarness.createIssue();

		tracker = new RedmineIssueTracker(logger) {

			@Override
			protected RedmineConfiguration getConfiguration() {
				return configuration;
			}

		};
	}

	@After
	public void deleteTestIssue() throws Exception {
		testIssueHarness.deleteIssues();
	}

	@Test
	public void should_be_able_to_add_a_comment_to_an_issue() throws Exception {
		String stringIssueKey = Integer.toString(issueKey);
		List<IssueComment> comments = tracker.getCommentsFor(stringIssueKey);
		assertThat(comments.size(), is(0));

		tracker.addComment(stringIssueKey, "Integration test comment");

		comments = tracker.getCommentsFor(stringIssueKey);
		assertThat(comments.size(), is(1));
	}

//	@Test
//	public void should_be_able_to_add_a_comment_to_a_closed_issue()
//			throws Exception {
//		List<IssueComment> comments = tracker.getCommentsFor(CLOSED_ISSUE);
//
//		String comment = "Comment on closed test: " + new Date();
//
//		tracker.addComment(CLOSED_ISSUE, comment);
//
//		comments = tracker.getCommentsFor(CLOSED_ISSUE);
//		assertThat(comments.size(), greaterThan(0));
//		assertThat(comments.get(comments.size() - 1).getText(), is(comment));
//	}

	@Test
	public void should_be_able_to_update_a_comment_from_an_issue()
			throws Exception {
		tracker.addComment(Integer.toString(issueKey),
				"Integration test comment 1");
		tracker.addComment(Integer.toString(issueKey),
				"Integration test comment 2");
		tracker.addComment(Integer.toString(issueKey),
				"Integration test comment 3");

		List<IssueComment> comments = tracker.getCommentsFor(Integer
				.toString(issueKey));

		IssueComment oldComment = comments.get(0);
		IssueComment updatedComment = new IssueComment(oldComment.getId(),
				"Integration test comment 4", oldComment.getAuthor());

		tracker.updateComment(updatedComment);

		comments = tracker.getCommentsFor(Integer.toString(issueKey));
		assertThat(comments.get(0).getText(), is("Integration test comment 4"));
	}

	@Test
	public void should_not_be_able_to_update_a_comment_from_an_issue_that_does_not_exist()
			throws Exception {
		tracker.addComment("#ISSUE-DOES-NOT-EXIST",
				"Integration test comment 1");

		verify(logger).error("No JIRA issue found with key {}",
				"#ISSUE-DOES-NOT-EXIST");
	}

	@Test
	public void should_be_able_to_read_the_status_of_an_issue_in_human_readable_form()
			throws Exception {

		String status = tracker.getStatusFor(Integer.toString(issueKey));

		assertThat(status, is("Open"));
	}

//	@Test
//	public void should_not_be_able_to_update_the_status_of_a_closed_issue()
//			throws Exception {
//		tracker.doTransition(CLOSED_ISSUE, "Resolved");
//		String newStatus = tracker.getStatusFor(CLOSED_ISSUE);
//		assertThat(newStatus, is("Closed"));
//	}

	@Test
	public void should_be_able_to_update_the_status_of_an_issue()
			throws Exception {
		String status = tracker.getStatusFor(Integer.toString(issueKey));
		assertThat(status, is("Open"));

		tracker.doTransition(Integer.toString(issueKey), "Resolved");

		String newStatus = tracker.getStatusFor(Integer.toString(issueKey));
		assertThat(newStatus, is("Resolved"));
	}

	@Test
	public void should_not_be_able_to_update_the_status_of_an_issue_if_transition_is_not_allowed()
			throws Exception {
		String status = tracker.getStatusFor(Integer.toString(issueKey));
		assertThat(status, is("Open"));

		tracker.doTransition(Integer.toString(issueKey), "In Progress");

		String newStatus = tracker.getStatusFor(Integer.toString(issueKey));
		assertThat(newStatus, is("Open"));
	}

	@Test
	public void should_not_be_able_to_update_the_status_for_an_issue_that_does_not_exist()
			throws Exception {
		tracker.doTransition("#ISSUE-DOES-NOT-EXIST", "Resolve Issue");

		verify(logger).error("No JIRA issue found with key {}",
				"#ISSUE-DOES-NOT-EXIST");
	}

}
