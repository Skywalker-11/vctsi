package org.vctsi.gitlab;

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

import org.gitlab.api.models.GitlabIssue;
import org.gitlab.api.models.GitlabMilestone;
import org.gitlab.api.models.GitlabUser;
import org.vctsi.internal.its.Issue;
import org.vctsi.internal.its.IssueConverter;
import org.vctsi.internal.its.IssueState;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;

public class GitLabIssueConverter implements IssueConverter<GitlabIssue> {
    /**
     * converts a gitlab issue to an internal Issue object
     *
     * @param gitlabIssue an issue that shall be converted
     * @return an internal Issue representation of the parameter
     */
    @Override
    public Issue convertToIssue(GitlabIssue gitlabIssue) {
        HashMap<String, Object> additionalData = new HashMap<>();
        if (gitlabIssue.getLabels() != null) additionalData.put("labels", gitlabIssue.getLabels());
        if (gitlabIssue.getUpdatedAt() != null)
            additionalData.put("updatedAt", LocalDateTime.ofInstant(gitlabIssue.getUpdatedAt().toInstant(), ZoneOffset.ofHours(2)));
        return new Issue(
                getTicketId(gitlabIssue),
                getName(gitlabIssue),
                getTitle(gitlabIssue),
                getDescription(gitlabIssue),
                getAuthor(gitlabIssue),
                LocalDateTime.ofInstant(getCreationDate(gitlabIssue).toInstant(), ZoneOffset.ofHours(2)),
                getState(gitlabIssue),
                getAssignee(gitlabIssue),
                getTargetVersion(gitlabIssue),
                additionalData
        );
    }

    /**
     * get the id of the issue
     *
     * @param gitlabIssue the issue object
     * @return the id of the issue
     */
    @Override
    public int getTicketId(GitlabIssue gitlabIssue) {
        return gitlabIssue.getId();
    }

    /**
     * get the name of the issue
     *
     * @param gitlabIssue the issue object
     * @return the name of the issue
     */
    @Override
    public String getName(GitlabIssue gitlabIssue) {
        return "" + gitlabIssue.getIid();
    }

    /**
     * get the title of the issue
     *
     * @param gitlabIssue the issue object
     * @return the title of the issue
     */
    @Override
    public String getTitle(GitlabIssue gitlabIssue) {
        return gitlabIssue.getTitle();
    }

    /**
     * get the description of the issue
     *
     * @param gitlabIssue the issue object
     * @return the description of the issue
     */
    @Override
    public String getDescription(GitlabIssue gitlabIssue) {
        return gitlabIssue.getDescription();
    }

    /**
     * get the author of the issue
     *
     * @param gitlabIssue the issue object
     * @return the author of the issue
     */
    @Override
    public String getAuthor(GitlabIssue gitlabIssue) {

        GitlabUser author = gitlabIssue.getAuthor();
        if (author != null) {
            return author.getName();
        }
        return null;
    }

    /**
     * get the date when the issue was created
     *
     * @param gitlabIssue the issue object
     * @return issue when the comment was created
     */
    @Override
    public Date getCreationDate(GitlabIssue gitlabIssue) {
        return gitlabIssue.getCreatedAt();
    }

    /**
     * get the state of the issue
     *
     * @param gitlabIssue the issue object
     * @return the state of the issue
     */
    @Override
    public String getState(GitlabIssue gitlabIssue) {
        IssueState state = getIssueState(gitlabIssue);
        if (state != null) {
            return state.toString();
        } else {
            return gitlabIssue.getState();
        }
    }

    /**
     * get the assignee of the issue
     *
     * @param gitlabIssue the issue object
     * @return the assignee of the issue; null if no assignee exists
     */
    @Override
    public String getAssignee(GitlabIssue gitlabIssue) {
        GitlabUser assignee = gitlabIssue.getAssignee();
        if (assignee != null) {
            return assignee.getName();
        }
        return null;
    }

    /**
     * get the target version / milestone of the issue
     *
     * @param gitlabIssue the issue object
     * @return the target version of the issue;  null if no target version exists
     */
    @Override
    public String getTargetVersion(GitlabIssue gitlabIssue) {
        GitlabMilestone milestone = gitlabIssue.getMilestone();
        if (milestone != null) {
            return milestone.getTitle();
        }
        return null;
    }

    /**
     * gets the IssueState for the state of a gitlab issue
     *
     * @param issue the issue object
     * @return the IssueState representing the gitlab issues state or null if no equivalent exists
     */
    private IssueState getIssueState(GitlabIssue issue) {
        switch (issue.getState()) {
            case GitlabIssue.STATE_CLOSED:
                return IssueState.SOLVED;
            case GitlabIssue.STATE_OPENED:
                if (issue.getAssignee() != null) {
                    return IssueState.ASSIGNED;
                } else {
                    return IssueState.NEW;
                }
            default:
                return null;
        }
    }
}
