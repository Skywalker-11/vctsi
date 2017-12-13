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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.vctsi.TestParameters.*;

public class BugzillaModuleTest extends VctsiTest {

    private DBSettings dbSettings = new DBSettings(DB_SERVER, DB_PORT, DB_DB, DB_USER, DB_PASSWORD);

   // @Test
    public void testImportIssues() throws JsonProcessingException {
        ITSSettings settings = new ITSSettings();
        settings.setPath(BUGZILLA_PATH1);
        settings.setProject(BUGZILLA_PROJECT1);
        BugzillaModule module = new BugzillaModule();
        module.setSettings(settings);
        module.setDBSettings(dbSettings);
        assertTrue(module.importIssues());
        assertTrue(errContent.size() == 0);
        assertTrue(outContent.size() == 0);
    }

   // @Test
    public void testImportIssues2() throws JsonProcessingException {
        ITSSettings settings = new ITSSettings();
        settings.setPath("https://bugzilla.mozilla.org/");
        settings.setProject("AUS Graveyard");
        BugzillaModule module = new BugzillaModule();
        module.setSettings(settings);
        module.setDBSettings(dbSettings);
        assertTrue(module.importIssues());
        assertTrue(errContent.size() == 0);
        assertTrue(outContent.size() == 0);
    }
  //  @Test
    public void testImportIssues3() throws JsonProcessingException {
        ITSSettings settings = new ITSSettings();
        settings.setPath(BUGZILLA_PATH3);
        settings.setProject(BUGZILLA_PROJECT3);
        BugzillaModule module = new BugzillaModule();
        module.setSettings(settings);
        module.setDBSettings(dbSettings);
        assertTrue(module.importIssues());
        assertTrue(errContent.size() == 0);
        assertTrue(outContent.size() == 0);
    }
  //  @Test
    public void testImportIssues4() throws JsonProcessingException {
        ITSSettings settings = new ITSSettings();
        settings.setPath("https://bugzilla.mozilla.org/");
        settings.setProject("Rhino");
        BugzillaModule module = new BugzillaModule();
        module.setSettings(settings);
        module.setDBSettings(dbSettings);
        assertTrue(module.importIssues());
        assertTrue(errContent.size() == 0);
        assertTrue(outContent.size() == 0);
    }
   // @Test
    public void testSearchIssues1() {
        ITSSettings settings = new ITSSettings();
        settings.setPath(BUGZILLA_PATH2);
        settings.setProject(BUGZILLA_PROJECT2);
        BugzillaModule module = new BugzillaModule();
        module.setSettings(settings);
        module.setDBSettings(dbSettings);
        ITSSearchParameters parm = new ITSSearchParameters();
        parm.setTitle(BUGZILLA_TESTTITLE);
        parm.setDescription(BUGZILLA_TESTTITLE_2);
        SearchIssueTask task = new SearchIssueTask();
        task.setSearchParameters(parm);
        List<Issue> bugs = module.onlineSearch(task);
        assertNotNull(bugs);
        assertTrue(!bugs.isEmpty());
        assertTrue(errContent.size() == 0);
        assertTrue(outContent.size() == 0);
    }
}
