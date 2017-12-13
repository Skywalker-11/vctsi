package org.vctsi.svn;

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

import org.junit.Test;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.vctsi.VctsiTest;
import org.vctsi.internal.DBSettings;
import org.vctsi.internal.vcs.Diff;
import org.vctsi.internal.vcs.VCSDiffSettings;
import org.vctsi.internal.vcs.VCSModule;
import org.vctsi.internal.vcs.VCSSettings;
import org.vctsi.utils.OutputUtil;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.vctsi.TestParameters.*;

public class SVNModuleTest extends VctsiTest {

    //@Test
    public void importCommits() throws Exception {
        DBSettings dbSettings = new DBSettings(DB_SERVER, DB_PORT, DB_DB, DB_USER, DB_PASSWORD);

        VCSSettings vcsSettings = new VCSSettings();
        vcsSettings.setProject(SVN_PROJECT1);
        vcsSettings.setLocalPath(SVN_LOCAL1);
        vcsSettings.setRemotePath(SVN_REMOTE1);
        vcsSettings.setUsername(SVN_USER1);
        vcsSettings.setPassword(SVN_PASS1);

        SVNModule module = new SVNModule();
        module.setSettings(vcsSettings);
        module.setDBSettings(dbSettings);
        boolean result = module.importCommits(true);
        assertEquals("", errContent.toString());
        assertEquals(outContent.toString(), OutputUtil.getInfoMessageAsJsonString(VCSModule.IMPORT_SUCCESS) + System.lineSeparator());
        assertTrue(result);
    }

    //@Test
    public void importCommits1() throws Exception {
        DBSettings dbSettings = new DBSettings(DB_SERVER, DB_PORT, DB_DB, DB_USER, DB_PASSWORD);
        VCSSettings vcsSettings = new VCSSettings();
        vcsSettings.setProject("code");
        vcsSettings.setLocalPath("/test/svn/code");
        vcsSettings.setRemotePath(SVN_REMOTE3);

        SVNModule module = new SVNModule();
        module.setSettings(vcsSettings);
        module.setDBSettings(dbSettings);
        boolean result = module.importCommits(true);
        assertEquals("", errContent.toString());
        assertEquals(outContent.toString(), OutputUtil.getInfoMessageAsJsonString(VCSModule.IMPORT_SUCCESS) + System.lineSeparator());
        assertTrue(result);
    }

    //@Test
    public void importCommitsRemote1() throws Exception {
        DBSettings dbSettings = new DBSettings(DB_SERVER, DB_PORT, DB_DB, DB_USER, DB_PASSWORD);
        VCSSettings vcsSettings = new VCSSettings();
        vcsSettings.setProject("code");
        vcsSettings.setRemotePath(SVN_REMOTE3);

        SVNRemoteModule module = new SVNRemoteModule();
        module.setSettings(vcsSettings);
        module.setDBSettings(dbSettings);
        boolean result = module.importCommits(true);
        assertEquals("", errContent.toString());
        assertEquals(outContent.toString(), OutputUtil.getInfoMessageAsJsonString(VCSModule.IMPORT_SUCCESS) + System.lineSeparator());
        assertTrue(result);
    }

    //@Test
    public void importCommits2() throws Exception {
        DBSettings dbSettings = new DBSettings(DB_SERVER, DB_PORT, DB_DB, DB_USER, DB_PASSWORD);
        VCSSettings vcsSettings = new VCSSettings();
        vcsSettings.setProject("openoffice");
        vcsSettings.setLocalPath("/test/svn/openoffice");

        vcsSettings.setRemotePath("http://svn.apache.org/repos/asf/openoffice");

        SVNModule module = new SVNModule();
        module.setSettings(vcsSettings);
        module.setDBSettings(dbSettings);
        boolean result = module.importCommits(true);
        assertEquals("", errContent.toString());
        assertEquals(outContent.toString(), OutputUtil.getInfoMessageAsJsonString(VCSModule.IMPORT_SUCCESS) + System.lineSeparator());
        assertTrue(result);
    }

    //@Test
    public void importCommits3() throws Exception {
        DBSettings dbSettings = new DBSettings(DB_SERVER, DB_PORT, DB_DB, DB_USER, DB_PASSWORD);
        VCSSettings vcsSettings = new VCSSettings();
        vcsSettings.setProject("ace");
        vcsSettings.setRemotePath("http://svn.apache.org/repos/asf/ace");
        vcsSettings.setLocalPath("/test/svn/ace/");

        SVNModule module = new SVNModule();
        module.setSettings(vcsSettings);
        module.setDBSettings(dbSettings);
        boolean result = module.importCommits(true);
        assertEquals("", errContent.toString());
        assertEquals(outContent.toString(), OutputUtil.getInfoMessageAsJsonString(VCSModule.IMPORT_SUCCESS) + System.lineSeparator());
        assertTrue(result);
    }

    //@Test
    public void importCommitsRemote3() throws Exception {
        DBSettings dbSettings = new DBSettings(DB_SERVER, DB_PORT, DB_DB, DB_USER, DB_PASSWORD);
        VCSSettings vcsSettings = new VCSSettings();
        vcsSettings.setProject("ace");
        vcsSettings.setRemotePath("http://svn.apache.org/repos/asf/ace");

        SVNRemoteModule module = new SVNRemoteModule();
        module.setSettings(vcsSettings);
        module.setDBSettings(dbSettings);
        boolean result = module.importCommits(true);
        assertEquals("", errContent.toString());
        assertEquals(outContent.toString(), OutputUtil.getInfoMessageAsJsonString(VCSModule.IMPORT_SUCCESS) + System.lineSeparator());
        assertTrue(result);
    }

    // @Test
    public void testDiffCommits() throws Exception {
        VCSSettings vcsSettings = new VCSSettings();
        vcsSettings.setProject(SVN_PROJECT2);
        vcsSettings.setLocalPath(SVN_REMOTE2);

        VCSDiffSettings diffSettings = new VCSDiffSettings();
        diffSettings.setCommit1("2");
        diffSettings.setCommit2("3");

        SVNModule module = new SVNModule();
        module.setSettings(vcsSettings);
        Diff d = module.getFileDiffs(diffSettings);
        assertNotNull(d);
        assertTrue(errContent.size() == 0);
        assertTrue(outContent.size() == 0);
    }

    //@Test
    public void testRemoteDiffCommits() throws Exception {
        VCSSettings vcsSettings = new VCSSettings();
        vcsSettings.setProject(SVN_PROJECT2);
        vcsSettings.setRemotePath(SVN_REMOTE3);

        VCSDiffSettings diffSettings = new VCSDiffSettings();
        diffSettings.setCommit1("2");
        diffSettings.setCommit2("3");

        SVNRemoteModule module = new SVNRemoteModule();
        module.setSettings(vcsSettings);
        Diff d = module.getFileDiffs(diffSettings);
        assertNotNull(d);
        assertTrue(errContent.size() == 0);
        assertTrue(outContent.size() == 0);
    }

}
