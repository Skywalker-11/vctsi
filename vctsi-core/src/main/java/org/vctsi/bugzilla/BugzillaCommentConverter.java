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
import com.j2bugzilla.base.Comment;
import org.vctsi.internal.its.CommentConverter;
import org.vctsi.internal.its.IssueComment;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

public class BugzillaCommentConverter implements CommentConverter<Comment, Bug> {
    /**
     * converts a Comment to an IssueComment
     *
     * @param comment a comment that shall be converted
     * @return a IssueComment representation of the parameter
     */
    @Override
    public IssueComment convertToIssueComment(Comment comment) {
        return new IssueComment(
                comment.getID(),
                comment.getCreator(),
                comment.getText(),
                (comment.getCreationTime() == null
                        ? null
                        : LocalDateTime.ofInstant(comment.getCreationTime().toInstant(), ZoneOffset.ofHours(2))
                )
        );
    }

    /**
     * get the id of the comment
     *
     * @param comment the comment object
     * @param bug     bug containing the comment (can be null)
     * @return the id of the comment
     */
    @Override
    public int getCommentId(Comment comment, Bug bug) {
        return comment.getID();
    }

    /**
     * get the ticket id for the comment
     *
     * @param comment the comment object
     * @param bug     bug containing the comment
     * @return id of the ticket containing the comment
     */
    @Override
    public int getTicketId(Comment comment, Bug bug) {
        return bug.getID();
    }

    /**
     * get the description of a comment
     *
     * @param comment the comment object
     * @param bug     bug containing the comment (can be null)
     * @return description/content of the comment
     */
    @Override
    public String getDescription(Comment comment, Bug bug) {
        return comment.getText();
    }

    /**
     * get the author of a comment
     *
     * @param comment the comment object
     * @param bug     bug containing the comment (can be null)
     * @return author of the comment
     */
    @Override
    public String getAuthor(Comment comment, Bug bug) {
        return comment.getCreator();
    }

    /**
     * get the date when the comment was created
     *
     * @param comment the comment object
     * @param bug     bug containing the comment (can be null)
     * @return date when the comment was created
     */
    @Override
    public Date getCreationDate(Comment comment, Bug bug) {
        return comment.getCreationTime();
    }
}
