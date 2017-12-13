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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;
import org.vctsi.VctsiTest;
import org.vctsi.debug.its.ITSTestModule;
import org.vctsi.internal.tasks.ImportCommitsTask;
import org.vctsi.internal.tasks.ImportIssuesTask;
import org.vctsi.internal.tasks.SearchIssueTask;
import org.vctsi.internal.tasks.Task;
import org.vctsi.utils.OutputUtil;

import java.util.List;

import static org.junit.Assert.*;

public class ITSModuleTest extends VctsiTest {
    @Test
    public void testWrongTaskTarget() throws Exception {
        ITSTestModule module = new ITSTestModule();
        assertFalse(module.executeTask(new ImportCommitsTask()));
        assertEquals(OutputUtil.getErrorMessageAsJsonString("its modules can't handle vcs tasks") + System.lineSeparator(), errContent.toString());
        assertTrue(outContent.size() == 0);
    }

    @Test
    public void testNoProject() throws Exception {
        ITSModule module = new ITSTestModule();
        ITSSettings settings = new ITSSettings();
        module.setSettings(settings);
        assertFalse(module.executeTask(new ImportIssuesTask()));
        assertEquals(OutputUtil.getErrorMessageAsJsonString("its modules require a project to be set in -itsProject") + System.lineSeparator(), errContent.toString());
        assertTrue(outContent.size() == 0);
    }

    @Test
    public void testImportIssuesTaskSuccess() throws JsonProcessingException {
        ITSModule module = new ITSTestModule();
        ITSSettings settings = new ITSSettings();
        settings.setProject("test");
        module.setSettings(settings);
        assertTrue(module.executeTask(new ImportIssuesTask()));
        assertTrue(errContent.size() == 0);
        assertEquals(
                OutputUtil.getInfoMessageAsJsonString(ITSTestModule.importMessage) + System.lineSeparator()
                        + OutputUtil.getInfoMessageAsJsonString(ITSModule.IMPORT_SUCCESS) + System.lineSeparator(),
                outContent.toString()
        );
    }

    @Test
    public void testImportIssuesTaskFailure() throws JsonProcessingException {
        ITSModule module = new ITSModule() {

            @Override
            protected boolean importIssues() {
                OutputUtil.printError("error");
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
        ITSSettings settings = new ITSSettings();
        settings.setProject("test");
        module.setSettings(settings);
        assertFalse(module.executeTask(new ImportIssuesTask()));
        assertEquals(OutputUtil.getErrorMessageAsJsonString("error") + System.lineSeparator(), errContent.toString());
        assertTrue(outContent.size() == 0);
    }

    @Test
    public void testTaskUnknown() throws JsonProcessingException {
        ITSTestModule module = new ITSTestModule();
        ITSSettings settings = new ITSSettings();
        settings.setProject("test");
        module.setSettings(settings);
        class testTask extends Task {
            public testTask() {
                super(TaskTarget.ISSUE, null);
            }

            @Override
            public String toString() {
                return "testTask";
            }
        }
        assertFalse(module.executeTask(new testTask()));
        assertEquals(OutputUtil.getErrorMessageAsJsonString("Unknown task") + System.lineSeparator(), errContent.toString());
        assertTrue(outContent.size() == 0);
    }
}
