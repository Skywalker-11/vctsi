package org.vctsi.github;

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

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHMilestone;
import org.kohsuke.github.GHUser;
import org.vctsi.internal.its.Issue;
import org.vctsi.internal.its.IssueState;
import org.vctsi.internal.its.IssueConverter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class GitHubIssueConverter implements IssueConverter<GHIssue> {
    /**
     * converts a GHIssue to an internal Issue object
     *
     * @param ghIssue an issue that shall be converted
     * @return an internal Issue representation of the parameter
     */
    @Override
    public Issue convertToIssue(GHIssue ghIssue) {
        HashMap<String, Object> additionalData = new HashMap<>();
        try {
            if (ghIssue.getLabels() != null) {
                List<HashMap<String, String>> labels = new ArrayList<>();
                for (GHLabel label : ghIssue.getLabels()) {
                    HashMap<String, String> labelM = new HashMap<>();
                    labelM.put("color", label.getColor());
                    labelM.put("name", label.getName());
                    labelM.put("url", label.getUrl());
                    labels.add(labelM);
                }
                additionalData.put("labels", labels);
            }
            if (ghIssue.getClosedAt() != null) additionalData.put("closedAt", ghIssue.getClosedAt());
            if (ghIssue.getClosedBy() != null) additionalData.put("closedBy", ghIssue.getClosedBy().getName());
            if (ghIssue.getRepository() != null) additionalData.put("repository", ghIssue.getRepository().getName());
        } catch (IOException e) {
            additionalData = null;
        }
        return new Issue(
                getTicketId(ghIssue),
                getName(ghIssue),
                getTitle(ghIssue),
                getDescription(ghIssue),
                getAuthor(ghIssue),
                LocalDateTime.ofInstant(getCreationDate(ghIssue).toInstant(), ZoneOffset.ofHours(2)),
                getState(ghIssue),
                getAssignee(ghIssue),
                getTargetVersion(ghIssue),
                additionalData
        );
    }

    /**
     * get the id of the issue
     *
     * @param ghIssue the issue object
     * @return the id of the issue
     */
    @Override
    public int getTicketId(GHIssue ghIssue) {
        //the issue contains an internal id but that will never be used neither from the api nor will it be shown to the
        //user anywhere so we use the project specific number only to reduce confusions
        return ghIssue.getNumber();
    }

    /**
     * get the name of the issue
     *
     * @param ghIssue the issue object
     * @return the name of the issue
     */
    @Override
    public String getName(GHIssue ghIssue) {
        return "" + ghIssue.getNumber();
    }

    /**
     * get the title of the issue
     *
     * @param ghIssue the issue object
     * @return the title of the issue
     */
    @Override
    public String getTitle(GHIssue ghIssue) {
        return ghIssue.getTitle();
    }

    /**
     * get the description of the issue
     *
     * @param ghIssue the issue object
     * @return the description of the issue
     */
    @Override
    public String getDescription(GHIssue ghIssue) {
        return ghIssue.getBody();
    }

    /**
     * get the author of the issue
     *
     * @param ghIssue the issue object
     * @return the author of the issue
     */
    @Override
    public String getAuthor(GHIssue ghIssue) {
        try {
            return ghIssue.getUser().getName();
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * get the date when the issue was created
     *
     * @param ghIssue the issue object
     * @return issue when the comment was created
     */
    @Override
    public Date getCreationDate(GHIssue ghIssue) {
        try {
            return ghIssue.getCreatedAt();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * get the state of the issue
     *
     * @param ghIssue the issue object
     * @return the state of the issue
     */
    @Override
    public String getState(GHIssue ghIssue) {
        IssueState issueState = getIssueState(ghIssue);
        if (issueState != null) {
            return issueState.toString();
        } else {
            return "";
        }
    }

    /**
     * gets the IssueState for an issue
     *
     * @param ghIssue the issue object
     * @return the IssueState representing the state of the issue or null if no equivalent exists
     */
    private IssueState getIssueState(GHIssue ghIssue) {
        switch (ghIssue.getState()) {
            case OPEN:
                if (ghIssue.getAssignee() != null) {
                    return IssueState.ASSIGNED;
                }
                return IssueState.NEW;
            case CLOSED:
                return IssueState.SOLVED;
            default:
                return null;
        }
    }

    /**
     * get the assignee of the issue
     *
     * @param ghIssue the issue object
     * @return the assignee of the issue; null if no assignee exists
     */
    @Override
    public String getAssignee(GHIssue ghIssue) {
        try {
            GHUser assignee = ghIssue.getAssignee();
            return (assignee == null ? "" : assignee.getName());
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * get the target version / milestone of the issue
     *
     * @param ghIssue the issue object
     * @return the target version of the issue;  null if no target version exists
     */
    @Override
    public String getTargetVersion(GHIssue ghIssue) {
        GHMilestone milestone = ghIssue.getMilestone();
        return (milestone == null ? null : milestone.getTitle());
    }
}
