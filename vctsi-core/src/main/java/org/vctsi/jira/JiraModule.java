package org.vctsi.jira;

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

import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.RestClientException;
import com.atlassian.jira.rest.client.api.SearchRestClient;
import com.atlassian.jira.rest.client.api.domain.BasicIssue;
import com.atlassian.jira.rest.client.api.domain.Comment;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.atlassian.util.concurrent.Promise;
import org.vctsi.internal.its.ITSModule;
import org.vctsi.internal.its.ITSSearchParameters;
import org.vctsi.internal.its.ITSSqlModule;
import org.vctsi.internal.its.IssueState;
import org.vctsi.internal.tasks.ImportIssuesTask;
import org.vctsi.internal.tasks.SearchIssueTask;
import org.vctsi.internal.vcs.VCSSearchParameters;
import org.vctsi.utils.OutputUtil;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class JiraModule extends ITSModule {

    private final int batchSize = 10;
    private SearchRestClient searchClient;
    private IssueRestClient issueRestClient;
    private ITSSqlModule sqlModule;
    private JiraIssueConverter issueConverter = new JiraIssueConverter();

    /**
     * this will import issues into the local database
     *
     * @return true if import is successful; else false
     */
    @Override
    protected boolean importIssues() {
        if (itsSettings.getPath() == null) {
            OutputUtil.printError("Jira requires a path to be set in -itsPath");
            return false;
        }
        try {
            sqlModule = new ITSSqlModule(dbSettings);
            sqlModule.prepareTicketImport(itsSettings.getProject());
            String query = "project = \"" + itsSettings.getProject() + "\"";

            if (!createClient()) {
                return false;
            }

            SearchResult result = searchClient.searchJql(query, batchSize, 0, null).claim();
            importIssues(result, 0);
            int total = result.getTotal();
            for (int i = batchSize; i < total; i += batchSize) {
                Promise<SearchResult> promise = searchClient.searchJql(query, batchSize, i, null);
                importIssues(promise.claim(), i);
            }

            sqlModule.finishImport();
            if (error.isEmpty()) {
                return true;
            } else {
                OutputUtil.printError("An error occured: " + error);
                return false;
            }
        } catch (SQLException e) {
            OutputUtil.printError("An error occured: " + e.getMessage());
            return false;
        } catch (RestClientException e) {
            if (e.getStatusCode().isPresent() && e.getStatusCode().get() == 401) {
                OutputUtil.printError("Access to jira denied. Check the its credentials");
            } else {
                OutputUtil.printError("An error occured while communicating with the jira server" + e.getStatusCode());
            }
            return false;
        }
    }

    /**
     * these word are ignored in search: "a", "and", "are", "as", "at", "be",
     * "but", "by", "for", "if", "in", "into", "is", "it", "no", "not", "of",
     * "on", "or", "s", "such", "t", "that", "the", "their", "then", "there",
     * "these", "they", "this", "to", "was", "will", "with"
     *
     * @param task search task that contains the parameters for the search
     * @return a list of issues according to the parameters
     */
    @Override
    protected List<org.vctsi.internal.its.Issue> onlineSearch(SearchIssueTask task) {
        ITSSearchParameters params = task.getSearchParameters();
        if (params.getIds() != null) {
            OutputUtil.printError("Search for ids is not supported by Jira. Use names instead");
            return null;
        }

        if (!createClient()) {
            return null;
        }

        try {
            String query = "project = \"" + itsSettings.getProject() + "\"";

            if (params.getAuthor() != null) {
                query += " AND reporter = \"" + params.getAuthor() + "\" ";
            }
            if (params.getTitle() != null) {
                query += " AND summary ~ '" + escapeString(params.getTitle()) + "'";
            }
            if (params.getDescription() != null) {
                query += " AND description ~ '" + escapeString(params.getDescription()) + "'";
            }
            if (params.getCommit() != null) {
                query += " AND (description ~ '" + escapeString("issue #" + params.getCommit()) + "'";
            }
            if (params.getAssignee() != null) {
                query += " AND (assignee ~ '" + escapeString(params.getAssignee()) + "'";
            }
            if (params.getNames() != null && params.getNames().length > 0) {

                query += " AND ( id = " + String.join(" OR id = ", (CharSequence[]) params.getNames()) + ")";
            }
            if (params.getState() != null) {
                query += " AND status = '" + getJiraState(params.getState()) + "'";
            }
            if (params.getStartDate() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm");
                query += " AND createdDate >= '" + params.getStartDate().format(formatter) + "' ";
                if (params.getEndDate() != null) {
                    query += " AND createdDate <= '" + params.getEndDate().format(formatter) + "' ";
                }
            }

            SearchResult result = searchClient.searchJql(query, batchSize, 0, null).claim();
            List<org.vctsi.internal.its.Issue> issues = getIssues(result);
            int total = result.getTotal();
            for (int i = batchSize; i < total; i += batchSize) {
                issues.addAll(getIssues(searchClient.searchJql(query, batchSize, i, null).claim()));
            }
            return issues;
        } catch (RestClientException e) {
            if (e.getStatusCode().isPresent() && e.getStatusCode().get() == 401) {
                OutputUtil.printError("Access to jira denied. Check the its credentials");
            } else {
                OutputUtil.printError("Error occured: you probably used a reserved character in a parameter. See for more information:" + System.lineSeparator()
                        + " https://confluence.atlassian.com/jira064/advanced-searching-functions-720416734.html#AdvancedSearchingFunctions-characters " + System.lineSeparator()
                        + e.getMessage()
                );
            }
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
    protected List<org.vctsi.internal.its.Issue> getOnlineIssue(int id) {
        OutputUtil.printError("Jira module does not support numeric ids. Use the search with 'name' instead.");
        return null;
    }


    /**
     * @param searchParameters contains the searchParameters where the ticket id has to be modified
     */
    @Override
    public void setTicketPrefixSuffix(VCSSearchParameters searchParameters) {
        searchParameters.setTicket("#" + searchParameters.getTicket());
    }

    /**
     * creates the connection to jira
     *
     * @return true if conection is established, else false
     */
    private boolean createClient() {
        if (itsSettings.getPath() == null) {
            OutputUtil.printError("Jira requires a path set in the -itsPath");
            return false;
        }
        try {
            URI uri = new URI(itsSettings.getPath());
            JiraRestClient client = (new AsynchronousJiraRestClientFactory()).createWithBasicHttpAuthentication(
                    uri,
                    itsSettings.getUsername(),
                    itsSettings.getPassword());
            searchClient = client.getSearchClient();
            issueRestClient = client.getIssueClient();
            return true;
        } catch (URISyntaxException e) {
            OutputUtil.printError("itsPath is not a valid URI");
            return false;
        }
    }

    /**
     * Escapes sql reserved characters in query values
     * escaping @see https://confluence.atlassian.com/jira064/advanced-searching-functions-720416734.html#AdvancedSearchingFunctions-characters
     * https://confluence.atlassian.com/jira064/performing-text-searches-720416596.html#PerformingTextSearches-escaping
     * This does not work correctly because this characters are ignored during search. But if we do not escape them they
     * would be interpreted as JQL functions (eg. '-' would exclude issues containing the following term from the results
     */
    String escapeString(String toEscape) {
        return toEscape.replace("\\", "\\\\")
                .replace("\"", "\\\\\"")
                .replace("'", "\\'")
                .replace("+", "\\+")
                .replace("-", "\\-")
                .replace("&", "\\&")
                .replace("|", "\\|")
                .replace("!", "\\!")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("^", "\\^")
                .replace("~", "\\~")
                .replace("*", "\\*")
                .replace("?", "\\?")
                .replace(":", "\\:");
    }

    /**
     * gets the internal issue representation of issues in the searchresult
     *
     * @param searchResult a searchresult containing a list of jira issues
     * @return a list of converted issues
     */
    private List<org.vctsi.internal.its.Issue> getIssues(SearchResult searchResult) {
        ArrayList<org.vctsi.internal.its.Issue> issues = new ArrayList<>();
        for (BasicIssue issue : searchResult.getIssues()) {
            Promise<Issue> issuePromise = issueRestClient.getIssue(issue.getKey());
            issues.add(issueConverter.convertToIssue(issuePromise.claim()));
        }
        return issues;
    }

    /**
     * imports the issues of the searchresult
     *
     * @param searchResult the searchresult containing the issues
     * @param startId      the internal id of the first issue in this result
     */
    private void importIssues(SearchResult searchResult, int startId) {
        int id = startId;
        for (BasicIssue basicIssue : searchResult.getIssues()) {
            Promise<Issue> issuePromise = issueRestClient.getIssue(basicIssue.getKey());
            Issue issue = issuePromise.claim();
            id++;
            try {
                sqlModule.importIssue(
                        id,
                        issueConverter.getName(issue),
                        issueConverter.getTitle(issue),
                        issueConverter.getDescription(issue),
                        issueConverter.getAuthor(issue),
                        issueConverter.getCreationDate(issue),
                        issueConverter.getState(issue),
                        issueConverter.getAssignee(issue),
                        issueConverter.getTargetVersion(issue)
                );
                for (Comment comment : issue.getComments()) {
                    sqlModule.importComment(
                            comment.getId(),
                            id,
                            comment.getBody(),
                            (comment.getAuthor() == null ? null : comment.getAuthor().getName()),
                            comment.getCreationDate().toDate()
                    );
                }
            } catch (SQLException e) {
                error += e.getMessage();
            }
        }
    }

    /**
     * returns the state in jira for an internal state
     *
     * @param vctsiState the internal state
     * @return the representation of the state in jira
     */
    private String getJiraState(String vctsiState) {
        try {
            switch (IssueState.valueOf(vctsiState)) {
                case NEW:
                    return "Open";
                case ASSIGNED:
                    return "In Progress";
                case RESOLVED:
                    return "Resolved";
                case REOPENED:
                    return "Reopened";
                case SOLVED:
                    return "Closed";
                default:
                    return vctsiState;
            }
        } catch (IllegalArgumentException e) {
            return vctsiState;
        }
    }
}
