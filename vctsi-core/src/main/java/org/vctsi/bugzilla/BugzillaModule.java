package org.vctsi.bugzilla;

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

import com.j2bugzilla.base.*;
import com.j2bugzilla.rpc.BugComments;
import com.j2bugzilla.rpc.BugSearch;
import org.vctsi.internal.its.*;
import org.vctsi.internal.tasks.SearchIssueTask;
import org.vctsi.utils.OutputUtil;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class BugzillaModule extends ITSModule implements CommentRetriever<Comment, Bug> {

    private final int threadPoolSize = 5;
    static final private int batchSize = 100;
    ThreadLocal<BugzillaConnector> connector;

    /**
     * this will import issues into the local database
     *
     * @return true if import is successful; else false
     */
    @Override
    protected boolean importIssues() {
        if (itsSettings.getPath() == null) {
            OutputUtil.printError("Bugzilla requires a path to be set in -itsPath");
            return false;
        }
        try {
            setupSqlTables();
        } catch (SQLException e) {
            OutputUtil.printError("Error preparing sql tables.");
            return false;
        }

        if (createConnector() == null) {
            OutputUtil.printError("Connection failed");
            return false;
        }
        IssueThreadSpawner<Bug, Comment> threadSpawner = new IssueThreadSpawner<>(
                new BugzillaIssueConverter(),
                this,
                new BugzillaCommentConverter(),
                dbSettings,
                itsSettings.getProject(),
                threadPoolSize,
                true
        );
        try {
            //fill issue queue
            int i = 0;
            do {
                BugSearch bs = new BugSearch(
                        new BugSearch.SearchQuery(BugSearch.SearchLimiter.PRODUCT, itsSettings.getProject()),
                        new BugSearch.SearchQuery(BugSearch.SearchLimiter.LIMIT, "" + batchSize),
                        new BugSearch.SearchQuery(BugSearch.SearchLimiter.OFFSET, "" + batchSize * i)
                );
                connector.get().executeMethod(bs);
                List<Bug> bugList = bs.getSearchResults();
                threadSpawner.put(bugList, i);
                if (bugList.size() != batchSize) {
                    break;
                }
                i++;
            } while (!threadSpawner.isShutdown());

            threadSpawner.finish();

            if (threadSpawner.getErrors().isEmpty()) {
                return true;
            } else {
                OutputUtil.printError(threadSpawner.getErrors());
                return false;
            }
        } catch (BugzillaException e) {
            OutputUtil.printError("Error retrieving the bugzilla bugs. " + e.getMessage());
            return false;
        } catch (InterruptedException e) {
            OutputUtil.debug("Thread " + Thread.currentThread().getName() + " was interrupted");
            return false;
        } finally {
            try {
                threadSpawner.stopNow();
                threadSpawner.finish();
            } catch (InterruptedException e) {
                //do nothing
            }
        }
    }

    /**
     * gets comments of a bug
     *
     * @param bug the bug of which the comments shall be returned
     * @return the comments of the bug
     */
    @Override
    public List<Comment> getComments(Bug bug) {
        BugComments bugComments = new BugComments(bug);
        BugzillaConnector bc = connector.get();
        try {
            bc.executeMethod(bugComments);
        } catch (BugzillaException e) {
            //some older comments may contain invalid characters like 0x12 (eg. https://bugzilla.mozilla.org/show_bug.cgi?id=185382)
            if (e.getMessage().contains("An unknown error was encountered") && e.getMessage().contains("(Unicode: 0x")) {
                OutputUtil.debug("Invalid character in comment of bug. the comments will be dropped" + bug.getID());
                List<Comment> commentList = new ArrayList<>(1);
                commentList.add(new Comment(-1, null, null, null));
                return commentList;
            } else {
                error += e.getMessage();
                return null;
            }
        }
        return bugComments.getComments();
    }

    /**
     * searches online for tickets
     *
     * @param task search task that contains the parameters for the search
     * @return a list of issues according to the parameters
     */
    @Override
    public List<Issue> onlineSearch(SearchIssueTask task) {
        if (task.getSearchParameters().getEndDate() != null) {
            OutputUtil.printError("endDate is not supported at online search with buzilla");
            return null;
        }
        if (itsSettings.getPath() == null) {
            OutputUtil.printError("Bugzilla requires a path to be set as itsPath");
            return null;
        }
        if (createConnector() == null) {
            OutputUtil.printError("Connection failed");
            return null;
        }
        ITSSearchParameters searchParameters = task.getSearchParameters();
        BugSearch.SearchQuery[] onlineSearchParams = getOnlineSearchParameters(searchParameters);
        try {
            return executeSearch(onlineSearchParams);
        } catch (InterruptedException e) {
            OutputUtil.printError("an error occured: " + e.getMessage());
            return null;
        }
    }

    /**
     * get issues for the specified id
     *
     * @param id the id of the issue
     * @return a list containing the issues matching the id (normally should contain only 1 element); null if error occured
     */
    @Override
    public List<Issue> getOnlineIssue(int id) {
        try {
            if (createConnector() == null) {
                OutputUtil.printError("Connection failed");
                return null;
            }

            BugSearch.SearchQuery[] parameters = new BugSearch.SearchQuery[]{
                    new BugSearch.SearchQuery(BugSearch.SearchLimiter.OFFSET, "0"),
                    new BugSearch.SearchQuery(BugSearch.SearchLimiter.LIMIT, "" + batchSize),
                    new BugSearch.SearchQuery(BugSearch.SearchLimiter.ID, id)
            };
            return executeSearch(parameters);
        } catch (InterruptedException e) {
            OutputUtil.printError("An error occured: " + e.getMessage());
            return null;
        }
    }

    /**
     * executes the appropriate search for the parameters
     *
     * @param onlineSearchParams parameters of the search
     * @return a list of issues that were found
     * @throws BugzillaException thrown if error occured while communicating with the server
     */
    private List<Issue> executeSearch(BugSearch.SearchQuery[] onlineSearchParams) throws InterruptedException {
        IssueThreadSpawner<Bug, Comment> threadSpawner = new IssueThreadSpawner<>(
                new BugzillaIssueConverter(),
                this,
                new BugzillaCommentConverter(),
                dbSettings,
                itsSettings.getProject(),
                threadPoolSize,
                false
        );

        try {
            int idx = 0;
            do {
                onlineSearchParams[0] = new BugSearch.SearchQuery(BugSearch.SearchLimiter.OFFSET, "" + (idx * batchSize));
                BugSearch bs = new BugSearch(onlineSearchParams);
                connector.get().executeMethod(bs);

                List<Bug> tmpList = bs.getSearchResults();
                threadSpawner.put(tmpList, idx++);
                if (tmpList.size() != batchSize) {
                    break;
                }
            } while (!threadSpawner.isShutdown());

            threadSpawner.finish();
            if (threadSpawner.getErrors().isEmpty() && error.isEmpty()) {
                return threadSpawner.getResults(batchSize);
            } else {
                OutputUtil.printError(threadSpawner.getErrors() + "; " + error);
                return null;
            }
        } catch (InterruptedException e) {
            OutputUtil.printError("Thread interrupted" + e.getMessage());
            return null;
        } catch (BugzillaException e) {
            threadSpawner.stopNow();
            threadSpawner.finish();
            return null;
        }
    }

    /**
     * creates the connection to bugzilla
     *
     * @return a connector representing the connection
     */
    private BugzillaConnector createConnector() {
        connector = new ThreadLocal<BugzillaConnector>() {
            @Override
            protected BugzillaConnector initialValue() {
                try {
                    BugzillaConnector bc = new BugzillaConnector();
                    //set parameters for connection
                    if (itsSettings.getUsername() != null) {
                        bc.connectTo(
                                itsSettings.getPath(),
                                itsSettings.getUsername(),
                                itsSettings.getPassword()
                        );
                    } else {
                        bc.connectTo(
                                itsSettings.getPath()
                        );
                    }

                    return bc;
                } catch (ConnectionException e) {
                    OutputUtil.printError("Error connecting to Bugzilla");
                    return null;
                }
            }
        };
        return connector.get();
    }

    /**
     * builds the search queries for the online search
     *
     * @param searchParameters the search parameters
     * @return array of SearchQuery representing the parameters specified in searchParameters
     */
    private BugSearch.SearchQuery[] getOnlineSearchParameters(ITSSearchParameters searchParameters) {
        ArrayList<BugSearch.SearchQuery> params = new ArrayList<>();
        ArrayList<String> summarySearches = new ArrayList<>();
        params.add(new BugSearch.SearchQuery(BugSearch.SearchLimiter.OFFSET, "")); //placeholder; will be filled when executed
        params.add(new BugSearch.SearchQuery(BugSearch.SearchLimiter.LIMIT, "" + batchSize));
        params.add(new BugSearch.SearchQuery(BugSearch.SearchLimiter.PRODUCT, itsSettings.getProject()));
        Integer[] ids = searchParameters.getIds();
        if (ids != null) {
            for (int i = 0; i < ids.length; i++) {
                params.add(new BugSearch.SearchQuery(BugSearch.SearchLimiter.ID, "" + ids[i]));
            }
        }
        if (searchParameters.getAssignee() != null) {
            params.add(new BugSearch.SearchQuery(BugSearch.SearchLimiter.OWNER, searchParameters.getAssignee()));
        }
        if (searchParameters.getCommit() != null) {
            summarySearches.add(searchParameters.getCommit());
        }
        if (searchParameters.getTitle() != null) {
            summarySearches.add(searchParameters.getTitle());
        }
        if (searchParameters.getDescription() != null) {
            summarySearches.add(searchParameters.getDescription());
        }
        if (searchParameters.getAuthor() != null) {
            params.add(new BugSearch.SearchQuery(BugSearch.SearchLimiter.CREATOR, searchParameters.getAuthor()));
        }
        if (searchParameters.getState() != null) {
            params.add(new BugSearch.SearchQuery(
                    BugSearch.SearchLimiter.STATUS,
                    getBugzillaState(IssueState.valueOf(searchParameters.getState()))
            ));
        }
        if (searchParameters.getStartDate() != null) {
            params.add(new BugSearch.SearchQuery(BugSearch.SearchLimiter.SINCE, searchParameters.getStartDate().format(DateTimeFormatter.ISO_DATE_TIME)));
        }
        if (!summarySearches.isEmpty()) {
            Object[] summaryParam = summarySearches.toArray();
            params.add(new BugSearch.SearchQuery(BugSearch.SearchLimiter.SUMMARY, summaryParam));
        }
        return params.toArray(new BugSearch.SearchQuery[params.size()]);
    }

    /**
     * returns the state in bugzilla for an internal state
     *
     * @param state the internal state
     * @return the representation of the state in bugzilla
     */
    private String getBugzillaState(IssueState state) {
        switch (state) {
            case NEW:
                return "NEW";
            case CONFIRMED:
                return "CONFIRMED";
            case ASSIGNED:
                return "ASSIGNED";
            case SOLVED:
                return "SOLVED";
            case REOPENED:
                return "REOPENED";
            case UNCONFIRMED:
                return "UNCONFIRMED";
            case VERIFIED:
                return "VERIFIED";
        }
        return "";
    }
}
