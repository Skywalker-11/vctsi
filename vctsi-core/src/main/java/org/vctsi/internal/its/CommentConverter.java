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

/** *
 * @param <COMMENT> the class of a comment that can be processed by this class
 * @param <ISSUE> the class of an issue that can be used to get additional information about the comment
 */
public interface CommentConverter<COMMENT, ISSUE> {

    /**
     * converts a COMMENT to an IssueComment
     *
     * @param comment a comment that shall be converted
     * @return a IssueComment representation of the parameter
     */
    IssueComment convertToIssueComment(COMMENT comment);

    /**
     * get the id of the comment
     *
     * @param comment the comment object
     * @param issue   issue containing the object (can be null)
     * @return the id of the comment
     */
    int getCommentId(COMMENT comment, ISSUE issue);

    /**
     * get the ticket id for the comment
     *
     * @param comment the comment object
     * @param issue   issue containing the comment
     * @return id of the ticket containing the comment
     */
    int getTicketId(COMMENT comment, ISSUE issue);

    /**
     * get the description of a comment
     *
     * @param comment the comment object
     * @param issue   issue containing the object (can be null)
     * @return description/content of the comment
     */
    String getDescription(COMMENT comment, ISSUE issue);

    /**
     * get the author of a comment
     *
     * @param comment the comment object
     * @param issue   issue containing the object (can be null)
     * @return author of the comment
     */
    String getAuthor(COMMENT comment, ISSUE issue);

    /**
     * get the date when the comment was created
     *
     * @param comment the comment object
     * @param issue   issue containing the object (can be null)
     * @return date when the comment was created
     */
    Date getCreationDate(COMMENT comment, ISSUE issue);
}
