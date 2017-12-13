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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;
import org.vctsi.VctsiTest;
import org.vctsi.internal.DBSettings;
import org.vctsi.internal.its.ITSSearchParameters;
import org.vctsi.internal.its.ITSSettings;
import org.vctsi.internal.its.Issue;
import org.vctsi.internal.tasks.SearchIssueTask;

import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.vctsi.TestParameters.*;

public class GitLabModuleTest extends VctsiTest {

    //@Test
    public void testImportIssues() throws JsonProcessingException {
        ITSSettings settings = new ITSSettings();
        settings.setPath(GITLAB_PATH);
        settings.setProject(GITLAB_PROJECT);
        settings.setUsername("token");
        settings.setPassword(GITLAB_TOKEN);
        GitLabModule module = new GitLabModule();
        module.setSettings(settings);
        module.setDBSettings(new DBSettings(DB_SERVER, DB_PORT, DB_DB, DB_USER, DB_PASSWORD));
        assertTrue(module.importIssues());
        assertTrue(errContent.size() == 0);
        assertTrue(outContent.size() == 0);
    }

   // @Test
    public void testImportIssues1() throws JsonProcessingException {
        ITSSettings settings = new ITSSettings();
        settings.setPath(GITLAB_PATH);
        settings.setProject("gitlab-org/gitlab-ee");
        settings.setUsername("token");
        settings.setPassword(GITLAB_TOKEN);
        GitLabModule module = new GitLabModule();
        module.setSettings(settings);
        module.setDBSettings(new DBSettings(DB_SERVER, DB_PORT, DB_DB, DB_USER, DB_PASSWORD));
        boolean result = module.importIssues();
        assertTrue(errContent.size() == 0);
        assertTrue(outContent.size() == 0);
        assertTrue(result);
    }

   // @Test
    public void testImportIssues2() throws JsonProcessingException {
        ITSSettings settings = new ITSSettings();
        settings.setPath(GITLAB_PATH);
        settings.setProject("GitLabRGI/swagd");
        settings.setUsername("token");
        settings.setPassword(GITLAB_TOKEN);
        GitLabModule module = new GitLabModule();
        module.setSettings(settings);
        module.setDBSettings(new DBSettings(DB_SERVER, DB_PORT, DB_DB, DB_USER, DB_PASSWORD));
        boolean result = module.importIssues();
        assertTrue(errContent.size() == 0);
        assertTrue(outContent.size() == 0);
        assertTrue(result);
    }

    //@Test
    public void testGetOnlineIssue() {
        ITSSettings settings = new ITSSettings();
        settings.setPath(GITLAB_PATH);
        settings.setProject("gitlab-org/gitlab-ee");
        settings.setUsername("token");
        settings.setPassword(GITLAB_TOKEN);
        GitLabModule module = new GitLabModule();
        module.setSettings(settings);
        List<Issue> issues = module.getOnlineIssue(3374653);
        assertTrue(!issues.isEmpty());
        assertTrue(errContent.size() == 0);
        assertTrue(outContent.size() == 0);
    }
    //@Test
    public void testGetOnlineIssues() {
        ITSSettings settings = new ITSSettings();
        settings.setPath(GITLAB_PATH);
        settings.setProject(GITLAB_PROJECT);
        settings.setUsername("token");
        settings.setPassword(GITLAB_TOKEN);
        GitLabModule module = new GitLabModule();
        module.setSettings(settings);
        ITSSearchParameters parameters = new ITSSearchParameters();
        parameters.setTitle("gg");
        SearchIssueTask task = new SearchIssueTask();
        task.setSearchParameters(parameters);
        List<Issue> issues = module.onlineSearch(task);
        assertTrue(!issues.isEmpty());
        assertTrue(errContent.size() == 0);
        assertTrue(outContent.size() == 0);
    }
}
