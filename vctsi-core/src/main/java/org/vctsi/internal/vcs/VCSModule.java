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

import org.vctsi.debug.vcs.VCSTestModule;
import org.vctsi.git.GitModule;
import org.vctsi.internal.DBSettings;
import org.vctsi.internal.its.ITSModule;
import org.vctsi.internal.tasks.GetCommitTask;
import org.vctsi.internal.tasks.GetDiffsTask;
import org.vctsi.internal.tasks.SearchCommitTask;
import org.vctsi.internal.tasks.Task;
import org.vctsi.svn.SVNModule;
import org.vctsi.svn.SVNRemoteModule;
import org.vctsi.utils.OutputUtil;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

public abstract class VCSModule {
    public final static String IMPORT_SUCCESS = "Import success";

    protected VCSSettings vcsSettings;
    protected DBSettings dbSettings;
    protected String error = "";
    protected ThreadLocal<VCSSqlModule> sqlModule;
    public final static HashMap<String, Class<? extends VCSModule>> availableVCSModules;

    static {
        availableVCSModules = new HashMap<>();
        availableVCSModules.put("VCSTestModule", VCSTestModule.class);
        availableVCSModules.put("git", GitModule.class);
        availableVCSModules.put("svn", SVNModule.class);
        availableVCSModules.put("svnRemote", SVNRemoteModule.class);
    }

    public VCSModule() {
    }

    public void setSettings(VCSSettings vcsSettings) {
        this.vcsSettings = vcsSettings;
    }

    public void setDBSettings(DBSettings dbSettings) {
        this.dbSettings = dbSettings;
    }

    /**
     * does some basic checks of the given task and then let the real module execute it
     *
     * @param task the task that should be executed
     */
    public void executeTask(Task task) {
        if (task.getTaskTarget() != Task.TaskTarget.COMMIT) {
            throw new RuntimeException("its modules can't handle vcs tasks");
        }
        switch (task.getTaskType()) {
            case SEARCH:
                VCSSearchParameters searchParameters = ((SearchCommitTask) task).getSearchParameters();
                if (searchParameters == null) {
                    throw new RuntimeException("no search parameters entered");
                }
                if (searchParameters.getTicket() != null
                        && task.getItsModule() == null) { //needed for lookup of prefix/suffix of ticket
                    throw new RuntimeException("its module needed to search for commit id but no its module was given");
                } else if (searchParameters.getTicket() != null) {
                    try {
                        ITSModule itsModule = task.getItsModule().newInstance();
                        itsModule.setTicketPrefixSuffix(searchParameters);
                    } catch (InstantiationException | IllegalAccessException e) {
                        throw new RuntimeException("could not instantiate its module");
                    }
                }
                List<Commit> result = search((SearchCommitTask) task);
                if (result != null) {
                    OutputUtil.printObjectList(result);
                }
                break;
            case GET:
                List<Commit> commits = getForId(((GetCommitTask) task).getCommitId());
                if (commits != null) {
                    OutputUtil.printObjectList(commits);
                }
                break;
            case IMPORT:
                if (vcsSettings.shouldOnlyUpdateNew() && vcsSettings.isNoUpdate()) {
                    OutputUtil.printError("Comination of -vcsOnlyNew and -vcsNoUpdate");
                    break;
                }
                if (vcsSettings.shouldOnlyUpdateNew()) {
                    importNewCommits();
                } else {
                    importCommits(vcsSettings.isNoUpdate());
                }
                break;
            case DIFF:
                Diff diff = getFileDiffs(((GetDiffsTask) task).getDiffSettings());
                if (diff != null) {
                    OutputUtil.printObject(diff);
                }
                break;
        }
    }

    protected List<Commit> search(SearchCommitTask task) {
        VCSSearchParameters searchParameters = task.getSearchParameters();
        String project = vcsSettings.getProject();
        try {
            VCSSqlModule sqlModule = new VCSSqlModule(dbSettings);
            return sqlModule.getCommitsForSearch(project, searchParameters);
        } catch (SQLException e) {
            OutputUtil.printError("an error occured: " + e.getMessage());
            return null;
        }
    }

    protected abstract boolean importCommits(boolean noUpdate);

    protected abstract boolean importNewCommits();

    protected List<Commit> getForId(String commitId) {
        try {
            VCSSqlModule sqlModule = new VCSSqlModule(dbSettings);
            return sqlModule.getCommitsForIds(vcsSettings.getProject(), new String[]{commitId});
        } catch (SQLException e) {
            OutputUtil.printError("An error occured: " + e.getMessage());
            return null;
        }
    }

    protected abstract Diff getFileDiffs(VCSDiffSettings diffSettings);


    /**
     * initializes the sql module by creating the necessary tables
     *
     * @param resetTable if true existing tables will be remove and recreated
     * @return true if initialization was successful; else false
     */
    protected boolean initializeSql(boolean resetTable) {
        try {
            VCSSqlModule module = createThreadLocalSqlModule();
            if (module == null) {
                return false;
            }
            if (resetTable) {
                module.recreateVcsTables(vcsSettings.getProject());
            }
            return true;
        } catch (SQLException e) {
            OutputUtil.printError("Error preparing sql tables for data import " + e.getMessage());
            return false;
        }
    }

    /**
     * creates thread local sql modules
     *
     * @return returns a sql module for the current thread or null if the creation failed
     * @throws SQLException if an error occurs while creating the sql module
     **/
    protected VCSSqlModule createThreadLocalSqlModule() throws SQLException {
        sqlModule = new ThreadLocal<VCSSqlModule>() {
            @Override
            protected VCSSqlModule initialValue() {
                try {
                    VCSSqlModule module = new VCSSqlModule(dbSettings);
                    module.prepareCommitImport(vcsSettings.getProject());
                    return module;
                } catch (SQLException e) {
                    OutputUtil.printError("could not connect to database");
                    return null;
                }
            }
        };
        return sqlModule.get();
    }

    /**
     * stores an error message this will later be used to check if an error occurred in worker threads
     *
     * @param error the message to store
     */
    protected void addError(String error) {
        this.error += error + System.lineSeparator();
    }

    /**
     * tests if import was successful
     *
     * @return true if successful, false else
     */
    protected boolean isSuccess() {
        if (error.isEmpty()) {
            OutputUtil.printInfo(VCSModule.IMPORT_SUCCESS);
            return true;
        } else {
            OutputUtil.printError("Some errors occured:" + error);
            return false;
        }
    }
}
