package org.vctsi.jira;

/*-
 * #%L
 * vctsi-core
 * %%
 * Copyright (C) 2016 - 2017 Michael Pietsch (aka. Skywalker-11)
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.atlassian.jira.rest.client.api.domain.*;
import org.joda.time.DateTime;
import org.vctsi.internal.its.IssueConverter;
import org.vctsi.internal.its.IssueState;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class JiraIssueConverter implements IssueConverter<Issue> {
    /**
     * converts a jira issue to an internal Issue object
     *
     * @param issue an issue that shall be converted
     * @return an internal Issue representation of the parameter
     */
    @Override
    public org.vctsi.internal.its.Issue convertToIssue(Issue issue) {
        return new org.vctsi.internal.its.Issue(
                getTicketId(issue),
                getName(issue),
                getTitle(issue),
                getDescription(issue),
                getAuthor(issue),
                LocalDateTime.ofInstant(getCreationDate(issue).toInstant(), ZoneOffset.ofHours(2)),
                getState(issue),
                getAssignee(issue),
                getTargetVersion(issue),
                getAdditionalData(issue));
    }

    /**
     * get the id of the issue
     *
     * @param issue the issue object
     * @return the id of the issue
     */
    @Override
    public int getTicketId(Issue issue) {
        return -1;
    }

    /**
     * get the name of the issue
     *
     * @param issue the issue object
     * @return the name of the issue
     */
    @Override
    public String getName(Issue issue) {
        return issue.getKey();
    }

    /**
     * get the title of the issue
     *
     * @param issue the issue object
     * @return the title of the issue
     */
    @Override
    public String getTitle(Issue issue) {
        return issue.getSummary();
    }

    /**
     * get the description of the issue
     *
     * @param issue the issue object
     * @return the description of the issue
     */
    @Override
    public String getDescription(Issue issue) {
        return issue.getDescription();
    }

    /**
     * get the author of the issue
     *
     * @param issue the issue object
     * @return the author of the issue
     */
    @Override
    public String getAuthor(Issue issue) {
        return (issue.getReporter() == null ? "" : issue.getReporter().getName());
    }

    /**
     * get the date when the issue was created
     *
     * @param issue the issue object
     * @return issue when the comment was created
     */
    @Override
    public Date getCreationDate(Issue issue) {
        return issue.getCreationDate().toDate();
    }

    /**
     * get the state of the issue
     *
     * @param issue the issue object
     * @return the state of the issue
     */
    @Override
    public String getState(Issue issue) {
        IssueState state = getIssueState(issue.getStatus());
        if (state != null) {
            return state.toString();
        } else {
            return issue.getStatus().toString();
        }
    }

    /**
     * get the assignee of the issue
     *
     * @param issue the issue object
     * @return the assignee of the issue; null if no assignee exists
     */
    @Override
    public String getAssignee(Issue issue) {
        return (issue.getAssignee() == null ? null : issue.getAssignee().getName());
    }

    /**
     * get the target version / milestone of the issue
     *
     * @param issue the issue object
     * @return the target version of the issue;  null if no target version exists
     */
    @Override
    public String getTargetVersion(Issue issue) {
        return getMileStone(issue.getFixVersions());
    }


    /**
     * gets the IssueState for a jira state
     * default states @link https://confluence.atlassian.com/jira064/what-is-an-issue-720416138.html
     *
     * @param status the jira state
     * @return the IssueState representing the jira state or null if no equivalent exists
     */
    private IssueState getIssueState(Status status) {
        switch (status.getName()) {
            case "Open":
                return IssueState.NEW;
            case "In Progress":
                return IssueState.ASSIGNED;
            case "Resolved":
                return IssueState.RESOLVED;
            case "Reopened":
                return IssueState.REOPENED;
            case "Closed":
                return IssueState.SOLVED;
            default:
                return null;
        }
    }

    /**
     * returns the fix versions as comma separated list
     *
     * @param versions jira fix versions
     * @return a string representing the fix versions as comma separated list or null if the parameter is null
     */
    private String getMileStone(Iterable<Version> versions) {
        if (versions == null) {
            return null;
        }
        List<String> versionAsStrings = new ArrayList<>();
        for (Version version : versions) {
            versionAsStrings.add(version.getName());
        }
        return String.join(",", versionAsStrings);
    }

    /**
     * get additional data that is stored in the jira issue
     *
     * @param issue the issue object
     * @return a hashmap containing additional information provided by the issue
     */
    private HashMap<String, Object> getAdditionalData(Issue issue) {
        HashMap<String, Object> additionalData = new HashMap<>();

        if (issue.getLabels() != null && !issue.getLabels().isEmpty()) additionalData.put("labels", issue.getLabels());
        if (issue.getAffectedVersions() != null) {
            List<String> versions = new ArrayList<>();
            for (Version version : issue.getAffectedVersions()) {
                versions.add(version.getName());
            }
            if (!versions.isEmpty()) {
                additionalData.put("affectedVersions", versions);
            }
        }
        if (issue.getAttachments() != null) {
            List<HashMap<String, Object>> attachments = new ArrayList<>();
            for (Attachment attachment : issue.getAttachments()) {
                HashMap<String, Object> attachmentM = new HashMap<>();
                attachmentM.put("filename", attachment.getFilename());
                attachmentM.put("author", attachment.getAuthor().getName());
                attachmentM.put("uri", attachment.getContentUri().getPath());
                attachmentM.put("mimeType", attachment.getMimeType());
                attachments.add(attachmentM);
            }
            if (!attachments.isEmpty()) {
                additionalData.put("attachments", attachments);
            }
        }
        if (issue.getDueDate() != null) additionalData.put("dueDate", issue.getDueDate());
        if (issue.getUpdateDate() != null) additionalData.put("updateDate", issue.getUpdateDate());
        if (issue.getComponents() != null) {
            List<String> components = new ArrayList<>();
            for (BasicComponent component : issue.getComponents()) {
                components.add(component.getName());
            }
            if (!components.isEmpty()) {
                additionalData.put("components", components);
            }
        }
        if (issue.getVotes() != null) additionalData.put("votes", issue.getVotes().getVotes());
        if (issue.getFields() != null) {
            HashMap<String, Object> fields = new HashMap<>();
            for (IssueField field : issue.getFields()) {
                if (field.getValue() != null) {
                    //if the value is not of the following types we probably can't print it out. so we will only use such
                    //values
                    if (field.getValue() instanceof String || field.getValue() instanceof DateTime || field.getValue() instanceof Number) {
                        fields.put(field.getName(), field.getValue());
                    }
                }
            }
            if (!fields.isEmpty()) {
                additionalData.put("fields", fields);
            }
        }
        if (issue.getSubtasks() != null) {
            List<String> subtasks = new ArrayList<>();
            for (Subtask subtask : issue.getSubtasks()) {
                subtasks.add(subtask.getIssueKey());
            }
            if (!subtasks.isEmpty()) {
                additionalData.put("fields", subtasks);
            }
        }
        if (issue.getWatchers() != null) {
            additionalData.put("numWatchers", issue.getWatchers().getNumWatchers());
        }

        return additionalData;
    }
}
