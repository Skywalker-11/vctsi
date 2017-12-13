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

import org.vctsi.internal.vcs.VCSDiffSettings;

public class GetDiffsTask extends Task {
    private VCSDiffSettings diffSettings;

    public GetDiffsTask() {
        super(TaskTarget.COMMIT, TaskType.DIFF);
    }

    public void setDiffSettings(VCSDiffSettings diffSettings) {
        this.diffSettings = diffSettings;
    }

    public VCSDiffSettings getDiffSettings() {
        return diffSettings;
    }

    @Override
    public String toString() {
        return "GetDiffsTask: for " + diffSettings.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GetDiffsTask) {
            GetDiffsTask other = (GetDiffsTask) obj;
            return (diffSettings == null ? other.getDiffSettings() == null : diffSettings.equals(other.getDiffSettings()));
        } else {
            return false;
        }
    }
}