package org.vctsi.git;

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
import org.vctsi.internal.tasks.GetDiffsTask;
import org.vctsi.internal.tasks.ImportCommitsTask;
import org.vctsi.internal.vcs.VCSDiffSettings;
import org.vctsi.internal.vcs.VCSModule;
import org.vctsi.internal.vcs.VCSSettings;
import org.vctsi.utils.OutputUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.vctsi.TestParameters.*;

public class GitModuleTest extends VctsiTest {

    private DBSettings dbSettings = new DBSettings(DB_SERVER, DB_PORT, DB_DB, DB_USER, DB_PASSWORD);

   // @Test
    public void testImportBootstrap() throws JsonProcessingException {
        GitModule module = new GitModule();
        VCSSettings vcsSettings = new VCSSettings();
        vcsSettings.setProject("bootstrap");
        vcsSettings.setLocalPath(GIT_BOOTSTRAP_PATH);

        module.setSettings(vcsSettings);
        module.setDBSettings(dbSettings);
        boolean result = module.importCommits(true);
        assertEquals("", errContent.toString());
        assertEquals(outContent.toString(), OutputUtil.getInfoMessageAsJsonString(VCSModule.IMPORT_SUCCESS) + System.lineSeparator());
        assertTrue(result);
    }

    //@Test
    public void testImport3() throws JsonProcessingException {
        GitModule module = new GitModule();
        VCSSettings vcsSettings = new VCSSettings();
        vcsSettings.setProject("nextcloudcontacts");
        vcsSettings.setLocalPath("E:\\git_test\\contacts");

        module.setSettings(vcsSettings);
        module.setDBSettings(dbSettings);
        boolean result = module.importCommits(false);
        assertEquals("", errContent.toString());
        assertEquals(outContent.toString(), OutputUtil.getInfoMessageAsJsonString(VCSModule.IMPORT_SUCCESS) + System.lineSeparator());
        assertTrue(result);
    }

   // @Test
    public void testImportNewSdl() throws JsonProcessingException {
        GitModule module = new GitModule();
        VCSSettings vcsSettings = new VCSSettings();
        vcsSettings.setProject("nextcloudmail");
        vcsSettings.setLocalPath("E:\\git_test\\mail");
        vcsSettings.setOnlyNew(true);
        vcsSettings.setUsername(GIT_SM_USER);
        vcsSettings.setPassword(GIT_SM_PASS);

        module.setSettings(vcsSettings);
        module.setDBSettings(dbSettings);
        assertTrue(module.importCommits(false));
        assertTrue(errContent.size() == 0);
        assertEquals(outContent.toString(), OutputUtil.getInfoMessageAsJsonString(VCSModule.IMPORT_SUCCESS) + System.lineSeparator());
    }
    //@Test
    public void testImportFz() throws JsonProcessingException {
        GitModule module = new GitModule();
        VCSSettings vcsSettings = new VCSSettings();
        vcsSettings.setProject(GIT_SM_PROJECT);
        vcsSettings.setLocalPath("e:\\git_test\\fz");
        vcsSettings.setUsername(GIT_SM_USER);
        vcsSettings.setPassword(GIT_SM_PASS);

        module.setSettings(vcsSettings);
        module.setDBSettings(dbSettings);
        module.importCommits(true);
        assertEquals("", errContent.toString());
        assertEquals(outContent.toString(), OutputUtil.getInfoMessageAsJsonString(VCSModule.IMPORT_SUCCESS) + System.lineSeparator());
    }

   // @Test
    public void testDiff() {
        GitModule module = new GitModule();
        VCSSettings vcsSettings = new VCSSettings();
        vcsSettings.setProject("repogit");
        vcsSettings.setLocalPath("e:\\git_test\\repo");

        module.setSettings(vcsSettings);

        VCSDiffSettings diffSettings = new VCSDiffSettings();
        diffSettings.setCommit1("7bd86d147c1171fb52a159a4129757389243dc15");
        diffSettings.setCommit2("b946b66591ab82cc513bb258bfd506bcf04dc56f");

        GetDiffsTask task = new GetDiffsTask();
        task.setDiffSettings(diffSettings);
        module.executeTask(task);
        assertTrue(errContent.size() == 0);
        assertTrue(outContent.size() > 0);
    }

}
