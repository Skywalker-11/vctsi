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
import org.gitlab.api.models.GitlabNote;
import org.gitlab.api.models.GitlabUser;
import org.vctsi.internal.its.CommentConverter;
import org.vctsi.internal.its.IssueComment;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

public class GitLabCommentConverter implements CommentConverter<GitlabNote, GitlabIssue> {

    /**
     * converts a GitlabNote to an IssueComment
     *
     * @param gitlabNote a comment that shall be converted
     * @return a IssueComment representation of the parameter
     */
    @Override
    public IssueComment convertToIssueComment(GitlabNote gitlabNote) {
        return new IssueComment(
                gitlabNote.getId(),
                getAuthor(gitlabNote, null),
                gitlabNote.getBody(),
                LocalDateTime.ofInstant(gitlabNote.getCreatedAt().toInstant(), ZoneOffset.ofHours(2))
        );
    }

    /**
     * get the id of the comment
     *
     * @param gitlabNote the comment object of gitlab
     * @param issue      issue containing the object (can be null)
     * @return the id of the comment
     */
    @Override
    public int getCommentId(GitlabNote gitlabNote, GitlabIssue issue) {
        return gitlabNote.getId();
    }

    /**
     * get the id of the ticket for the comment
     *
     * @param gitlabNote the comment object of gitlab
     * @param issue      issue containing the commentf
     * @return id of the ticket containing the comment
     */
    @Override
    public int getTicketId(GitlabNote gitlabNote, GitlabIssue issue) {
        return issue.getId();
    }

    /**
     * get the description of a comment
     *
     * @param gitlabNote the comment object of gitlab
     * @param issue      issue containing the object (can be null)
     * @return description/content of the comment
     */
    @Override
    public String getDescription(GitlabNote gitlabNote, GitlabIssue issue) {
        return gitlabNote.getBody();
    }

    /**
     * get the author of a comment
     *
     * @param gitlabNote the comment object of gitlab
     * @param issue      issue containing the object (can be null)
     * @return name of the author of the comment; null if the author con not be accessed
     */
    @Override
    public String getAuthor(GitlabNote gitlabNote, GitlabIssue issue) {
        GitlabUser author = gitlabNote.getAuthor();
        if (author != null) {
            return author.getName();
        }
        return null;
    }

    /**
     * get the date the comment was created
     *
     * @param gitlabNote the comment object
     * @param issue      issue containing the object (can be null)
     * @return date the comment was created
     */
    @Override
    public Date getCreationDate(GitlabNote gitlabNote, GitlabIssue issue) {
        return gitlabNote.getCreatedAt();
    }
}
