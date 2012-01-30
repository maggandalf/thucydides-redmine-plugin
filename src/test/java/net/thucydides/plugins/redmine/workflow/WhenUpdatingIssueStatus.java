package net.thucydides.plugins.redmine.workflow;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import net.thucydides.core.annotations.Feature;
import net.thucydides.core.annotations.Issue;
import net.thucydides.core.annotations.Issues;
import net.thucydides.core.annotations.Story;
import net.thucydides.core.model.TestOutcome;
import net.thucydides.core.model.TestResult;
import net.thucydides.core.model.TestStep;
import net.thucydides.core.util.EnvironmentVariables;
import net.thucydides.plugins.jira.model.IssueTracker;
import net.thucydides.plugins.jira.workflow.ClasspathWorkflowLoader;
import net.thucydides.plugins.redmine.RedmineListener;
import net.thucydides.plugins.redmine.service.RedmineConfiguration;

public class WhenUpdatingIssueStatus {

	@Feature
	public static final class SampleFeature {
		public class SampleStory {
		}

		public class SampleStory2 {
		}
	}

	@Story(SampleFeature.SampleStory.class)
	static class SampleTestCase {

		@Issue("#123")
		public void issue_123_should_be_fixed_now() {
		}

		@Issues({ "#123", "#456" })
		public void issue_123_and_456_should_be_fixed_now() {
		}

		public void anotherTest() {
		}
	}

	@Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        when(environmentVariables.getProperty("redmine.url")).thenReturn("http://my.redmine.server");
        when(environmentVariables.getProperty("thucydides.public.url")).thenReturn("http://my.server/myproject/thucydides");
        
        workflowLoader = new ClasspathWorkflowLoader(ClasspathWorkflowLoader.BUNDLED_WORKFLOW, environmentVariables);
    }

    @Mock
    IssueTracker issueTracker;

    @Mock
    EnvironmentVariables environmentVariables;

    
    ClasspathWorkflowLoader workflowLoader;

	
    @Test
    public void a_successful_test_should_not_update_status_if_workflow_is_not_activated() {

        TestOutcome result = newTestOutcome("issue_123_should_be_fixed_now", TestResult.SUCCESS);

        when(issueTracker.getStatusFor("123")).thenReturn("Assigned");
        when(environmentVariables.getProperty("thucydides.jira.workflow.active")).thenReturn("false");

        workflowLoader = new ClasspathWorkflowLoader(ClasspathWorkflowLoader.BUNDLED_WORKFLOW, environmentVariables);
        RedmineListener listener = new RedmineListener(issueTracker, environmentVariables, workflowLoader);
        
        listener.testSuiteStarted(SampleTestCase.class);
        listener.testStarted("issue_123_should_be_fixed_now");
        listener.testFinished(result);
        listener.testSuiteFinished();

        verify(issueTracker, never()).doTransition(eq("123"),anyString());
    }
    
    @Test
    public void a_successful_test_should_not_update_status_if_workflow_update_status_is_not_specified() {

        TestOutcome result = newTestOutcome("issue_123_should_be_fixed_now", TestResult.SUCCESS);

        when(issueTracker.getStatusFor("123")).thenReturn("Assigned");
        when(environmentVariables.getProperty("thucydides.jira.workflow.active")).thenReturn("");

        workflowLoader = new ClasspathWorkflowLoader(ClasspathWorkflowLoader.BUNDLED_WORKFLOW, environmentVariables);
        RedmineListener listener = new RedmineListener(issueTracker, environmentVariables, workflowLoader);

        listener.testSuiteStarted(SampleTestCase.class);
        listener.testStarted("issue_123_should_be_fixed_now");
        listener.testFinished(result);
        listener.testSuiteFinished();
        
        verify(issueTracker, never()).doTransition(eq("123"),anyString());
    }
    
    
    @Test
    public void a_successful_test_should_resolve_an_in_progress_issue() {
        when(environmentVariables.getProperty("thucydides.jira.workflow.active")).thenReturn("true");
        TestOutcome result = newTestOutcome("issue_123_should_be_fixed_now", TestResult.SUCCESS);

        when(issueTracker.getStatusFor("123")).thenReturn("In Progress");

        RedmineListener listener = new RedmineListener(issueTracker, environmentVariables, workflowLoader);
        listener.testSuiteStarted(SampleTestCase.class);
        listener.testStarted("issue_123_should_be_fixed_now");
        listener.testFinished(result);
        listener.testSuiteFinished();
        
        InOrder inOrder = inOrder(issueTracker);
        inOrder.verify(issueTracker).doTransition("123","Resolved");
    }
    
    
    @Test
    public void a_successful_test_should_not_affect_a_resolved_issue() {
        when(environmentVariables.getProperty("thucydides.jira.workflow.active")).thenReturn("true");
        TestOutcome result = newTestOutcome("issue_123_should_be_fixed_now", TestResult.SUCCESS);

        when(issueTracker.getStatusFor("123")).thenReturn("Resolved");

        RedmineListener listener = new RedmineListener(issueTracker, environmentVariables, workflowLoader);
        listener.testSuiteStarted(SampleTestCase.class);
        listener.testStarted("issue_123_should_be_fixed_now");
        listener.testFinished(result);
        listener.testSuiteFinished();
        
        verify(issueTracker, never()).doTransition(eq("123"), anyString());
    }
    
    @Test
    public void a_failing_test_should_open_a_resolved_issue() {
        when(environmentVariables.getProperty("thucydides.jira.workflow.active")).thenReturn("true");
        TestOutcome result = newTestOutcome("issue_123_should_be_fixed_now", TestResult.FAILURE);

        when(issueTracker.getStatusFor("123")).thenReturn("Resolved");

        RedmineListener listener = new RedmineListener(issueTracker, environmentVariables, workflowLoader);
        listener.testSuiteStarted(SampleTestCase.class);
        listener.testStarted("issue_123_should_be_fixed_now");
        listener.testFinished(result);
        listener.testSuiteFinished();
        
        verify(issueTracker).doTransition("123", "In Progress");
    }
    
    @Test
    public void a_failing_test_should_open_a_closed_issue() {
        when(environmentVariables.getProperty("thucydides.jira.workflow.active")).thenReturn("true");
        TestOutcome result = newTestOutcome("issue_123_should_be_fixed_now", TestResult.FAILURE);

        when(issueTracker.getStatusFor("123")).thenReturn("Closed");

        RedmineListener listener = new RedmineListener(issueTracker, environmentVariables, workflowLoader);
        listener.testSuiteStarted(SampleTestCase.class);
        listener.testStarted("issue_123_should_be_fixed_now");
        listener.testFinished(result);
        listener.testSuiteFinished();
        
        verify(issueTracker).doTransition("123", "In Progress");
    }
    
    @Test
    public void a_failing_test_should_leave_an_open_issue_open() {
        when(environmentVariables.getProperty("thucydides.jira.workflow.active")).thenReturn("true");
        TestOutcome result = newTestOutcome("issue_123_should_be_fixed_now", TestResult.FAILURE);

        when(issueTracker.getStatusFor("123")).thenReturn("In Progress");

        RedmineListener listener = new RedmineListener(issueTracker, environmentVariables, workflowLoader);
        listener.testSuiteStarted(SampleTestCase.class);
        listener.testStarted("issue_123_should_be_fixed_now");
        listener.testFinished(result);
        listener.testSuiteFinished();

        verify(issueTracker, never()).doTransition(eq("123"), anyString());
    }

    
    @Test
    public void a_failing_test_should_leave_in_progress_issue_in_progress() {
        when(environmentVariables.getProperty("thucydides.jira.workflow.active")).thenReturn("true");
        TestOutcome result = newTestOutcome("issue_123_should_be_fixed_now", TestResult.FAILURE);

        when(issueTracker.getStatusFor("123")).thenReturn("In Progress");

        RedmineListener listener = new RedmineListener(issueTracker, environmentVariables, workflowLoader);
        listener.testSuiteStarted(SampleTestCase.class);
        listener.testStarted("issue_123_should_be_fixed_now");
        listener.testFinished(result);
        listener.testSuiteFinished();

        verify(issueTracker, never()).doTransition(eq("123"), anyString());
    }
    
    private TestOutcome newTestOutcome(String testMethod, TestResult testResult) {
        TestOutcome result = TestOutcome.forTest(testMethod, SampleTestCase.class);
        TestStep step = new TestStep("a narrative description");
        step.setResult(testResult);
        result.recordStep(step);
        return result;
    }
    
}
