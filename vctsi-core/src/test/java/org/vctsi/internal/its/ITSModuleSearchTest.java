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

import org.junit.Before;
import org.junit.Test;
import org.vctsi.debug.its.ITSTestModule;
import org.vctsi.internal.DBSettings;
import org.vctsi.internal.tasks.SearchIssueTask;
import org.vctsi.utils.OutputUtil;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.vctsi.TestParameters.*;

public class ITSModuleSearchTest {

    private static DBSettings dbSettings = new DBSettings(DB_SERVER, DB_PORT, DB_DB, DB_USER, DB_PASSWORD);
    private static String project = "DEBUG";

    private static List<Issue> issues = new ArrayList<>();

    @Before
    public void prepareTest() throws SQLException {
        OutputUtil.DEBUG = false;
        createTestIssues();
        setupDb();
    }

    private static void createTestIssues() {
        issues.add(new Issue(0, "testname0", "testtitle0", "testdescription0", "testauthor0", LocalDateTime.of(2016, 4, 30, 23, 40, 58), IssueState.NEW.toString(), "assignee0", "targetversion0"));
        issues.add(new Issue(1, "testname0", "testtitle1", "testdescription1", "testauthor1", LocalDateTime.of(2016, 4, 30, 23, 40, 59), IssueState.NEW.toString(), "", ""));
        issues.add(new Issue(2, "testname0", "testtitle1_2", "testdescription1", "testauthor0", LocalDateTime.of(2016, 5, 30, 23, 40, 50), IssueState.NEW.toString(), "", ""));
        issues.add(new Issue(3, "testname0", "testtitle #399 ", "testdescription1", "testauthor1", LocalDateTime.of(2016, 5, 30, 23, 40, 50), IssueState.NEW.toString(), "", ""));
        issues.add(new Issue(4, "testname0", "testtitle #399,", "testdescription1", "testauthor1", LocalDateTime.of(2016, 5, 30, 23, 40, 51), IssueState.NEW.toString(), "", ""));
        issues.add(new Issue(5, "testname0", "testtitle 399* ", "testdescription1", "testauthor1", LocalDateTime.of(2016, 7, 30, 23, 40, 50), IssueState.NEW.toString(), "", ""));
        issues.add(new Issue(6, "testname0", "testtitle 399*,", "testdescription1", "testauthor1", LocalDateTime.of(2016, 7, 30, 23, 40, 50), IssueState.NEW.toString(), "", ""));
        issues.add(new Issue(7, "testname0", "testtitle #399*,", "testdescription1", "testauthor1", LocalDateTime.of(2016, 7, 30, 23, 40, 50), IssueState.NEW.toString(), "", ""));
        issues.add(new Issue(8, "testname0", "testtitle #399*,", "testdescription1", "testauthor1", LocalDateTime.of(2016, 7, 30, 23, 40, 50), IssueState.NEW.toString(), "", ""));
        issues.add(new Issue(9, "testname0", "testtitle #3998", "testdescription1", "testauthor1", LocalDateTime.of(2016, 7, 30, 23, 40, 50), IssueState.NEW.toString(), "", ""));
    }

    private static void setupDb() throws SQLException {
        ITSSqlModule sqlModule = new ITSSqlModule(dbSettings);
        sqlModule.prepareTicketImport(project);
        sqlModule.clearTables(project);

        for (Issue i : issues) {
            i.getCreationDate().toInstant(ZoneOffset.UTC);
            sqlModule.importIssue(i.getId(), i.getName(), i.getTitle(), i.getDescription(), i.getAuthor(), i.getCreationDate(), i.getState().toString(), i.getAssignee(), i.getTargetVersion());
        }
        sqlModule.finishImport();
    }

    @Test
    public void searchInDateRange() {
        ITSSearchParameters parameters = new ITSSearchParameters();
        LocalDateTime start = LocalDateTime.of(2016, 4, 30, 23, 40, 58);
        LocalDateTime end = LocalDateTime.of(2016, 5, 30, 23, 40, 50);
        parameters.setStartDate(start);
        parameters.setEndDate(end);
        List<Issue> result = executeSearchTest(parameters);
        //different drivers are handling the comparison differently
        if (result.size() == 4) {  //this is for drivers where the start date is inclusive
            assertEquals(result.get(0), issues.get(0));
            assertEquals(result.get(1), issues.get(1));
            assertEquals(result.get(2), issues.get(2));
            assertEquals(result.get(3), issues.get(3));
        } else if (result.size() == 3) { //this is for drivers where the start date is exclusive
            assertEquals(result.get(0), issues.get(1));
            assertEquals(result.get(1), issues.get(2));
            assertEquals(result.get(2), issues.get(3));
        } else {
            throw new AssertionError();
        }
    }

    @Test
    public void searchSameAuthor() throws Exception {
        ITSSearchParameters parameters = new ITSSearchParameters();
        parameters.setAuthor("testauthor0");
        List<Issue> result = executeSearchTest(parameters);
        assertTrue(result.size() == 2);
        assertEquals(result.get(0), issues.get(0));
        assertEquals(result.get(1), issues.get(2));
    }

    @Test
    public void searchCommitWithPrefix() throws Exception {
        ITSModule module = new ITSModule() {
            @Override
            protected ITSSearchParameters getWithCommitPrefixSuffix(ITSSearchParameters itsSearchParameters) {
                itsSearchParameters.setCommitPrefix("#");
                return itsSearchParameters;
            }

            @Override
            protected boolean importIssues() {
                return false;
            }

            @Override
            protected List<Issue> onlineSearch(SearchIssueTask task) {
                return null;
            }

            @Override
            protected List<Issue> getOnlineIssue(int id) {
                return null;
            }
        };
        ITSSearchParameters parameters = new ITSSearchParameters();
        parameters.setCommitPrefix("#");
        parameters.setCommit("399");
        module.setDBSettings(dbSettings);
        ITSSettings settings = new ITSSettings();
        settings.setProject(project);
        module.setSettings(settings);
        List<Issue> result = module.search(parameters);
        assertTrue(result.size() == 4);
        assertEquals(result.get(0), issues.get(3));
        assertEquals(result.get(1), issues.get(4));
        assertEquals(result.get(2), issues.get(7));
        assertEquals(result.get(3), issues.get(8));
    }

    @Test
    public void searchCommitWithSuffix() throws Exception {
        ITSModule module = new ITSModule() {
            @Override
            protected ITSSearchParameters getWithCommitPrefixSuffix(ITSSearchParameters itsSearchParameters) {
                itsSearchParameters.setCommitSuffix("*");
                return itsSearchParameters;
            }

            @Override
            protected boolean importIssues() {
                return false;
            }

            @Override
            protected List<Issue> onlineSearch(SearchIssueTask task) {
                return null;
            }

            @Override
            protected List<Issue> getOnlineIssue(int id) {
                return null;
            }
        };
        ITSSearchParameters parameters = new ITSSearchParameters();
        parameters.setCommitSuffix("*");
        parameters.setCommit("399");
        module.setDBSettings(dbSettings);
        ITSSettings settings = new ITSSettings();
        settings.setProject(project);
        module.setSettings(settings);
        List<Issue> result = module.search(parameters);
        assertTrue(result.size() == 4);
        assertEquals(result.get(0), issues.get(5));
        assertEquals(result.get(1), issues.get(6));
        assertEquals(result.get(2), issues.get(7));
        assertEquals(result.get(3), issues.get(8));
    }

    @Test
    public void searchCommitWithPrefixAndSuffix() throws Exception {
        ITSModule module = new ITSModule() {
            @Override
            protected ITSSearchParameters getWithCommitPrefixSuffix(ITSSearchParameters itsSearchParameters) {
                itsSearchParameters.setCommitPrefix("#");
                itsSearchParameters.setCommitSuffix("*");
                return itsSearchParameters;
            }

            @Override
            protected boolean importIssues() {
                return false;
            }

            @Override
            protected List<Issue> onlineSearch(SearchIssueTask task) {
                return null;
            }

            @Override
            protected List<Issue> getOnlineIssue(int id) {
                return null;
            }
        };
        ITSSearchParameters parameters = new ITSSearchParameters();
        parameters.setCommitPrefix("#");
        parameters.setCommitSuffix("*");
        parameters.setCommit("399");
        module.setDBSettings(dbSettings);
        ITSSettings settings = new ITSSettings();
        settings.setProject(project);
        module.setSettings(settings);
        List<Issue> result = module.search(parameters);
        assertTrue(result.size() == 2);
        assertEquals(result.get(0), issues.get(7));
        assertEquals(result.get(1), issues.get(8));
    }

    private List<Issue> executeSearchTest(ITSSearchParameters parameters) {
        ITSModule module = new ITSTestModule();
        module.setDBSettings(dbSettings);
        ITSSettings settings = new ITSSettings();
        settings.setProject(project);
        module.setSettings(settings);
        return module.search(parameters);
    }
}
