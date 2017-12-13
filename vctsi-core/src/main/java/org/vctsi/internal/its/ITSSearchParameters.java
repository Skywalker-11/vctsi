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
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class ITSSearchParameters {

    private String author;
    private String[] names;
    private String assignee;
    private Integer[] ids;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String title;
    private String description;
    private String commit;
    private String commitPrefix = "";
    private String commitSuffix = "";
    private String state;
    private String targetVersion;

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public Integer[] getIds() {
        return ids;
    }

    public void setIds(Integer[] ids) {
        this.ids = ids;
    }

    public String[] getNames() {
        return names;
    }

    public void setNames(String[] names) {
        this.names = names;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCommit() {
        return commit;
    }

    public void setCommit(String commit) {
        this.commit = commit;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCommitPrefix() {
        return commitPrefix;
    }

    public void setCommitPrefix(String commitPrefix) {
        this.commitPrefix = commitPrefix;
    }

    public String getCommitSuffix() {
        return commitSuffix;
    }

    public void setCommitSuffix(String commitSuffix) {
        this.commitSuffix = commitSuffix;
    }

    public String getTargetVersion() {
        return targetVersion;
    }

    public void setTargetVersion(String targetVersion) {
        this.targetVersion = targetVersion;
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
        return "ITSSearchParameters: "
                + "author " + author + ", "
                + "assignee " + assignee + ", "
                + "ids " + (idStrings == null ? null : "[" + String.join(",", idStrings) + "]") + ", "
                + "names [" + (names == null ? null : String.join(",", (CharSequence[]) names)) + "], "
                + "startDate " + (startDate == null ? null : startDate.format(DateTimeFormatter.ISO_DATE_TIME)) + ", "
                + "endDate " + (endDate == null ? null : endDate.format(DateTimeFormatter.ISO_DATE_TIME)) + ", "
                + "title " + title + ", "
                + "description " + description + ", "
                + "commit " + commit + ", "
                + "targetVersion " + targetVersion;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ITSSearchParameters) {
            ITSSearchParameters other = (ITSSearchParameters) obj;
            return (author == null ? other.getAuthor() == null : author.equals(other.getAuthor()))
                    && (assignee == null ? other.getAssignee() == null : assignee.equals(other.getAssignee()))
                    && Arrays.equals(other.getIds(), ids)
                    && Arrays.equals(other.getNames(), names)
                    && (startDate == null ? other.getStartDate() == null : startDate.compareTo(other.getStartDate()) == 0)
                    && (endDate == null ? other.getEndDate() == null : endDate.compareTo(other.getEndDate()) == 0)
                    && (title == null ? other.getTitle() == null : title.equals(other.getTitle()))
                    && (description == null ? other.getDescription() == null : description.equals(other.getDescription()))
                    && (commit == null ? other.getCommit() == null : commit.equals(other.getCommit()))
                    && (targetVersion == null ? other.getTargetVersion() == null : targetVersion.equals(other.getTargetVersion()));

        }
        return false;
    }
}
