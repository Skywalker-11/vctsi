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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Issue {

    private int id;
    private String title;
    private String name;
    private String description;
    private String author;
    private LocalDateTime creationDate;
    private String state;
    private String assignee;
    private String targetVersion;
    private List<IssueComment> comments = new ArrayList<>();
    private HashMap<String, Object> additionalData;

    //needed for json serialization
    @SuppressWarnings("unused")
    public Issue() {
    }

    public Issue(int id, String name, String title, String description, String author, LocalDateTime creationDate,
                 String state, String assignee, String targetVersion) {
        this.id = id;
        this.name = name;
        this.title = title;
        this.description = description;
        this.author = author;
        this.creationDate = creationDate;
        this.state = state;
        this.assignee = assignee;
        this.targetVersion = targetVersion;
    }

    public Issue(int id, String name, String title, String description, String author, LocalDateTime creationDate,
                 String state, String assignee, String targetVersion, HashMap<String, Object> additionalData) {
        this.id = id;
        this.name = name;
        this.title = title;
        this.description = description;
        this.author = author;
        this.creationDate = creationDate;
        this.state = state;
        this.assignee = assignee;
        this.targetVersion = targetVersion;
        if (!additionalData.isEmpty()) {
            this.additionalData = additionalData;
        }
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getAuthor() {
        return author;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public String getState() {
        return state;
    }

    public String getAssignee() {
        return assignee;
    }

    public String getTargetVersion() {
        return targetVersion;
    }

    public List<IssueComment> getComments() {
        return comments;
    }

    public void setComments(List<IssueComment> comments) {
        this.comments = comments;
    }

    public HashMap<String, Object> getAdditionalData() {
        return additionalData;
    }

   @Override
    public boolean equals(Object o) {
        if (!(o instanceof Issue)) {
            return false;
        } else {
            Issue other = (Issue) o;
            return id == other.getId() &&
                    (name == null ? other.getName() == null : name.equals(other.getName())) &&
                    (description == null ? other.getDescription() == null : description.equals(other.getDescription())) &&
                    (author == null ? other.getAuthor() == null : author.equals(other.getAuthor())) &&
                    (creationDate == null ? other.getCreationDate() == null : creationDate.equals(other.creationDate)) &&
                    (state == null ? other.getState() == null : state.equals(other.getState()))&&
                    (assignee == null ? other.getAssignee() == null : assignee.equals(other.assignee)) &&
                    (targetVersion == null ? other.getTargetVersion() == null : targetVersion.equals(other.targetVersion)) &&
                    (comments == null ? other.getComments() == null : comments.equals(other.getComments())) &&
                    (additionalData == null ? other.getAdditionalData() == null : additionalData.equals(other.getAdditionalData()));
        }
    }
}
