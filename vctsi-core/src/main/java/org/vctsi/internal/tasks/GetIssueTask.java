package org.vctsi.internal.tasks;

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

import org.vctsi.internal.its.ITSSettings;

public class GetIssueTask extends Task {

    private int issueId = -1;
    private ITSSettings settings;

    public GetIssueTask(int issueId) {
        super(TaskTarget.ISSUE, TaskType.GET);
        this.issueId = issueId;
        this.settings = new ITSSettings();
    }

    public ITSSettings getSettings() {
        return settings;
    }

    public void setSettings(ITSSettings settings) {
        this.settings = settings;
    }

    public int getIssueId() {
        return issueId;
    }

    public void setIssueId(int issueId) {
        this.issueId = issueId;
    }

    @Override
    public String toString() {
        return "GetIssueTask: for " + issueId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GetIssueTask) {
            GetIssueTask other = (GetIssueTask) obj;
            return (issueId == other.getIssueId()) &&
                    (settings == null ? other.getSettings() == null : settings.equals(other.getSettings()));
        } else {
            return false;
        }
    }
}
