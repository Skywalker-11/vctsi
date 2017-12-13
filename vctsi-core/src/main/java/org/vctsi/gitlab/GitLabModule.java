package org.vctsi.gitlab;

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

import org.gitlab.api.GitlabAPI;
import org.gitlab.api.GitlabAPIException;
import org.gitlab.api.http.GitlabHTTPRequestor;
import org.gitlab.api.models.GitlabIssue;
import org.gitlab.api.models.GitlabNote;
import org.gitlab.api.models.GitlabProject;
import org.vctsi.internal.its.*;
import org.vctsi.internal.tasks.SearchIssueTask;
import org.vctsi.internal.vcs.VCSSearchParameters;
import org.vctsi.utils.OutputUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GitLabModule extends ITSModule implements CommentRetriever<GitlabNote, GitlabIssue>, IssueFilter<GitlabIssue> {
    private static int threadWait = 700;
    private static int taskWait = 25;
    private final static int threadPoolSize = 2; //TODO
    private GitLabIssueConverter issueConverter = new GitLabIssueConverter();
    private GitlabAPI api;

    private boolean checkId;
    private boolean checkAuthor;
    private boolean checkAssignee;
    private boolean checkTargetVersion;
    private boolean checkCommit;
    private boolean checkDescription;
    private boolean checkEndDate;
    private boolean checkStartDate;
    private boolean checkTitle;
    private boolean checkIsAssigned;
    private boolean checkState;
    private ITSSearchParameters searchParameters;

    /**
     * imports the issues into the local database
     *
     * @return true if successful; else false
     */
    @Override
    protected boolean importIssues() {
        try {
            if (itsSettings.getPassword() == null || itsSettings.getUsername() == null || !itsSettings.getUsername().equals("token")) {
                OutputUtil.printError("You have to authenticate to GitLab by using an apitoken as password and 'token' as username");
                return false;
            }
            ITSSqlModule sqlModule = new ITSSqlModule(dbSettings);
            String sqlProject = itsSettings.getProject();
            sqlModule.clearTables(sqlProject);
            sqlModule.close();
        } catch (SQLException e) {
            OutputUtil.printError("An error occured: " + e.toString());
        }
        IssueThreadSpawner<GitlabIssue, GitlabNote> threadSpawner = new IssueThreadSpawner<>(
                new GitLabIssueConverter(),
                this,
                new GitLabCommentConverter(),
                dbSettings,
                itsSettings.getProject(),
                threadPoolSize,
                true
        );

        api = GitlabAPI.connect(itsSettings.getPath(), itsSettings.getPassword());
        try {
            //start the actual import
            if (!walkPages(threadSpawner)) {
                return false;
            }
            //wait till all thread finished their work
            threadSpawner.finish();

            if (threadSpawner.getErrors().isEmpty() && error.isEmpty()) {
                return true;
            } else {
                OutputUtil.printError(threadSpawner.getErrors() + ";" + error);
                return false;
            }
        } catch (InterruptedException e) {
            OutputUtil.printError("An error occured: " + e.toString());
            try {
                threadSpawner.stopNow();
            } catch (InterruptedException e1) {
                OutputUtil.debug("could not stop thread spawner");
            }
            return false;
        }
    }

    /**
     * this will search online for issues matches the parameters mentioned in the task. Because gitlab does not support
     * filtering/searching of the issues this will first get ALL issues from gitlab and then filters them locally
     *
     * @param task search task that contains the parameters for the search
     * @return the issues matching the parameters or null if an error occured
     */
    @Override
    protected List<Issue> onlineSearch(SearchIssueTask task) {
        if (itsSettings.getPassword() == null || itsSettings.getUsername() == null || !itsSettings.getUsername().equals("token")) {
            OutputUtil.printError("You have to authenticate to GitLab by using an apitoken as password and 'token' as username");
            return null;
        }
        if (itsSettings.getPath() == null) {
            OutputUtil.printError("Gitlab requires a path set in the -itsPath");
            return null;
        }

        //prepare filter
        issueConverter = new GitLabIssueConverter();
        searchParameters = task.getSearchParameters();
        checkId = searchParameters.getIds() != null && searchParameters.getIds().length != 0;
        checkAuthor = searchParameters.getAuthor() != null;
        checkAssignee = searchParameters.getAssignee() != null;
        checkTargetVersion = searchParameters.getTargetVersion() != null;
        checkCommit = searchParameters.getCommit() != null;
        checkDescription = searchParameters.getDescription() != null;
        checkEndDate = searchParameters.getEndDate() != null;
        checkStartDate = searchParameters.getStartDate() != null;
        checkTitle = searchParameters.getTitle() != null;
        checkIsAssigned = "ASSIGNED".equals(searchParameters.getState());
        checkState = searchParameters.getState() != null;

        IssueThreadSpawner<GitlabIssue, GitlabNote> threadSpawner = new IssueThreadSpawner<>(
                new GitLabIssueConverter(),
                this,
                this,
                new GitLabCommentConverter(),
                dbSettings,
                itsSettings.getProject(),
                threadPoolSize,
                false
        );
        api = GitlabAPI.connect(itsSettings.getPath(), itsSettings.getPassword());
        try {
            if (!walkPages(threadSpawner)) {
                return null;
            }
            //wait till all threads finished their work
            threadSpawner.finish();

            //if no errors occured return the results from the threadspawner
            if (threadSpawner.getErrors().isEmpty() && error.isEmpty()) {
                return threadSpawner.getResults(0);
            } else {
                OutputUtil.printError(threadSpawner.getErrors() + ";" + error);
                return null;
            }
        } catch (InterruptedException e) {
            OutputUtil.printError("An error occured: " + e.getMessage());
            return null;
        }
    }

    /**
     * this will get a single issue for that id. this has to be the unique id of the issue
     *
     * @param id the unique id of the issue (not the issue number in the project)
     * @return the a single issue with that id or null if an error occured
     */
    @Override
    protected List<Issue> getOnlineIssue(int id) {
        if (itsSettings.getPassword() == null || itsSettings.getUsername() == null || !itsSettings.getUsername().equals("token")) {
            OutputUtil.printError("You have to authenticate to GitLab by using an apitoken as password and 'token' as username");
            return null;
        }
        if (itsSettings.getPath() == null) {
            OutputUtil.printError("Gitlab requires a path set in the -itsPath");
            return null;
        }
        try {
            GitlabAPI api = GitlabAPI.connect(itsSettings.getPath(), itsSettings.getPassword());
            GitlabIssue issue = api.getIssue(itsSettings.getProject(), id);
            Issue vctsiIssue = issueConverter.convertToIssue(issue);
            List<GitlabNote> comments = api.getNotes(issue);
            vctsiIssue.setComments(convertToIssueComment(comments, new GitLabCommentConverter()));
            List<Issue> result = new ArrayList<>();
            result.add(vctsiIssue);
            return result;
        } catch (FileNotFoundException e) {
            return Collections.emptyList();
        } catch (IOException e) {
            OutputUtil.printError("An error occured: " + e.getMessage());
            return null;
        }
    }

    /**
     * sets the prefix of an issue id (#) so that it is in the format it would be written in a commit message and
     * in the webinterface would link to the real issue
     *
     * @param searchParameters the searchparameters of a commit search where the prefix/suffix should be added
     */
    @Override
    public void setTicketPrefixSuffix(VCSSearchParameters searchParameters) {
        String ticket = searchParameters.getTicket();
        if (ticket != null) {
            searchParameters.setTicket("#" + ticket);
        }
    }

    /**
     * this will return all comments of an issue
     *
     * @param issue the issue from which the comments shall be returned
     * @return list of comments (GitlabNote) from the issue
     */
    @Override
    public List<GitlabNote> getComments(GitlabIssue issue) {
        try {
            try { //sleep to prevent lockout because of too many requests
                Thread.sleep(taskWait * threadPoolSize);
                return api.getNotes(issue);
            } catch (GitlabAPIException e) {
                if (e.getResponseCode() == 429) { //we made too many requsts. so we wait and then try again
                    Thread.sleep(threadWait);
                    OutputUtil.debug("too many requests! waiting" + threadWait);
                    return getComments(issue);
                } else if (e.getResponseCode() == 502) {
                    //seems like the server can't handle that many requests so wo will slow down
                    threadWait += 10;
                    taskWait += 2;
                    OutputUtil.debug("throttling speed");
                    Thread.sleep(threadWait);
                    return getComments(issue);
                }
                error += e;
                return null;
            }
        } catch (IOException e) {
            error += e;
            return null;
        } catch (InterruptedException e) {
            error += e;
            return null;
        }
    }

    @Override
    public boolean evaluateIssue(GitlabIssue issue) {
        return (!checkId || isContainedIn(issue.getIid(), searchParameters.getIds()))
                && (!checkAuthor || searchParameters.getAuthor().equals(issueConverter.getAuthor(issue)))
                && (!checkAssignee || searchParameters.getAssignee().equals(issueConverter.getAssignee(issue)))
                && (!checkTargetVersion || searchParameters.getTargetVersion().equals(issueConverter.getTargetVersion(issue)))
                && (!checkCommit || (issueConverter.getTitle(issue).contains(searchParameters.getCommit().substring(0, 8))
                || issueConverter.getDescription(issue).contains(searchParameters.getCommit().substring(0, 8))))
                && (!checkDescription || issueConverter.getDescription(issue).contains(searchParameters.getDescription()))
                && (!checkEndDate || searchParameters.getEndDate().compareTo(LocalDateTime.ofInstant(issue.getUpdatedAt().toInstant(), ZoneOffset.ofHours(2))) >= 0)
                && (!checkStartDate || searchParameters.getStartDate().compareTo(LocalDateTime.ofInstant(issue.getUpdatedAt().toInstant(), ZoneOffset.ofHours(2))) <= 0)
                && (!checkTitle || issueConverter.getTitle(issue).contains(searchParameters.getTitle()))
                && (!checkIsAssigned || (issue.getAssignee() != null && GitlabIssue.STATE_OPENED.equals(issue.getState())))
                && (!checkState || hasSameState(issue.getState(), searchParameters.getState()));
    }

    /**
     * We can't use the asIterator method from GitlabHTTPRequestor because in case our request gets blocked due to
     * too many request we loose a page (10 issue) from the result. So we have to request the pages manually.
     *
     * @param threadSpawner the threadspawner which will retrieve the comments for the issues
     * @return true if the issues were successfully received,  false on error
     * @throws InterruptedException
     */
    private boolean walkPages(IssueThreadSpawner<GitlabIssue, GitlabNote> threadSpawner) throws InterruptedException {
        //
        GitlabHTTPRequestor requestor = api.retrieve();
        String tailUrl;
        try {
            tailUrl = GitlabProject.URL + "/" + api.getProject(itsSettings.getProject()).getId() + GitlabIssue.URL;
        } catch (IOException e) {
            OutputUtil.printError("Could not find the project " + itsSettings.getProject() + ". Check connection and project name");
            return false;
        }

        int page = 1; //page 0 and 1 are the same (see GitlabHTTPRequestor.asIterator(..,..).findNextUrl()
        do {
            try {
                Thread.sleep(taskWait * 3 * threadPoolSize);
                GitlabIssue[] issues = requestor.to(tailUrl + "?per_page=20&page=" + page, GitlabIssue[].class, null);
                if (issues.length == 0) {
                    break;
                }
                threadSpawner.put(Arrays.asList(issues), page);
                page++;
            } catch (IOException e) {
                if (e instanceof GitlabAPIException) {
                    if (((GitlabAPIException) e).getResponseCode() == 429) {
                        //we did too many request, so we wait and retry later
                        Thread.sleep(threadWait);
                        OutputUtil.debug("too many requests! waiting" + threadWait);
                        continue;
                    } else if (((GitlabAPIException) e).getResponseCode() == 502) {
                        //seems like the server can't handle that many requests so wo will slow down
                        threadWait += 10;
                        taskWait += 2;
                        OutputUtil.debug("throttling speed");
                        Thread.sleep(threadWait);
                        continue;
                    }
                }
                threadSpawner.stopNow();
                return false;
            }
        } while (!threadSpawner.isShutdown());
        return true;
    }

    /**
     * tests if the two state represents the same
     * @param gitlabState the state of gitlab
     * @param issueState the internal state
     * @return true if both represents the same; else false
     */
    private boolean hasSameState(String gitlabState, String issueState) {
        return (IssueState.NEW.toString().equals(issueState) && gitlabState.equals(GitlabIssue.STATE_OPENED)) ||
                (IssueState.ASSIGNED.toString().equals(issueState) && gitlabState.equals(GitlabIssue.STATE_OPENED)) ||
                (IssueState.SOLVED.toString().equals(issueState) && gitlabState.equals(GitlabIssue.STATE_CLOSED));
    }
}
