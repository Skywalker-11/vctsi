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
import java.util.Arrays;

public class VCSSearchParameters {

    private String author;
    private String[] ids;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String startCommit;
    private String endCommit;
    private String message;
    private String ticket;
    private String file;
    private String branch;

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String[] getIds() {
        return ids;
    }

    public void setIds(String[] ids) {
        this.ids = ids;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public String getStartCommit() {
        return startCommit;
    }

    public void setStartCommit(String startCommit) {
        this.startCommit = startCommit;
    }

    public String getEndCommit() {
        return endCommit;
    }

    public void setEndCommit(String endCommit) {
        this.endCommit = endCommit;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String title) {
        this.message = title;
    }

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    @Override
    public String toString() {
        CharSequence[] idStrings = null;
        if (ids != null) {
            idStrings = new String[ids.length];
            for (int i = 0; i < ids.length; i++) {
                idStrings[i] = "" + ids[i];
            }
        }
        return "VCSSearchParameters: "
                + "author " + author + ", "
                + "ids " + (idStrings == null ? null : "[" + String.join(",", idStrings) + "]") + ", "
                + "startDate " + (startDate == null ? null : startDate.toString()) + ", "
                + "endDate " + (endDate == null ? null : endDate.toString()) + ", "
                + "startCommit " + startCommit + ", "
                + "endCommit " + endCommit + ", "
                + "message " + message + ", "
                + "ticket " + ticket + ", "
                + "file " + file + ", "
                + "branch " + branch;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VCSSearchParameters) {
            VCSSearchParameters other = (VCSSearchParameters) obj;
            return (author == null ? other.getAuthor() == null : author.equals(other.getAuthor())) &&
                    (startDate == null ? other.getStartDate() == null : startDate.equals(other.getStartDate())) &&
                    (endDate == null ? other.getEndDate() == null : endDate.equals(other.getEndDate())) &&
                    (startCommit == null ? other.getStartCommit() == null : startCommit.equals(other.getStartCommit())) &&
                    (endCommit == null ? other.getEndCommit() == null : endCommit.equals(other.getEndCommit())) &&
                    (message == null ? other.getMessage() == null : message.equals(other.getMessage())) &&
                    (ticket == null ? other.getTicket() == null : ticket.equals(other.getTicket())) &&
                    (ids == null ? other.getIds() == null : Arrays.equals(ids, other.getIds())) &&
                    (file == null ? other.getFile() == null : file.equals(other.getFile())) &&
                    (branch == null ? other.getBranch() == null : branch.equals(other.getBranch()));
        } else {
            return false;
        }
    }
}
