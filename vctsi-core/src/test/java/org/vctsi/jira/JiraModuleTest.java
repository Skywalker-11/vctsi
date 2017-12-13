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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;
import org.vctsi.VctsiTest;
import org.vctsi.internal.DBSettings;
import org.vctsi.internal.its.ITSModule;
import org.vctsi.internal.its.ITSSearchParameters;
import org.vctsi.internal.its.ITSSettings;
import org.vctsi.internal.its.Issue;
import org.vctsi.internal.tasks.ImportIssuesTask;
import org.vctsi.internal.tasks.SearchIssueTask;
import org.vctsi.utils.OutputUtil;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.vctsi.TestParameters.*;

public class JiraModuleTest extends VctsiTest {

   // @Test
    public void importTest() throws JsonProcessingException {
        JiraModule module = new JiraModule();
        ITSSettings settings = new ITSSettings();
        settings.setProject(JIRA_PROJECT);
        settings.setUsername(JIRA_USER);
        settings.setPassword(JIRA_PASS);
        settings.setPath(JIRA_PATH);
        module.setDBSettings(new DBSettings(DB_SERVER, DB_PORT, DB_DB, DB_USER,  DB_PASSWORD));
        module.setSettings(settings);
        assertTrue(module.importIssues());
        assertTrue(errContent.size() == 0);
        assertTrue(outContent.size() == 0);
    }

   // @Test
    public void importTest2() throws JsonProcessingException {
        JiraModule module = new JiraModule();
        ITSSettings settings = new ITSSettings();
        settings.setProject("PLUG");
        settings.setUsername(JIRA_USER);
        settings.setPassword(JIRA_PASS);
        settings.setPath(JIRA_PATH);
        module.setDBSettings(new DBSettings(DB_SERVER, DB_PORT, DB_DB, DB_USER,  DB_PASSWORD));
        module.setSettings(settings);
        boolean result = module.importIssues();
        assertEquals(errContent.toString(), "");
        assertEquals(outContent.toString(), "");
        assertTrue(result);
    }

    //@Test
    public void onlineSearchTest() throws JsonProcessingException {
        JiraModule module = new JiraModule();
        ITSSettings settings = new ITSSettings();
        settings.setProject(JIRA_PROJECT);
        settings.setUsername(JIRA_USER);
        settings.setPassword(JIRA_PASS);
        settings.setPath(JIRA_PATH);

        module.setSettings(settings);
        ITSSearchParameters parameters = new ITSSearchParameters();
        parameters.setDescription("change");
        SearchIssueTask task = new SearchIssueTask();
        task.setSearchParameters(parameters);
        List<Issue> issues = module.onlineSearch(task);
        assertTrue(!issues.isEmpty());
        assertTrue(errContent.size() == 0);
        assertTrue(outContent.size() == 0);
    }

    @Test
    public void testEscaping() {
        JiraModule module = new JiraModule();
        assertEquals("s\\\\ q\\\\\" sq\\'", module.escapeString("s\\ q\" sq'"));
    }
}
