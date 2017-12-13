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

import org.junit.Before;
import org.junit.Test;
import org.vctsi.debug.vcs.VCSTestModule;
import org.vctsi.internal.DBSettings;
import org.vctsi.internal.tasks.SearchCommitTask;
import org.vctsi.utils.OutputUtil;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.vctsi.TestParameters.*;

public class VCSModuleSearchTest {
    private static DBSettings dbSettings = new DBSettings(DB_SERVER, DB_PORT, DB_DB, DB_USER, DB_PASSWORD);
    private static String project = "DEBUG";

    private static List<Commit> commits;

    @Before
    public void prepareTest() throws SQLException {
        OutputUtil.DEBUG = false;
        createTestCommits();
        setupDb();
    }

    private static void createTestCommits() {
        commits = new ArrayList<>();
        commits.add(new Commit("a", "testtitle0", "test author0", LocalDateTime.of(2016, 4, 30, 23, 40, 58)));
        commits.add(new Commit("c", "testtitle1", "test author0", LocalDateTime.of(2016, 4, 30, 23, 40, 59)));
        commits.add(new Commit("d", "testtitle1_2", "test author1", LocalDateTime.of(2016, 5, 30, 23, 40, 50)));
        commits.add(new Commit("e", "testtitle issue #3", "test author2", LocalDateTime.of(2016, 5, 30, 23, 40, 50)));
        commits.add(new Commit("f", "testtitle issue #33", "test author3", LocalDateTime.of(2016, 5, 30, 23, 40, 51)));
        commits.add(new Commit("g", "testtitle 399* ", "test author1", LocalDateTime.of(2016, 7, 30, 23, 40, 50)));
        commits.add(new Commit("h", "testtitle 399*,", "test author10", LocalDateTime.of(2016, 7, 30, 23, 40, 50)));
        commits.add(new Commit("i", "testtitle #399*,", "test author1", LocalDateTime.of(2016, 7, 30, 23, 40, 50)));
        commits.add(new Commit("j", "testtitle #399*,", "test author1", LocalDateTime.of(2016, 7, 30, 23, 40, 50)));
        commits.add(new Commit("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "testtitle #3998", "test author0", LocalDateTime.of(2016, 7, 30, 23, 40, 50)));

        AbstractList<String> branches1 = new ArrayList<>();
        branches1.add("branch0");
        branches1.add("branch1");
        AbstractList<String> branches2 = new ArrayList<>();
        branches2.add("branch1");
        branches2.add("branch2");
        AbstractList<String> branches3 = new ArrayList<>();
        branches3.add("branch4");
        commits.get(0).setBranches(branches1);
        commits.get(1).setBranches(branches1);
        commits.get(2).setBranches(branches2);
        commits.get(3).setBranches(branches3);


        AbstractList<FileChange> changes1 = new ArrayList<>();
        changes1.add(new FileChange("d", "file1", "file44"));
        changes1.add(new FileChange("d", "file12", "file442"));
        changes1.add(new FileChange("d", "file13", "file444"));
        AbstractList<FileChange> changes2 = new ArrayList<>();
        changes2.add(new FileChange("e", "file1", "file1"));
        AbstractList<FileChange> changes3 = new ArrayList<>();
        changes3.add(new FileChange("f", "file44", "dev/null"));
        changes3.add(new FileChange("g", "file44", "dev/null"));
        commits.get(0).setChangedFiles(changes1);
        commits.get(1).setChangedFiles(changes2);
        commits.get(2).setChangedFiles(changes3);
    }

    private static void setupDb() throws SQLException {
        VCSSqlModule sqlModule = new VCSSqlModule(dbSettings);
        sqlModule.prepareStmts(project);
        sqlModule.recreateVcsTables(project);

        for (Commit c : commits) {
            if (c.getBranches() != null) {
                for (String branch : c.getBranches()) {
                    sqlModule.importCommit(
                            c.getId(),
                            branch,
                            c.getMessage(),
                            c.getAuthor(),
                            c.getDateTime()
                    );
                }
            } else {
                sqlModule.importCommit(
                        c.getId(),
                        null,
                        c.getMessage(),
                        c.getAuthor(),
                        c.getDateTime()
                );
            }
            int i = 0;
            if (c.getChangedFiles() != null) {
                for (FileChange change : c.getChangedFiles()) {
                    sqlModule.importDiff(
                            c.getId(),
                            change.getOldCommit(),
                            i++,
                            change.getNewName(),
                            change.getOldName()
                    );
                }
            }
        }

        sqlModule.finishImport();
    }

    @Test
    public void searchInDateRange() {
        VCSSearchParameters parameters = new VCSSearchParameters();
        LocalDateTime start = LocalDateTime.of(2016, 4, 30, 23, 40, 58);
        LocalDateTime end = LocalDateTime.of(2016, 5 , 30, 23, 40, 50);
        parameters.setStartDate(start);
        parameters.setEndDate(end);
        List<Commit> result = executeSearchTest(parameters);
        //different drivers are handling the comparison differently
        if (result.size() == 4) {  //this is for drivers where the start date is inclusive
            assertEquals(result.get(0), commits.get(0));
            assertEquals(result.get(1), commits.get(1));
            assertEquals(result.get(2), commits.get(2));
            assertEquals(result.get(3), commits.get(3));
        } else if (result.size() == 3) { //this is for drivers where the start date is exclusive
            assertEquals(result.get(0), commits.get(1));
            assertEquals(result.get(1), commits.get(2));
            assertEquals(result.get(2), commits.get(3));
        } else {
            throw new AssertionError();
        }
    }

    @Test
    public void searchSameAuthor() throws Exception {
        VCSSearchParameters parameters = new VCSSearchParameters();
        parameters.setAuthor("test author0");
        List<Commit> result = executeSearchTest(parameters);
        assertTrue(result.size() == 3);
        assertEquals(result.get(0), commits.get(0));
        assertEquals(result.get(1), commits.get(1));
        assertEquals(result.get(2), commits.get(9));
    }

    @Test
    public void searchForBranches() throws Exception {
        VCSSearchParameters parameters = new VCSSearchParameters();
        parameters.setBranch("branch1");
        List<Commit> result = executeSearchTest(parameters);
        assertTrue(result.size() == 3);
        assertEquals(result.get(0), commits.get(0));
        assertEquals(result.get(1), commits.get(1));
        assertEquals(result.get(2), commits.get(2));

        parameters.setBranch("branch4");
        List<Commit> result2 = executeSearchTest(parameters);
        assertTrue(result2.size() == 1);
        assertEquals(result2.get(0), commits.get(3));

    }

    @Test
    public void searchForFile() throws Exception {
        VCSSearchParameters parameters = new VCSSearchParameters();
        parameters.setFile("file1");
        List<Commit> result = executeSearchTest(parameters);
        assertTrue(result.size() == 2);
        assertEquals(result.get(0), commits.get(0));
        assertEquals(result.get(1), commits.get(1));

        parameters.setFile("file44");
        List<Commit> result2 = executeSearchTest(parameters);
        assertTrue(result2.size() == 2);
        assertEquals(result2.get(0), commits.get(0));
        assertEquals(result2.get(1), commits.get(2));
    }

    private List<Commit> executeSearchTest(VCSSearchParameters parameters) {
        VCSModule module = new VCSTestModule();
        module.setDBSettings(dbSettings);
        VCSSettings settings = new VCSSettings();
        settings.setProject(project);
        module.setSettings(settings);
        SearchCommitTask task = new SearchCommitTask();
        task.setSearchParameters(parameters);
        return module.search(task);
    }
}
