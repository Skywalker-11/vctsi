package org.vctsi.internal.vcs;

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
import java.util.AbstractList;

/**
 * Represents a commit
 */
public class Commit {

    private String id;
    private AbstractList<String> branches;
    private LocalDateTime dateTime;
    private String author;
    private String message;
    private AbstractList<FileChange> changedFiles;
    private Diff diff;


    //needed for json serialization
    @SuppressWarnings("unused")
    public Commit() {
    }

    public Commit(String id, String message, String author, LocalDateTime dateTime) {
        this.id = id;
        this.message = message;
        this.author = author;
        this.dateTime = dateTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public AbstractList<String> getBranches() {
        return branches;
    }

    public void setBranches(AbstractList<String> branches) {
        this.branches = branches;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public Diff getDiff() {
        return diff;
    }

    public void setDiff(Diff diff) {
        this.diff = diff;
    }

    public void setChangedFiles(AbstractList<FileChange> changedFiles) {
        this.changedFiles = changedFiles;
    }

    public AbstractList<FileChange> getChangedFiles() {
        return changedFiles;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Commit) {
            Commit other = (Commit) obj;
            return (id == null ? other.getId() == null : id.equals(other.getId())) &&
                    (author == null ? other.getAuthor() == null : author.equals(other.getAuthor())) &&
                    (message == null ? other.getMessage() == null : message.equals(other.getMessage())) &&
                    (branches == null ? other.getBranches() == null : branches.equals(other.getBranches())) &&
                    (dateTime == null ? other.getDateTime() == null : dateTime.equals(other.getDateTime())) &&
                    (diff == null ? other.getDiff() == null : diff.equals(other.getDiff())) &&
                    (changedFiles == null ? other.getChangedFiles() == null : changedFiles.equals(other.getChangedFiles())) &&
                    (branches == null ? other.getBranches() == null : branches.equals(other.getBranches()));
        } else {
            return false;
        }
    }
}
