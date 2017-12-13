package org.vctsi.bugzilla;

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

import com.j2bugzilla.base.Bug;
import org.vctsi.internal.its.Issue;
import org.vctsi.internal.its.IssueConverter;
import org.vctsi.internal.its.IssueState;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

public class BugzillaIssueConverter implements IssueConverter<Bug> {
    /**
     * converts a Bug to an internal Issue object
     *
     * @param bug a bug that shall be converted
     * @return an internal Issue representation of the parameter
     */
    @Override
    public Issue convertToIssue(Bug bug) {
        bug.getParameterMap();
        return new Issue(
                getTicketId(bug),
                getName(bug),
                getTitle(bug),
                getDescription(bug),
                getAuthor(bug),
                LocalDateTime.ofInstant(getCreationDate(bug).toInstant(), ZoneOffset.ofHours(2)),
                getState(bug),
                getAssignee(bug),
                getTargetVersion(bug)
        );
    }

    /**
     * get the id of the issue
     *
     * @param bug the issue object
     * @return the id of the issue
     */
    @Override
    public int getTicketId(Bug bug) {
        return bug.getID();
    }

    /**
     * get the name of the issue
     *
     * @param bug the issue object
     * @return the name of the issue
     */
    @Override
    public String getName(Bug bug) {
        return "" + getTicketId(bug);
    }

    /**
     * get the title of the issue
     *
     * @param bug the issue object
     * @return the title of the issue
     */
    @Override
    public String getTitle(Bug bug) {
        return bug.getSummary();
    }

    /**
     * get the description of the issue
     *
     * @param bug the issue object
     * @return the description of the issue
     */
    @Override
    public String getDescription(Bug bug) {
        return bug.getSummary();
    }

    /**
     * get the author of the issue
     *
     * @param bug the issue object
     * @return the author of the issue
     */
    @Override
    public String getAuthor(Bug bug) {
        return bug.getCreator();
    }

    /**
     * get the date when the issue was created
     *
     * @param bug the issue object
     * @return issue when the comment was created
     */
    @Override
    public Date getCreationDate(Bug bug) {
        return bug.getCreationTime();
    }

    /**
     * get the state of the issue
     *
     * @param bug the issue object
     * @return the state of the issue
     */
    @Override
    public String getState(Bug bug) {
        IssueState state = getIssueState(bug.getStatus());
        if (state != null) {
            return state.toString();
        } else {
            return bug.getStatus();
        }
    }

    private IssueState getIssueState(String state) {
        switch (state) {
            case "NEW":
                return IssueState.NEW;
            case "ASSIGNED":
                return IssueState.ASSIGNED;
            case "SOLVED":
                return IssueState.SOLVED;
            case "REOPENED":
                return IssueState.REOPENED;
            case "CONFIRMED":
                return IssueState.CONFIRMED;
            case "UNCONFIRMED":
                return IssueState.UNCONFIRMED;
            case "VERIFIED":
                return IssueState.VERIFIED;
        }
        return null;
    }

    /**
     * get the assignee of the issue
     *
     * @param bug the issue object
     * @return the assignee of the issue; null if no assignee exists
     */
    @Override
    public String getAssignee(Bug bug) {
        return bug.getAssignee();
    }

    /**
     * get the target version / milestone of the issue
     *
     * @param bug the issue object
     * @return the target version of the issue;  null if no target version exists
     */
    @Override
    public String getTargetVersion(Bug bug) {
        return bug.getTargetVersion();
    }
}
