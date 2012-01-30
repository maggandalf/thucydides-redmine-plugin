package net.thucydides.plugins.redmine.integration;

import java.util.ArrayList;
import java.util.List;

import org.redmine.ta.RedmineManager;
import org.redmine.ta.beans.Issue;

public class IssueHarness {

	private String redmineUrl;
	private String redmineUser;
	private String redminePassword;
	
	private List<Integer> testIssueKeys = new ArrayList<Integer>();
	
	public IssueHarness(String redmineUrl, String redmineUser,
			String redminePassword) {
		super();
		this.redmineUrl = redmineUrl;
		this.redmineUser = redmineUser;
		this.redminePassword = redminePassword;
	}
	
	public Integer createIssue() throws Exception {
		
		RedmineManager redmineManager = new RedmineManager(redmineUrl);
		redmineManager.setLogin(redmineUser);
		redmineManager.setPassword(redminePassword);
		
		Issue issue = new Issue();
		issue.setSubject("A test issue");
		
		Issue createdIssue = redmineManager.createIssue("thucydemo", issue);
		testIssueKeys.add(createdIssue.getId());
		
		return createdIssue.getId();
	}
	
	public void deleteIssues() throws Exception {
		
		RedmineManager redmineManager = new RedmineManager(redmineUrl);
		redmineManager.setLogin(redmineUser);
		redmineManager.setPassword(redminePassword);
		
		for (Integer issueId : this.testIssueKeys) {
			redmineManager.deleteIssue(issueId);			
		}
	}
	
}
