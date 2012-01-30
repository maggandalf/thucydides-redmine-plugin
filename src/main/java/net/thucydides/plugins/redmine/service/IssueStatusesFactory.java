package net.thucydides.plugins.redmine.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.thucydides.plugins.jira.model.IssueTrackerUpdateException;

import org.redmine.ta.AuthenticationException;
import org.redmine.ta.NotFoundException;
import org.redmine.ta.RedmineException;
import org.redmine.ta.RedmineManager;
import org.redmine.ta.beans.IssueStatus;

class IssueStatusesFactory {

	private Map<String, IssueStatus> issueStatuses = null;
	
	private RedmineManager redmineManager;
	
	IssueStatusesFactory(RedmineManager redmineManager) {
		this.redmineManager = redmineManager;
	}
	
	IssueStatus getIssueStatus(String workFlowAction) {

		if(issueStatuses == null) {
			loadIssueStatuses();
		}
		
		return issueStatuses.get(workFlowAction);
	}
	
	private final void loadIssueStatuses()  {
		
		issueStatuses = new HashMap<String, IssueStatus>();
		
		List<IssueStatus> statuses = statuses();
		
		for (IssueStatus issueStatus : statuses) {
			//workflow action is treated as status name in Redmine Rest API
			this.issueStatuses.put(issueStatus.getName(), issueStatus);
		}
		
	}

	private List<IssueStatus> statuses() {
		List<IssueStatus> statuses = new ArrayList<IssueStatus>();
		try {
			statuses.addAll(redmineManager.getStatuses());
		} catch (IOException e) {
			throw new IssueTrackerUpdateException(e.getMessage(), e);
		} catch (AuthenticationException e) {
			throw new IssueTrackerUpdateException(e.getMessage(), e);
		} catch (RedmineException e) {
			throw new IssueTrackerUpdateException(e.getMessage(), e);
		} catch (NotFoundException e) {
			throw new IssueTrackerUpdateException(e.getMessage(), e);
		}
		return statuses;
	}
	
}
