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

public class IssueComment {
    private int id;
    private String author;
    private String description;
    private LocalDateTime creationDate;

    //needed for json serialization
    @SuppressWarnings("unused")
    public IssueComment() {
    }

    public IssueComment(int id, String author, String description, LocalDateTime creationDate) {
        this.id = id;
        this.author = author;
        this.description = description;
        this.creationDate = creationDate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof IssueComment)) {
            return false;
        } else {
            IssueComment other = (IssueComment) o;
            return id == other.getId() &&
                    (author == null ? other.getAuthor() == null : author.equals(other.getAuthor())) &&
                    (description == null ? other.getDescription() == null : description.equals(other.getDescription())) &&
                    (creationDate == null ? other.getCreationDate() == null : creationDate.equals(other.getCreationDate()));
        }
    }
}
