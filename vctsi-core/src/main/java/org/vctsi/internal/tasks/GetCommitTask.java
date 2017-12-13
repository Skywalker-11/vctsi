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

public class GetCommitTask extends Task {

    private String commitId;

    public GetCommitTask(String commitId) {
        super(TaskTarget.COMMIT, TaskType.GET);
        this.commitId = commitId;
    }

    public String getCommitId() {
        return commitId;
    }

    @Override
    public String toString() {
        return "GetCommitTask: for " + commitId;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof GetCommitTask) {
            GetCommitTask other = (GetCommitTask) obj;
            return (commitId == null ? other.getCommitId() == null : commitId.equals(other.getCommitId()));
        } else {
            return false;
        }
    }
}
