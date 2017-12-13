package org.vctsi.github;

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

import org.kohsuke.github.*;
import org.vctsi.internal.its.*;
import org.vctsi.internal.tasks.SearchIssueTask;
import org.vctsi.internal.vcs.VCSSearchParameters;
import org.vctsi.utils.OutputUtil;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class GitHubModule extends ITSModule implements CommentRetriever<GHIssueComment, GHIssue>, IssueFilter<GHIssue> {

    private static final int threadPoolSize = 2;
    private static final int batchSize = 50;

    private boolean checkId;
    private boolean checkDescription;
    private boolean checkEndDate;
    private boolean checkStartDate;
    private boolean checkTitle;
    private boolean checkIsAssigned;
    private boolean checkNames;
    private ITSSearchParameters searchParameters;
    private IssueConverter<GHIssue> issueConverter;
    private GitHub api;
    private GHRepository repo;
    private boolean stop = false;

    private LinkedBlockingQueue<Byte> availableSearchApiCallPool = new LinkedBlockingQueue<>(30);
    private Thread schedular;

    /**
     * this will import issues into the local database
     *
     * @return true if import is successful; else false
     */
    @Override
    protected boolean importIssues() {
        try {
            setupSqlTables();
        } catch (SQLException e) {
            OutputUtil.printError("Error preparing sql tables.");
            return false;
        }
        try {
            if (!prepareRepo()) {
                return false;
            }
        } catch (IOException e) {
            OutputUtil.printError("Error retrieving the bugzilla bugs. " + e.getMessage());
            return false;
        }
        IssueThreadSpawner<GHIssue, GHIssueComment> threadSpawner = new IssueThreadSpawner<>(
                new GitHubIssueConverter(),
                this,
                new GitHubCommentConverter(),
                dbSettings,
                itsSettings.getProject(),
                threadPoolSize,
                true
        );
        try {
            //fill issue queue for import
            int j = 0;
            Iterator<GHIssue> iterator = repo.listIssues(GHIssueState.ALL)._iterator(batchSize);
            while (iterator.hasNext() && !threadSpawner.isShutdown()) {
                List<GHIssue> issueList = new ArrayList<>();
                for (int i = 0; i < batchSize; i++) {
                    if (iterator.hasNext()) {
                        GHIssue issue = iterator.next();
                        if (!issue.isPullRequest()) {
                            issueList.add(issue);
                        }
                    }
                }
                threadSpawner.put(issueList, j++);
            }
            threadSpawner.finish();


            if (threadSpawner.getErrors().isEmpty() && error.isEmpty()) {
                return true;
            } else {
                OutputUtil.printError(threadSpawner.getErrors() + ";" + error);
                return false;
            }
        } catch (InterruptedException e) {
            OutputUtil.debug("Thread " + Thread.currentThread().getName() + " was interrupted");
            return false;
        } finally {
            try {
                threadSpawner.stopNow();
                threadSpawner.finish();
            } catch (InterruptedException e) {
                //ignore
            }
        }
    }

    /**
     * searches online for tickets
     *
     * @param task search task that contains the parameters for the search
     * @return a list of issues according to the parameters
     */
    @Override
    protected List<Issue> onlineSearch(SearchIssueTask task) {
        searchParameters = task.getSearchParameters();
        GHIssueState state = GHIssueState.ALL;
        if (searchParameters.getState() != null) {
            state = getGHIssueState(searchParameters.getState());
        }
        try {
            if (!prepareRepo()) {
                return null;
            }
        } catch (IOException e) {
            OutputUtil.printError("An error occured: " + e.getMessage());
            return null;
        }
        IssueThreadSpawner<GHIssue, GHIssueComment> threadSpawner = new IssueThreadSpawner<>(
                new GitHubIssueConverter(),
                this,
                this,
                new GitHubCommentConverter(),
                dbSettings,
                itsSettings.getProject(),
                threadPoolSize,
                false
        );
        //GitHub only allows 30 search requests per minute so we have to limit the requests
        schedular = new Thread(new Runnable() {
            @Override
            public void run() {
                Byte b = 0;
                while (!stop) {
                    int freeSpace = 30 - availableSearchApiCallPool.size();
                    for (int i = 0; i < freeSpace; i++) {
                        try {
                            availableSearchApiCallPool.put(b);
                        } catch (InterruptedException e) {
                            OutputUtil.debug(e.getMessage());
                        }
                    }
                    try {
                        Thread.sleep(60000);
                    } catch (InterruptedException e) {
                        OutputUtil.debug(e.getMessage());
                    }
                }
            }
        });
        schedular.start();

        //prepare the filtering
        checkId = searchParameters.getIds() != null && searchParameters.getIds().length != 0;
        checkNames = searchParameters.getNames() != null && searchParameters.getNames().length != 0;
        checkDescription = searchParameters.getDescription() != null;
        checkEndDate = searchParameters.getEndDate() != null;
        checkStartDate = searchParameters.getStartDate() != null;
        checkTitle = searchParameters.getTitle() != null;
        checkIsAssigned = "ASSIGNED".equals(searchParameters.getState());
        issueConverter = new GitHubIssueConverter();

        //get the issues
        try {
            GHIssueSearchBuilder searchBuilder = api.searchIssues();
            searchBuilder.q("repo:" + itsSettings.getProject());
            searchBuilder.q("type:issue"); //to prevent pullrequests
            boolean inTitle = false;
            boolean inBody = false;
            if (searchParameters.getState() != null) {
                switch (state) {
                    case OPEN:
                        searchBuilder.isOpen();
                        break;
                    case CLOSED:
                        searchBuilder.isClosed();
                        break;
                }
                OutputUtil.printError("invalid state! github only supported states are: "
                        + IssueState.NEW.toString() + "," + IssueState.SOLVED.toString() + "," + IssueState.ASSIGNED.toString());
            }
            if (searchParameters.getAssignee() != null) {
                searchBuilder.q("assignee:\"" + searchParameters.getAssignee() + "\"");
            }
            if (searchParameters.getAuthor() != null) {
                searchBuilder.q("author:\"" + searchParameters.getAuthor() + "\"");
            }
            if (searchParameters.getTargetVersion() != null) {
                searchBuilder.q("milestone:\"" + searchParameters.getTargetVersion() + "\"");
            }
            if (searchParameters.getCommit() != null) {
                inBody = true;
                inTitle = true;
                searchBuilder.q(searchParameters.getCommit());
            }
            if (checkTitle) {
                inTitle = true;
                searchBuilder.q("\"" + searchParameters.getTitle() + "\"");
            }
            if (checkDescription) {
                inBody = true;
                searchBuilder.q("\"" + searchParameters.getDescription() + "\"");
            }
            if (inBody) {
                searchBuilder.q("in:body");
            }
            if (inTitle) {
                searchBuilder.q("in:title");
            }
            Iterator<GHIssue> iterator = searchBuilder.list()._iterator(batchSize);
            int taskIdx = 0;
            availableSearchApiCallPool.take();
            while (iterator.hasNext() && !threadSpawner.isShutdown()) {
                List<GHIssue> tmp = new ArrayList<>(batchSize);
                for (int i = 0; i < batchSize && iterator.hasNext(); i++) {
                    GHIssue issue = iterator.next();
                    tmp.add(issue);
                }
                threadSpawner.put(tmp, taskIdx++);
                availableSearchApiCallPool.take();
            }
            threadSpawner.finish();

            //if no errors occured return the results from the threadspawner
            if (threadSpawner.getErrors().isEmpty() && error.isEmpty()) {
                return threadSpawner.getResults(0);
            } else {
                OutputUtil.printError(threadSpawner.getErrors() + ";" + error);
                return null;
            }
        } catch (InterruptedException e) {
            OutputUtil.printError("main thread interrupted" + e.getMessage());
            return null;
        } finally {
            stopSchedular();
            try {
                threadSpawner.stopNow();
                threadSpawner.finish();
            } catch (InterruptedException e) {
                //ignore
            }
        }
    }

    /**
     * get issues for the specified id
     *
     * @param id the id of the issue
     * @return a list containing the issues matching the id (normally should contain only 1 element); null if error occured
     */
    @Override
    protected List<Issue> getOnlineIssue(int id) {
        GitHubIssueConverter converter = new GitHubIssueConverter();
        GitHubCommentConverter commentConverter = new GitHubCommentConverter();
        try {
            if (!prepareRepo()) {
                return null;
            }

            GHIssue issue = repo.getIssue(id);
            List<GHIssueComment> comments = issue.getComments();
            Issue vctsiIssue = converter.convertToIssue(issue);
            vctsiIssue.setComments(convertToIssueComment(comments, commentConverter));

            List<Issue> result = new ArrayList<>();
            result.add(vctsiIssue);
            return result;
        } catch (IOException e) {
            OutputUtil.printError("An error occured: " + e.getMessage());
            return null;
        }
    }

    /**
     * gets comments of an issue
     *
     * @param ghIssue the issue of which the comments shall be returned
     * @return the comments of the issue
     */
    @Override
    public List<GHIssueComment> getComments(GHIssue ghIssue) {
        try {
            if (ghIssue.getCommentsCount() > 0) {
                PagedIterator<GHIssueComment> iterator;
                if (ghIssue.getRepository() == null) {
                    //This requires two api calls because gitlab-api does not pass the repository to issues from the searchresult
                    //to the issue and so a NullPointerException would be thrown.
                    GHIssue issueWithRepo = repo.getIssue(ghIssue.getNumber());
                    iterator = issueWithRepo.listComments()._iterator(50);
                } else {
                    iterator = ghIssue.listComments()._iterator(50);
                }
                ArrayList<GHIssueComment> comments = new ArrayList<>(ghIssue.getCommentsCount());
                for (int i = 0; i < ghIssue.getCommentsCount(); i += 50) {
                    comments.addAll(iterator.nextPage());
                }
                return comments;
            }
            return Collections.emptyList();
        } catch (IOException e) {
            error += e.getMessage();
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
     * this evaluates an issue for specified filter parameters
     *
     * @param issue the issue that will be evaluated
     * @return false if the issue shall be discarded; true else
     */
    @Override
    public boolean evaluateIssue(GHIssue issue) {
        try {
            return (!checkId || isContainedIn(issue.getNumber(), searchParameters.getIds()))
                    && (!checkNames || isContainedIn("" + issue.getNumber(), searchParameters.getNames()))
                    && (!checkDescription || issueConverter.getDescription(issue).contains(searchParameters.getDescription()))
                    && (!checkEndDate || searchParameters.getEndDate().compareTo(LocalDateTime.ofInstant(issue.getUpdatedAt().toInstant(), ZoneOffset.ofHours(2))) >= 0)
                    && (!checkStartDate || searchParameters.getStartDate().compareTo(LocalDateTime.ofInstant(issue.getUpdatedAt().toInstant(), ZoneOffset.ofHours(2))) <= 0)
                    && (!checkTitle || issueConverter.getTitle(issue).contains(searchParameters.getTitle()))
                    && (!checkIsAssigned || (issue.getAssignee() != null && GHIssueState.OPEN.equals(issue.getState())));
        } catch (IOException e) {
            //can not be thrown from issue.getUpdatedAt() in the current version be if it will occure in the future we will decline the issue
            return false;
        }
    }

    /**
     * prepares the connection to github
     *
     * @return null if parameters are missing or an error occured
     * @throws IOException connection to github is not available
     */
    private boolean prepareRepo() throws IOException {
        String user = itsSettings.getUsername();
        String pass = itsSettings.getPassword();
        GitHubBuilder gitHubBuilder;
        if (user == null && pass == null) {
            gitHubBuilder = new GitHubBuilder();
        } else if (user == null || pass == null) {
            OutputUtil.printError("Github requires a username and password set in the itsUsername/itsPassword or neither of them for anonymous access. "
                    + "If you want to user a token for login use 'token' as username and the actual access token as password");
            return false;
        } else if (user.equals("token")) {
            gitHubBuilder = new GitHubBuilder().withOAuthToken(pass, user);
        } else {
            gitHubBuilder = new GitHubBuilder().withPassword(user, pass);
        }

        if (itsSettings.getPath() != null) {
            gitHubBuilder = gitHubBuilder.withEndpoint(itsSettings.getPath());
        }
        api = gitHubBuilder.build();

        if (!api.isCredentialValid()) {
            OutputUtil.printError("Invalid GitHub credentials");
            return false;
        }
        repo = api.getRepository(itsSettings.getProject());
        return true;
    }

    /**
     * gets the correct github issue state for a given vctsi state
     *
     * @param state vctsi issue state
     * @return github issue state
     */
    private GHIssueState getGHIssueState(String state) {
        if (state.equals(IssueState.NEW.toString()) || state.equals(IssueState.ASSIGNED.toString())) {
            return GHIssueState.OPEN;
        } else if (state.equals(IssueState.SOLVED.toString())) {
            return GHIssueState.CLOSED;
        } else {
            return GHIssueState.ALL;
        }
    }

    /**
     * this will cause the schedular to stop
     */
    private void stopSchedular() {
        stop = true;
        schedular.interrupt();
    }
}
