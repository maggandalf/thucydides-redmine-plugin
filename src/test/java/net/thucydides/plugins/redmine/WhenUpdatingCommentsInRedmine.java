package net.thucydides.plugins.redmine;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import net.thucydides.core.annotations.Feature;
import net.thucydides.core.annotations.Issue;
import net.thucydides.core.annotations.Issues;
import net.thucydides.core.annotations.Story;
import net.thucydides.core.annotations.Title;
import net.thucydides.core.model.TestOutcome;
import net.thucydides.core.model.TestResult;
import net.thucydides.core.model.TestStep;
import net.thucydides.core.steps.ExecutedStepDescription;
import net.thucydides.core.steps.StepFailure;
import net.thucydides.core.util.EnvironmentVariables;
import net.thucydides.plugins.jira.model.IssueComment;
import net.thucydides.plugins.jira.model.IssueTracker;
import net.thucydides.plugins.jira.service.NoSuchIssueException;
import net.thucydides.plugins.jira.workflow.ClasspathWorkflowLoader;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class WhenUpdatingCommentsInRedmine {

	@Feature
    public static final class SampleFeature {
        public class SampleStory {}
        public class SampleStory2 {}
    }

    @Story(SampleFeature.SampleStory.class)
    private static final class SampleTestSuite {

        @Title("Test for issue #123")
        public void issue_123_should_be_fixed_now() {}

        @Title("Fixes issues #123,#456")
        public void issue_123_and_456_should_be_fixed_now() {}

        public void anotherTest() {}
    }

    @Story(SampleFeature.SampleStory.class)
    private static final class SampleTestSuiteWithIssueAnnotation {

        @Issue("#123")
        public void issue_123_should_be_fixed_now() {}

        @Issues({"123", "456"})
        public void issue_123_and_456_should_be_fixed_now() {}

        public void anotherTest() {}
    }
    ClasspathWorkflowLoader workflowLoader;
    
    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        when(environmentVariables.getProperty("redmine.url")).thenReturn("http://my.redmine.server");
        when(environmentVariables.getProperty("thucydides.public.url"))
                                 .thenReturn("http://my.server/myproject/thucydides");
        workflowLoader = new ClasspathWorkflowLoader(ClasspathWorkflowLoader.BUNDLED_WORKFLOW, environmentVariables);
    }
    
    @After
    public void resetPluginSpecificProperties() {
        System.clearProperty("thucydides.skip.redmine.updates");
    }

    @Mock
    IssueTracker issueTracker;

    @Mock
    EnvironmentVariables environmentVariables;
    
    private TestOutcome newTestOutcome(String testMethod, TestResult testResult) {
        TestOutcome result = TestOutcome.forTest(testMethod, SampleTestSuite.class);
        TestStep step = new TestStep("a narrative description");
        step.setResult(testResult);
        result.recordStep(step);
        return result;
    }
    
    @Test
    public void when_a_test_with_a_referenced_issue_finishes_the_plugin_should_add_a_new_comment_for_this_issue() {
        RedmineListener listener = new RedmineListener(issueTracker, environmentVariables, workflowLoader);

        listener.testSuiteStarted(SampleTestSuite.class);
        listener.testStarted("issue_123_should_be_fixed_now");
        listener.testFinished(newTestOutcome("issue_123_should_be_fixed_now", TestResult.SUCCESS));

        verify(issueTracker).addComment(eq("123"), anyString());
    }
    
    @Test
    public void when_a_test_with_a_referenced_annotated_issue_finishes_the_plugin_should_add_a_new_comment_for_this_issue() {
    	RedmineListener listener = new RedmineListener(issueTracker, environmentVariables, workflowLoader);

        listener.testSuiteStarted(SampleTestSuiteWithIssueAnnotation.class);
        listener.testStarted("issue_123_should_be_fixed_now");
        listener.testFinished(newTestOutcome("issue_123_should_be_fixed_now", TestResult.SUCCESS));

        verify(issueTracker).addComment(eq("123"), anyString());
    }
    
    @Test
    public void when_a_test_with_several_referenced_issues_finishes_the_plugin_should_add_a_new_comment_for_each_issue() {
    	RedmineListener listener = new RedmineListener(issueTracker, environmentVariables, workflowLoader);

        listener.testSuiteStarted(SampleTestSuite.class);
        listener.testStarted("issue_123_and_456_should_be_fixed_now");
        listener.testFinished(newTestOutcome("issue_123_and_456_should_be_fixed_now", TestResult.SUCCESS));

        verify(issueTracker).addComment(eq("123"), anyString());
        verify(issueTracker).addComment(eq("456"), anyString());
    }
    
    @Test
    public void when_a_test_with_several_annotated_referenced_issues_finishes_the_plugin_should_add_a_new_comment_for_each_issue() {
        RedmineListener listener = new RedmineListener(issueTracker, environmentVariables, workflowLoader);

        listener.testSuiteStarted(SampleTestSuiteWithIssueAnnotation.class);
        listener.testStarted("issue_123_and_456_should_be_fixed_now");
        listener.testFinished(newTestOutcome("issue_123_and_456_should_be_fixed_now", TestResult.SUCCESS));

        verify(issueTracker).addComment(eq("123"), anyString());
        verify(issueTracker).addComment(eq("456"), anyString());
    }
    
    @Mock
    ExecutedStepDescription stepDescription;

    @Mock
    StepFailure failure;
    
    @Test
    public void should_add_one_comment_even_when_several_steps_are_called() {
        RedmineListener listener = new RedmineListener(issueTracker, environmentVariables, workflowLoader);

        listener.testSuiteStarted(SampleTestSuite.class);
        listener.testStarted("issue_123_and_456_should_be_fixed_now");

        listener.stepStarted(stepDescription);
        listener.stepFinished();

        listener.stepStarted(stepDescription);
        listener.stepFailed(failure);

        listener.stepStarted(stepDescription);
        listener.stepIgnored();

        listener.stepStarted(stepDescription);
        listener.stepPending();

        listener.testFailed(new AssertionError("Oops!"));

        listener.testFinished(newTestOutcome("issue_123_and_456_should_be_fixed_now", TestResult.FAILURE));

        listener.testStarted("anotherTest");
        listener.testIgnored();

        verify(issueTracker).addComment(eq("123"), anyString());
        verify(issueTracker).addComment(eq("456"), anyString());
    }
    
    @Test
    public void should_work_with_a_story_class() {
    	RedmineListener listener = new RedmineListener(issueTracker, environmentVariables, workflowLoader);

        listener.testSuiteStarted(net.thucydides.core.model.Story.from(SampleTestSuite.class));
        listener.testStarted("Fixes issues #MYPROJECT-123");
        listener.testFinished(newTestOutcome("Fixes issues #MYPROJECT-123", TestResult.FAILURE));

        verify(issueTracker).addComment(eq("MYPROJECT-123"),
                contains("[Thucydides Test Results|http://my.server/myproject/thucydides/sample_test_suite.html]"));
    }
    
    @Test
    public void the_comment_should_contain_a_link_to_the_corresponding_story_report() {
    	RedmineListener listener = new RedmineListener(issueTracker, environmentVariables, workflowLoader);
        listener.testSuiteStarted(SampleTestSuite.class);
        listener.testStarted("issue_123_should_be_fixed_now");
        listener.testFinished(newTestOutcome("issue_123_should_be_fixed_now", TestResult.FAILURE));

        verify(issueTracker).addComment(eq("MYPROJECT-123"),
                contains("[Thucydides Test Results|http://my.server/myproject/thucydides/sample_story.html]"));
    }
 
    @Test
    public void should_update_existing_thucydides_report_comments_if_present() {

        List<IssueComment> existingComments = Arrays.asList(new IssueComment(1L,"a comment", "bruce"),
                                                            new IssueComment(2L,"Thucydides Test Results", "bruce"));
        when(issueTracker.getCommentsFor("MYPROJECT-123")).thenReturn(existingComments);

        RedmineListener listener = new RedmineListener(issueTracker, environmentVariables, workflowLoader);
        listener.testSuiteStarted(SampleTestSuite.class);
        listener.testStarted("issue_123_should_be_fixed_now");
        listener.testFinished(newTestOutcome("issue_123_should_be_fixed_now", TestResult.FAILURE));

        verify(issueTracker).updateComment(any(IssueComment.class));
    }
    
    @Test
    public void should_not_update_status_if_issue_does_not_exist() {
        when(issueTracker.getStatusFor("MYPROJECT-123"))
                         .thenThrow(new NoSuchIssueException("It ain't there no more."));

        RedmineListener listener = new RedmineListener(issueTracker, environmentVariables, workflowLoader);
        listener.testSuiteStarted(SampleTestSuite.class);
        listener.testStarted("issue_123_should_be_fixed_now");
        listener.testFinished(newTestOutcome("issue_123_should_be_fixed_now", TestResult.FAILURE));

        verify(issueTracker, never()).doTransition(anyString(), anyString());
    }

    @Test
    public void should_not_update_status_if_jira_url_is_undefined() {
        when(environmentVariables.getProperty("redmine.url")).thenReturn("");

        RedmineListener listener = new RedmineListener(issueTracker, environmentVariables, workflowLoader);
        listener.testSuiteStarted(SampleTestSuite.class);
        listener.testStarted("issue_123_should_be_fixed_now");
        listener.testFinished(newTestOutcome("issue_123_should_be_fixed_now", TestResult.FAILURE));

        verify(issueTracker, never()).doTransition(anyString(), anyString());
    }

    @Test
    public void should_skip_JIRA_updates_if_requested() {
        when(environmentVariables.getProperty("thucydides.skip.redmine.updates")).thenReturn("true");

        RedmineListener listener = new RedmineListener(issueTracker, environmentVariables, workflowLoader);
        listener.testSuiteStarted(SampleTestSuite.class);
        listener.testStarted("issue_123_should_be_fixed_now");
        listener.testFinished(newTestOutcome("issue_123_should_be_fixed_now", TestResult.FAILURE));

        verify(issueTracker, never()).addComment(anyString(), anyString());
    }


    @Test
    public void should_skip_JIRA_updates_if_no_public_url_is_specified() {

        when(environmentVariables.getProperty("thucydides.public.url")).thenReturn("");
        RedmineListener listener = new RedmineListener(issueTracker, environmentVariables, workflowLoader);
        listener.testSuiteStarted(SampleTestSuite.class);
        listener.testStarted("issue_123_should_be_fixed_now");
        listener.testFinished(newTestOutcome("issue_123_should_be_fixed_now", TestResult.FAILURE));

        verify(issueTracker, never()).addComment(anyString(), anyString());
    }

    @Test
    public void default_listeners_should_use_default_issue_tracker() {
    	RedmineListener listener = new RedmineListener();

        assertThat(listener.getIssueTracker(), is(notNullValue()));
    }
    
}
