package org.vctsi.utils;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vctsi.internal.DBSettings;
import org.vctsi.internal.its.ITSSearchParameters;
import org.vctsi.internal.its.ITSSettings;
import org.vctsi.internal.tasks.*;
import org.vctsi.internal.vcs.VCSDiffSettings;
import org.vctsi.internal.vcs.VCSSearchParameters;
import org.vctsi.internal.vcs.VCSSettings;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;

import static org.junit.Assert.*;
import static org.vctsi.utils.ArgumentParser.ERROR_PARSE_INTEGERS;

public class ArgumentParserTest {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private PrintStream out;
    private PrintStream err;

    @Before
    public void setUpStreams() {
        out = System.out;
        err = System.err;
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @After
    public void cleanUpStreams() {
        System.setOut(out);
        System.setErr(err);

    }

    @Test
    public void testNoParams() throws JsonProcessingException {
        ArgumentParser ap = new ArgumentParser();
        assertFalse(ap.parse(new String[]{}));
        assertEquals(OutputUtil.getErrorMessageAsJsonString(ArgumentParser.ERROR_NO_TASKS_GIVEN) + System.lineSeparator(), errContent.toString());
        assertTrue(outContent.size() == 0);
    }

    @Test
    public void testParamInvalid() throws JsonProcessingException {
        String ident = "-blabla";
        ArgumentParser ap = new ArgumentParser();
        assertFalse(ap.parse(new String[]{
                ident + "=j"
        }));
        assertEquals(OutputUtil.getErrorMessageAsJsonString(ArgumentParser.ERROR_UNKNOWN_IDENTIFIER + ident) + System.lineSeparator(), errContent.toString());
        assertTrue(outContent.size() == 0);
    }

    @Test
    public void testHelpText() {
        ArgumentParser ap = new ArgumentParser();
        assertFalse(ap.parse(new String[]{
                "-h"
        }));
        assertTrue(outContent.toString().startsWith(ArgumentParser.helpText1));
        assertTrue(outContent.toString().contains(ArgumentParser.helpTextPart2));
        assertTrue(outContent.toString().endsWith(ArgumentParser.helpTextPart3));
        assertTrue(errContent.size() == 0);
    }

    @Test
    public void testNoTasks() throws JsonProcessingException {
        ArgumentParser ap = new ArgumentParser();
        assertFalse(ap.parse(new String[]{
                "-vcsLocalPath=localhost", "-vcsModule=VCSTestModule",
                "-itsPath=localhost", "-itsModule=ITSTestModule"
        }));
        assertEquals(OutputUtil.getErrorMessageAsJsonString(ArgumentParser.ERROR_NO_TASKS_GIVEN) + System.lineSeparator(), errContent.toString());
        assertTrue(outContent.size() == 0);
    }

    @Test
    public void testVcsModuleInvalid() throws JsonProcessingException {
        ArgumentParser ap = new ArgumentParser();
        assertFalse(ap.parse(new String[]{
                "-vcsModule=blablamodule"
        }));
        assertEquals(OutputUtil.getErrorMessageAsJsonString(ArgumentParser.ERROR_PARSE_VCS_MODULE) + System.lineSeparator(), errContent.toString());
        assertTrue(outContent.size() == 0);
    }

    @Test
    public void testSearchCommitEndDateInvalid() {
        ArgumentParser ap = new ArgumentParser();
        assertFalse(ap.parse(new String[]{
                "-searchCommitEndDate=abc"
        }));
        assertTrue(errContent.toString().contains(ArgumentParser.ERROR_PARSE_DATETIME));
        assertTrue(outContent.size() == 0);
    }

    @Test
    public void testSearchCommitStartDateInvalid() {
        ArgumentParser ap = new ArgumentParser();
        assertFalse(ap.parse(new String[]{
                "-searchCommitStartDate=abc"
        }));
        assertTrue(errContent.toString().contains(ArgumentParser.ERROR_PARSE_DATETIME));
        assertTrue(outContent.size() == 0);
    }

    @Test
    public void testSearchIssueEndDateInvalid() {
        ArgumentParser ap = new ArgumentParser();
        assertFalse(ap.parse(new String[]{
                "-searchIssueEndDate=abc"
        }));
        assertTrue(errContent.toString().contains(ArgumentParser.ERROR_PARSE_DATETIME));
        assertTrue(outContent.size() == 0);
    }

    @Test
    public void testSearchIssueStartDateInvalid() {
        ArgumentParser ap = new ArgumentParser();
        assertFalse(ap.parse(new String[]{
                "-searchIssueStartDate=abc"
        }));
        assertTrue(errContent.toString().contains(ArgumentParser.ERROR_PARSE_DATETIME));
        assertTrue(outContent.size() == 0);
    }

    @Test
    public void testImportCommitsValid() {
        ArgumentParser ap = new ArgumentParser();
        assertTrue(ap.parse(new String[]{
                "-importCommits"
        }));
        assertEquals(ap.getTask(), new ImportCommitsTask());
        assertTrue(errContent.size() == 0);
        assertTrue(outContent.size() == 0);
    }

    @Test
    public void testImportIssuesValid() {
        ArgumentParser ap = new ArgumentParser();
        assertTrue(ap.parse(new String[]{
                "-importIssues"
        }));
        assertEquals(ap.getTask(), new ImportIssuesTask());
        assertTrue(errContent.size() == 0);
        assertTrue(outContent.size() == 0);
    }

    @Test
    public void testSearchIssueIdsInvalid() throws JsonProcessingException {
        ArgumentParser ap = new ArgumentParser();
        assertFalse(ap.parse(new String[]{
                "-searchIssueIds=2,c"
        }));
        assertTrue(errContent.toString().equals(OutputUtil.getErrorMessageAsJsonString(ERROR_PARSE_INTEGERS + "-searchIssueIds") + System.lineSeparator()));
        assertTrue(outContent.size() == 0);
    }

    @Test
    public void testGetDiffValid() {
        ArgumentParser ap = new ArgumentParser();
        assertTrue(ap.parse(new String[]{
                "-getDiffsCommit1=commit123",
                "-getDiffsCommit2=commit456",
                "-getDiffsPath=path123"
        }));
        GetDiffsTask task = new GetDiffsTask();
        VCSDiffSettings diffSettings = new VCSDiffSettings();
        diffSettings.setCommit1("commit123");
        diffSettings.setCommit2("commit456");
        diffSettings.setPath("path123");
        task.setDiffSettings(diffSettings);
        assertEquals(ap.getTask(), task);
        assertTrue(errContent.size() == 0);
        assertTrue(outContent.size() == 0);
    }


    @Test
    public void testSearchIssueValid() throws JsonProcessingException {
        ArgumentParser ap = new ArgumentParser();
        assertTrue(ap.parse(new String[]{
                "-searchIssueAssignee=assignee1",
                "-searchIssueAuthor=author1",
                "-searchIssueCommit=commit1",
                "-searchIssueDescription=desc1",
                "-searchIssueStartDate=2016-01-20T13:30:30",
                "-searchIssueEndDate=2016-01-21T13:30:30",
                "-searchIssueIds=123,234,345",
                "-searchIssueNames=abc,bcd,cde",
                "-searchIssueState=state1",
                "-searchIssueTargetVersion=version1",
                "-searchIssueTitle=title1",
                "-vcsModule=VCSTestModule"
        }));
        ITSSearchParameters parameters = new ITSSearchParameters();
        parameters.setAssignee("assignee1");
        parameters.setAuthor("author1");
        parameters.setCommit("commit1");
        parameters.setDescription("desc1");
        parameters.setStartDate(LocalDateTime.of(2016, 1, 20, 13, 30, 30));
        parameters.setEndDate(LocalDateTime.of(2016, 1, 21, 13, 30, 30));
        parameters.setIds(new Integer[]{123, 234, 345});
        parameters.setNames(new String[]{"abc", "bcd", "cde"});
        parameters.setState("state1");
        parameters.setTargetVersion("version1");
        parameters.setTitle("title1");
        SearchIssueTask task = new SearchIssueTask();
        task.setSearchParameters(parameters);
        assertTrue(errContent.toString().length() == 0);
        assertTrue(outContent.toString().length() == 0);
        assertEquals(ap.getTask(), task);
    }

    @Test
    public void testSearchCommitValid() throws JsonProcessingException {
        ArgumentParser ap = new ArgumentParser();
        assertTrue(ap.parse(new String[]{
                "-searchCommitMessage=abc",
                "-searchCommitAuthor=author1",
                "-searchCommitBranch=branch1",
                "-searchCommitFile=file1",
                "-searchCommitTicket=1234",
                "-searchCommitStartCommit=ab123456",
                "-searchCommitEndCommit=ab234567",
                "-searchCommitIds=id123,id234,id345",
                "-searchCommitStartDate=2016-01-20T13:30:30",
                "-searchCommitEndDate=2016-01-21T13:30:30",
                "-vcsModule=VCSTestModule"
        }));
        VCSSearchParameters parameters = new VCSSearchParameters();
        parameters.setMessage("abc");
        parameters.setAuthor("author1");
        parameters.setBranch("branch1");
        parameters.setFile("file1");
        parameters.setTicket("1234");
        parameters.setStartCommit("ab123456");
        parameters.setEndCommit("ab234567");
        parameters.setStartDate(LocalDateTime.of(2016, 1, 20, 13, 30, 30));
        parameters.setEndDate(LocalDateTime.of(2016, 1, 21, 13, 30, 30));
        parameters.setIds(new String[]{"id123", "id234", "id345"});
        SearchCommitTask task = new SearchCommitTask();
        task.setSearchParameters(parameters);
        assertTrue(errContent.toString().length() == 0);
        assertTrue(outContent.toString().length() == 0);
        assertEquals(ap.getTask(), task);
    }


    @Test
    public void testGetCommitValid() throws JsonProcessingException {
        String commit = "commit1245";
        ArgumentParser ap = new ArgumentParser();
        assertTrue(ap.parse(new String[]{
                "-vcsLocalPath=localhost",
                "-getCommit=" + commit,
                "-vcsModule=VCSTestModule"
        }));
        GetCommitTask commitTask = new GetCommitTask(commit);
        assertTrue(errContent.toString().length() == 0);
        assertTrue(outContent.toString().length() == 0);
        assertTrue(ap.getTask() instanceof GetCommitTask);
        assertEquals(ap.getTask(), commitTask);
    }

    @Test
    public void testItsModuleInvalid() throws JsonProcessingException {
        ArgumentParser ap = new ArgumentParser();
        assertFalse(ap.parse(new String[]{
                "-itsModule=blablamodule"
        }));
        assertEquals(OutputUtil.getErrorMessageAsJsonString(ArgumentParser.ERROR_PARSE_ITS_MODULE) + System.lineSeparator(), errContent.toString());
        assertTrue(outContent.size() == 0);
    }


    @Test
    public void testGetIssueValid() throws JsonProcessingException {
        int issue = 1245;
        ArgumentParser ap = new ArgumentParser();
        assertTrue(ap.parse(new String[]{
                "-itsPath=localhost",
                "-getIssue=" + issue,
                "-itsModule=ITSTestModule"
        }));
        GetIssueTask issueTask = new GetIssueTask(issue);
        assertTrue(errContent.toString().length() == 0);
        assertTrue(outContent.toString().length() == 0);
        assertEquals(ap.getTask(), issueTask);
    }

    @Test
    public void testGetIssueInvalid() throws JsonProcessingException {
        ArgumentParser ap = new ArgumentParser();
        assertFalse(ap.parse(new String[]{
                "-itsPath=localhost",
                "-getIssue=issue",
                "-itsModule=ITSTestModule"
        }));
        assertTrue(errContent.toString().contains(ArgumentParser.ERROR_ISSUEID_NOT_A_NUMBER));
        assertTrue(outContent.toString().length() == 0);
    }

    @Test
    public void testDBSettingsValid() throws JsonProcessingException {
        ArgumentParser ap = new ArgumentParser();
        assertTrue(ap.parse(new String[]{
                "-itsPath=localhost",
                "-getIssue=1245",
                "-itsModule=ITSTestModule",
                "-dbServer=localhost1", "-dbPort=3307", "-dbUsername=user1", "-dbPassword=pass123", "-dbDatabase=vctsi2"
        }));
        assertTrue(errContent.toString().length() == 0);
        assertTrue(outContent.toString().length() == 0);
        DBSettings dbSettings = new DBSettings("localhost1", 3307, "vctsi2", "user1", "pass123");
        assertEquals(ap.getDbSettings(), dbSettings);
    }

    @Test
    public void testDBPortInvalid() throws JsonProcessingException {
        ArgumentParser ap = new ArgumentParser();
        assertFalse(ap.parse(new String[]{
                "-itsPath=localhost",
                "-getIssue=1245",
                "-itsModule=ITSTestModule",
                "-dbPort=test}"
        }));
        assertTrue(errContent.toString().contains(ArgumentParser.ERROR_PORT_NOT_A_NUMBER));
        assertTrue(outContent.toString().length() == 0);
    }


    @Test
    public void testITSSettingsValid() {
        ArgumentParser ap = new ArgumentParser();
        assertTrue(ap.parse(new String[]{
                "-itsUsername=user",
                "-itsPassword=pass123",
                "-itsPath=c:\\TEST",
                "-itsProject=proj1",
                "-itsOnline=true",
                "-getIssue=123",
                "-itsModule=ITSTestModule"
        }));
        assertTrue(errContent.toString().length() == 0);
        assertTrue(outContent.toString().length() == 0);
        ITSSettings itsSettings = new ITSSettings();
        itsSettings.setProject("proj1");
        itsSettings.setUsername("user");
        itsSettings.setPassword("pass123");
        itsSettings.setPath("c:\\TEST");
        itsSettings.setOnline(true);
        assertEquals(ap.getItsSettings(), itsSettings);
        assertTrue(errContent.size() == 0);
        assertTrue(outContent.size() == 0);
    }

    @Test
    public void testVCSSettingsValid() {
        ArgumentParser ap = new ArgumentParser();
        assertTrue(ap.parse(new String[]{
                "-vcsUsername=user", "-vcsPassword=pass123",
                "-vcsLocalPath=c:\\TEST", "-vcsProject=proj1",
                "-vcsBranchRootFolder=root/branches", "-vcsOnlyNew=true",
                "-vcsRemotePath=http://TEST2",
                "-vcsNoUpdate=true",
                "-vcsSshKey=sshkey2349v",
                "-getCommit=ca123",
                "-itsModule=ITSTestModule"
        }));
        assertTrue(errContent.toString().length() == 0);
        assertTrue(outContent.toString().length() == 0);
        VCSSettings vcsSettings = new VCSSettings();
        vcsSettings.setProject("proj1");
        vcsSettings.setUsername("user");
        vcsSettings.setPassword("pass123");
        vcsSettings.setOnlyNew(true);
        vcsSettings.setNoUpdate(true);
        vcsSettings.setLocalPath("c:\\TEST");
        vcsSettings.setRemotePath("http://TEST2");
        vcsSettings.setBranchRootFolder("root/branches");
        vcsSettings.setSshKey("sshkey2349v");
        assertEquals(ap.getVcsSettings(), vcsSettings);
        assertTrue(errContent.size() == 0);
        assertTrue(outContent.size() == 0);
    }
}
