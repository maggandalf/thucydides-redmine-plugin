package net.thucydides.plugins.redmine.workflow;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;

import net.thucydides.core.model.TestResult;
import net.thucydides.core.util.EnvironmentVariables;
import net.thucydides.plugins.jira.workflow.ClasspathWorkflowLoader;
import net.thucydides.plugins.jira.workflow.Workflow;
import net.thucydides.plugins.jira.workflow.WorkflowLoader;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class WhenConfiguringWorkflow {

	@Mock
    EnvironmentVariables environmentVariables;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }
	
    @Test
    public void should_be_able_to_load_a_workflow_configuration() {

        WorkflowLoader loader = new ClasspathWorkflowLoader("default-workflow.groovy", environmentVariables);

        Workflow workflow = loader.load();
        assertThat(workflow, is(notNullValue()));

        List<String> transitionSetMap = workflow.getTransitions()
                                                .forTestResult(TestResult.SUCCESS)
                                                .whenIssueIs("In Progress");

        assertThat(transitionSetMap.size(), is(1));
        assertThat(transitionSetMap, Matchers.hasItems("Resolved"));

    }
}
