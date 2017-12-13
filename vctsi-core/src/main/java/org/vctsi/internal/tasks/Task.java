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

import org.vctsi.internal.its.ITSModule;
import org.vctsi.internal.vcs.VCSDiffSettings;
import org.vctsi.internal.vcs.VCSModule;

public abstract class Task {

    protected final TaskType taskType;
    protected final TaskTarget taskTarget;
    private Class<? extends VCSModule> vcsModule;
    private Class<? extends ITSModule> itsModule;

    public Task(TaskTarget taskTarget, TaskType taskType) {
        this.taskTarget = taskTarget;
        this.taskType = taskType;
    }

    public enum TaskType {
        IMPORT,
        GET,
        DIFF, SEARCH
    }

    public enum TaskTarget {
        ISSUE,
        COMMIT
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public TaskTarget getTaskTarget() {
        return taskTarget;
    }

    public Class<? extends VCSModule> getVcsModule() {
        return vcsModule;
    }

    public void setVcsModule(Class<? extends VCSModule> vcsModule) {
        this.vcsModule = vcsModule;
    }

    public Class<? extends ITSModule> getItsModule() {
        return itsModule;
    }

    public void setItsModule(Class<? extends ITSModule> itsModule) {
        this.itsModule = itsModule;
    }

    public abstract String toString();
}
