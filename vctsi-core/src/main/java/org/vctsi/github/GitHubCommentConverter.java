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
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHUser;
import org.vctsi.internal.its.CommentConverter;
import org.vctsi.internal.its.IssueComment;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

public class GitHubCommentConverter implements CommentConverter<GHIssueComment, GHIssue> {
    /**
     * converts a GHIssueComment to an IssueComment
     *
     * @param ghIssueComment a comment that shall be converted
     * @return a IssueComment representation of the parameter
     */
    @Override
    public IssueComment convertToIssueComment(GHIssueComment ghIssueComment) {
        LocalDateTime creationDate;
        try {
            creationDate = LocalDateTime.ofInstant(ghIssueComment.getCreatedAt().toInstant(), ZoneOffset.ofHours(2));
        } catch (IOException e) {
            creationDate = null;
        }
        return new IssueComment(
                ghIssueComment.getId(),
                getAuthor(ghIssueComment, null),
                ghIssueComment.getBody(),
                creationDate
        );
    }

    /**
     * get the id of the comment
     *
     * @param ghIssueComment the comment object
     * @param ghIssue        issue containing the object (can be null)
     * @return the id of the comment
     */
    @Override
    public int getCommentId(GHIssueComment ghIssueComment, GHIssue ghIssue) {
        return ghIssueComment.getId();
    }

    /**
     * get the ticket id for the comment
     *
     * @param ghIssueComment the comment object
     * @param ghIssue        issue containing the comment
     * @return id of the ticket containing the comment
     */
    @Override
    public int getTicketId(GHIssueComment ghIssueComment, GHIssue ghIssue) {
        return ghIssue.getId();
    }

    /**
     * get the description of a comment
     *
     * @param ghIssueComment the comment object
     * @param ghIssue        issue containing the object (can be null)
     * @return description/content of the comment
     */
    @Override
    public String getDescription(GHIssueComment ghIssueComment, GHIssue ghIssue) {
        return ghIssueComment.getBody();
    }

    /**
     * get the author of a comment
     *
     * @param ghIssueComment the comment object
     * @param ghIssue        issue containing the object (can be null)
     * @return author of the comment
     */
    @Override
    public String getAuthor(GHIssueComment ghIssueComment, GHIssue ghIssue) {
        try {
            GHUser user = ghIssueComment.getUser();
            if (user != null) {
                return user.getName();
            } else {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * get the date when the comment was created
     *
     * @param ghIssueComment the comment object
     * @param ghIssue        issue containing the object (can be null)
     * @return date when the comment was created or null if an error occured
     */
    @Override
    public Date getCreationDate(GHIssueComment ghIssueComment, GHIssue ghIssue) {
        try {
            return ghIssueComment.getCreatedAt();
        } catch (IOException e) {
            return null;
        }
    }
}
