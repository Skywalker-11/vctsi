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

import org.vctsi.bugzilla.BugzillaModule;
import org.vctsi.debug.its.ITSTestModule;
import org.vctsi.github.GitHubModule;
import org.vctsi.gitlab.GitLabModule;
import org.vctsi.internal.DBSettings;
import org.vctsi.internal.tasks.GetIssueTask;
import org.vctsi.internal.tasks.ImportIssuesTask;
import org.vctsi.internal.tasks.SearchIssueTask;
import org.vctsi.internal.tasks.Task;
import org.vctsi.internal.vcs.VCSSearchParameters;
import org.vctsi.jira.JiraModule;
import org.vctsi.utils.OutputUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class ITSModule {

    final static String IMPORT_SUCCESS = "Import success";

    protected ITSSettings itsSettings;
    protected DBSettings dbSettings;
    public final static HashMap<String, Class<? extends ITSModule>> availableITSModules;
    protected String error = "";

    static {
        availableITSModules = new HashMap<>();
        availableITSModules.put("ITSTestModule", ITSTestModule.class);
        availableITSModules.put("bugzilla", BugzillaModule.class);
        availableITSModules.put("github", GitHubModule.class);
        availableITSModules.put("gitlab", GitLabModule.class);
        availableITSModules.put("jira", JiraModule.class);

    }

    public ITSModule() {
    }

    public void setSettings(ITSSettings itsSettings) {
        this.itsSettings = itsSettings;
    }

    public void setDBSettings(DBSettings dbSettings) {
        this.dbSettings = dbSettings;
    }

    /**
     * imports issues and their comments into local database
     *
     * @return true if import is successful; else false
     */
    protected abstract boolean importIssues();

    /**
     * searches online for issues
     *
     * @param task search task that contains the parameters for the search
     * @return a list of found issues, can be empty if no issues were found; null if errors occured
     */
    protected abstract List<Issue> onlineSearch(SearchIssueTask task);

    /**
     * searches online for issues
     *
     * @param id the id of the issue
     * @return a list containing the issue to the id (may contains more than one issue); null if errors occured
     */
    protected abstract List<Issue> getOnlineIssue(int id);


    /**
     * does some basic checks of the given task and then let the real module execute it
     *
     * @param task the task that should be executed
     * @return true if execution is successful; else false
     */
    public boolean executeTask(Task task) {
        if (task.getTaskTarget() != Task.TaskTarget.ISSUE) {
            OutputUtil.printError("its modules can't handle vcs tasks");
            return false;
        }
        if (itsSettings == null) {
            OutputUtil.printError("error itsModule contains no ITSSettings");
        }
        if (itsSettings.getProject() == null) {
            OutputUtil.printError("its modules require a project to be set in -itsProject");
            return false;
        }
        if (task instanceof SearchIssueTask) {
            ITSSearchParameters searchParameters = ((SearchIssueTask) task).getSearchParameters();
            if (searchParameters != null && searchParameters.getCommit() != null
                    && task.getVcsModule() == null) {
                OutputUtil.printError("its module should handle search for commit id but no vcs module is given");
                return false;
            }
            List<Issue> result;
            if (!itsSettings.isOnline()) {
                result = search(((SearchIssueTask) task).getSearchParameters());
            } else {
                result = onlineSearch((SearchIssueTask) task);
            }
            if (result != null) {
                OutputUtil.printObjectList(result);
                return true;
            }
            return false;
        } else if (task instanceof ImportIssuesTask) {
            if (importIssues()) {
                OutputUtil.printInfo(ITSModule.IMPORT_SUCCESS);
                return true;
            } else {
                return false;
            }
        } else if (task instanceof GetIssueTask) {
            GetIssueTask getTask = (GetIssueTask) task;
            List<Issue> result;
            if (!itsSettings.isOnline()) {
                ITSSearchParameters itsSearchParameters = new ITSSearchParameters();
                itsSearchParameters.setIds(new Integer[]{getTask.getIssueId()});
                result = search(itsSearchParameters);
            } else {
                result = getOnlineIssue(getTask.getIssueId());
            }
            if (result != null) {
                OutputUtil.printObjectList(result);
                return true;
            } else {
                return false;
            }
        }
        OutputUtil.printError("Unknown task");
        return false;
    }

    /**
     * searches for issues in the local database
     *
     * @param searchParameters parameters for the search
     * @return a list of found issues, can be empty if no issues were found; null if errors occured
     */
    protected List<Issue> search(ITSSearchParameters searchParameters) {
        try {
            ITSSqlModule sqlModule = new ITSSqlModule(dbSettings);
            return sqlModule.getIssues(
                    getWithCommitPrefixSuffix(searchParameters),
                    itsSettings.getProject()
            );
        } catch (SQLException e) {
            OutputUtil.printError(e.getMessage());
            return null;
        }
    }

    /**
     * this method can add prefix and suffix for the commitid that may are
     * referenced inside the title of an issue default no prefix and suffix are
     * added
     *
     * @param itsSearchParameters the search parameters of the current search
     * @return the modified itsSearchParameters
     */
    protected ITSSearchParameters getWithCommitPrefixSuffix(ITSSearchParameters itsSearchParameters) {
        return itsSearchParameters;
    }

    /**
     * initializes the sql module by creating necessary tables and prepared statements for import
     *
     * @return the sql modules with prepared statements and working tables
     * @throws SQLException if sql queries could not be executed or no connection could be established
     */
    protected ITSSqlModule setupSqlTables() throws SQLException {
        ITSSqlModule sqlModule = new ITSSqlModule(dbSettings);
        sqlModule.prepareTicketImport(itsSettings.getProject());
        sqlModule.clearTables(itsSettings.getProject());
        return sqlModule;
    }

    /**
     * can be used to set a prefix or suffix of ticket ids when searching in commit messages
     *
     * @param searchParameters contains the searchParameters where the ticket id has to be modified
     */
    public void setTicketPrefixSuffix(VCSSearchParameters searchParameters) {
        //default no prefix or suffix are defined
    }

    /**
     * tests if an object is contained inside an object array
     *
     * @param <R>    some object class that implements an equals method
     * @param needle the object that should be searched
     * @param stack  a list of objects that
     * @return true if the needle is found in the stack; else false
     */
    protected <R extends Object> boolean isContainedIn(R needle, R[] stack) {
        for (int i = 0; i < stack.length; i++) {
            if (stack[i].equals(needle)) {
                return true;
            }
        }
        return false;
    }

    /**
     * converts a list of T representing module specific comments of issues to a
     * list of vctsi IssueComment
     *
     * @param <T>       a class representing comments of issues
     * @param comments  a list of module specific comments of issues
     * @param converter converter that will be used to convert the single
     *                  comments
     * @return a converted list of comments
     */
    protected <T> List<IssueComment> convertToIssueComment(List<T> comments, CommentConverter<T, ?> converter) {
        List<IssueComment> issueComments = new ArrayList<>(comments.size());
        for (T comment : comments) {
            issueComments.add(converter.convertToIssueComment(comment));
        }
        return issueComments;
    }
}
