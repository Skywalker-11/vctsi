package org.vctsi;

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
import org.vctsi.internal.tasks.Task;
import org.vctsi.internal.vcs.VCSModule;
import org.vctsi.utils.ArgumentParser;
import org.vctsi.utils.OutputUtil;

public class Main {

    static final String ERROR_MISSING_VCS_MODULE = "requested a task to retrieve commits but no vcs module is given";
    static final String ERROR_MISSING_ITS_MODULE = "requested a task to retrieve issues but no its module is given";

    /**
     * main executable: will call the argumentparser and starts the actions depending on what parameters are passed
     * @param args the parameters controlling the task and settings
     */
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        ArgumentParser ap = new ArgumentParser();
        if (ap.parse(args)) {
            startProcessing(ap);
        }
        OutputUtil.debug("Runtime in millis: " + (System.currentTimeMillis() - start));
    }

    /**
     * this will start the actual actions
     * @param ap the argumentparser that parsed the program arguments and now contains the task and settings
     */
    private static void startProcessing(ArgumentParser ap) {
        Task task = ap.getTask();
        if (task.getTaskTarget() == Task.TaskTarget.COMMIT) {
            if (task.getVcsModule() == null) {
                OutputUtil.printError(ERROR_MISSING_VCS_MODULE);
                return;
            }
            try {
                VCSModule vcsModule = task.getVcsModule().newInstance();
                vcsModule.setSettings(ap.getVcsSettings());
                vcsModule.setDBSettings(ap.getDbSettings());
                vcsModule.executeTask(task);
            } catch (InstantiationException | IllegalAccessException e) {
                OutputUtil.printError("could not instantiate the vcs module");
            }
        } else if (task.getTaskTarget() == Task.TaskTarget.ISSUE) {
            if (task.getItsModule() == null) {
                OutputUtil.printError(ERROR_MISSING_ITS_MODULE);
                return;
            }
            try {
                ITSModule itsModule = task.getItsModule().newInstance();
                itsModule.setSettings(ap.getItsSettings());
                itsModule.setDBSettings(ap.getDbSettings());
                itsModule.executeTask(task);
            } catch (InstantiationException | IllegalAccessException e) {
                OutputUtil.printError("could not instantiate the its module");
            }
        }
    }
}
