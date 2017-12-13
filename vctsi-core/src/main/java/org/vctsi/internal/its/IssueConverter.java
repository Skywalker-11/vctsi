package org.vctsi.internal.its;

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

import java.util.Date;


/**
 * @param <ISSUE> the class of an issue that can be used to get additional information about the comment
 */
public interface IssueConverter<ISSUE> {

    /**
     * converts an ISSUE to an internal Issue object
     *
     * @param issue an issue that shall be converted
     * @return an internal Issue representation of the parameter
     */
    Issue convertToIssue(ISSUE issue);

    /**
     * get the id of the issue
     *
     * @param issue the issue object
     * @return the id of the issue
     */
    int getTicketId(ISSUE issue);

    /**
     * get the name of the issue
     *
     * @param issue the issue object
     * @return the name of the issue
     */
    String getName(ISSUE issue);

    /**
     * get the title of the issue
     *
     * @param issue the issue object
     * @return the title of the issue
     */
    String getTitle(ISSUE issue);

    /**
     * get the description of the issue
     *
     * @param issue the issue object
     * @return the description of the issue
     */
    String getDescription(ISSUE issue);

    /**
     * get the author of the issue
     *
     * @param issue the issue object
     * @return the author of the issue
     */
    String getAuthor(ISSUE issue);

    /**
     * get the date when the issue was created
     *
     * @param issue the issue object
     * @return issue when the comment was created
     */
    Date getCreationDate(ISSUE issue);

    /**
     * get the state of the issue
     *
     * @param issue the issue object
     * @return the state of the issue
     */
    String getState(ISSUE issue);

    /**
     * get the assignee of the issue
     *
     * @param issue the issue object
     * @return the assignee of the issue; null if no assignee exists
     */
    String getAssignee(ISSUE issue);

    /**
     * get the target version / milestone of the issue
     *
     * @param issue the issue object
     * @return the target version of the issue;  null if no target version exists
     */
    String getTargetVersion(ISSUE issue);
}
