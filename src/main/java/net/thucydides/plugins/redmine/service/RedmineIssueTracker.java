package net.thucydides.plugins.redmine.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.thucydides.plugins.jira.model.IssueComment;
import net.thucydides.plugins.jira.model.IssueTracker;
import net.thucydides.plugins.jira.model.IssueTrackerUpdateException;
import net.thucydides.plugins.redmine.guice.Injectors;

import org.redmine.ta.AuthenticationException;
import org.redmine.ta.NotFoundException;
import org.redmine.ta.RedmineException;
import org.redmine.ta.RedmineManager;
import org.redmine.ta.RedmineManager.INCLUDE;
import org.redmine.ta.beans.Issue;
import org.redmine.ta.beans.IssueStatus;
import org.redmine.ta.beans.Journal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedmineIssueTracker implements IssueTracker {

	private final RedmineConfiguration redmineConfiguration;
	private final Logger logger;
	
	private RedmineManager redmineManager;
	private IssueStatusesFactory issueStatusesFactory;
	
	public RedmineIssueTracker(Logger logger) {
		this.logger = logger;
		this.redmineConfiguration = Injectors.getInjector().getInstance(RedmineConfiguration.class);
		this.issueStatusesFactory = new IssueStatusesFactory(getRedmineManager());
	}
	
	public RedmineIssueTracker() {
		 this(LoggerFactory.getLogger(RedmineIssueTracker.class));
	}
	
	protected RedmineConfiguration getConfiguration() {
		return this.redmineConfiguration;
	}
	
	public void addComment(String issueKey, String commentText)
			throws IssueTrackerUpdateException {
		RedmineManager redmineManager = getRedmineManager();
		addCommentField(issueKey, commentText, redmineManager);
	}

	private void addCommentField(String issueKey, String commentText,
			RedmineManager redmineManager) {
		try {
			Issue issue = redmineManager.getIssueById(Integer.parseInt(issueKey));
			issue.setNotes(commentText);
			redmineManager.updateIssue(issue);
		} catch (NumberFormatException e) {
			 logger.error("Redmine issue {} is not a valid Redmine issue id.", issueKey);
		} catch (IOException e) {
			throw new IssueTrackerUpdateException(e.getMessage(), e);
		} catch (AuthenticationException e) {
			throw new IssueTrackerUpdateException(e.getMessage(), e);
		} catch (NotFoundException e) {
			 logger.error("No Redmine issue found with key {}", issueKey);
		} catch (RedmineException e) {
			throw new IssueTrackerUpdateException(e.getMessage(), e);
		}
	}

	public List<IssueComment> getCommentsFor(String issueKey)
			throws IssueTrackerUpdateException {
		
		List<IssueComment> issueComments = Collections.emptyList();
		
		try {
			
			Issue issue = getRedmineManager().getIssueById(Integer.parseInt(issueKey), INCLUDE.journals);
			issueComments = convertJournalsToIssueComments(issue.getJournals());
			
		} catch (NumberFormatException e) {
			logger.error("Redmine issue {} is not a valid Redmine issue id.", issueKey);
		} catch (IOException e) {
			throw new IssueTrackerUpdateException(e.getMessage(), e);
		} catch (AuthenticationException e) {
			throw new IssueTrackerUpdateException(e.getMessage(), e);
		} catch (NotFoundException e) {
			 logger.error("No Redmine issue found with key {}", issueKey);
		} catch (RedmineException e) {
			throw new IssueTrackerUpdateException(e.getMessage(), e);
		}
		return issueComments;
	}

	public void updateComment(IssueComment issueComment) {
		//JavaRestAPI does not support update journals. They are working on it.
	}

	public String getStatusFor(String issueKey)
			throws IssueTrackerUpdateException {
			
		String status = null;
		
		try {
			Issue issue = getRedmineManager().getIssueById(Integer.parseInt(issueKey), INCLUDE.journals);
			return issue.getStatusName();
		} catch (NumberFormatException e) {
			logger.error("Redmine issue {} is not a valid Redmine issue id.", issueKey);
		} catch (IOException e) {
			throw new IssueTrackerUpdateException(e.getMessage(), e);
		} catch (AuthenticationException e) {
			throw new IssueTrackerUpdateException(e.getMessage(), e);
		} catch (NotFoundException e) {
			 logger.error("No Redmine issue found with key {}", issueKey);
		} catch (RedmineException e) {
			throw new IssueTrackerUpdateException(e.getMessage(), e);
		}
		
		return status;
	}

	public void doTransition(String issueKey, String status)
			throws IssueTrackerUpdateException {
		try {
			
			Issue issue = getRedmineManager().getIssueById(Integer.parseInt(issueKey));
			
			IssueStatus statusCode = getStatusCode(status);
			if(statusCode!=null) {
				issue.setStatusId(statusCode.getId());
				issue.setStatusName(statusCode.getName());
				getRedmineManager().updateIssue(issue);
			}
			
		} catch (NumberFormatException e) {
			logger.error("Redmine issue {} is not a valid Redmine issue id.", issueKey);
		} catch (IOException e) {
			throw new IssueTrackerUpdateException(e.getMessage(), e);
		} catch (AuthenticationException e) {
			throw new IssueTrackerUpdateException(e.getMessage(), e);
		} catch (NotFoundException e) {
			 logger.error("No Redmine issue found with key {}", issueKey);
		} catch (RedmineException e) {
			throw new IssueTrackerUpdateException(e.getMessage(), e);
		}
	}

	private IssueStatus getStatusCode(String status) {
		//Redmine Rest API does not support getting available status depending on current issue status. So all statuses are eligibles.
		return this.issueStatusesFactory.getIssueStatus(status);
	}
	
	public String getRedmineApiAccessKey() {
		return getConfiguration().getApiAccessKey();
	}
	
	public String getRedmineUrl() {
		return getConfiguration().getRedmineUrl();
	}
	
	public String getRedmineUser() {
		return getConfiguration().getRedmineUser();
	}
	
	public String getRedminePassword() {
		return getConfiguration().getRedminePassword();
	}
	
	@Override
	public String toString() {
		return "Connection to Redmine instance at " + getRedmineUrl();
	}
	
	private List<IssueComment> convertJournalsToIssueComments(List<Journal> journals) {
	
		List<IssueComment> issueComments = new ArrayList<IssueComment>();
		
		for (Journal journal : journals) {
			issueComments.add(new IssueComment((long)journal.getId(), journal.getNotes(), journal.getUser().getFullName()));
		}
		
		return issueComments;
		
	}
	
	private RedmineManager getRedmineManager() {
		if(this.redmineManager == null) {
			if(StringUtils.hasText(getRedmineApiAccessKey())) {
				redmineManager = new RedmineManager(getRedmineUrl(), getRedmineApiAccessKey());
			} else {
				redmineManager = new RedmineManager(getRedmineUrl());
				redmineManager.setLogin(getRedmineUser());
				redmineManager.setPassword(getRedminePassword());
			}
		}
		return redmineManager;
	}
	
	
	
}
