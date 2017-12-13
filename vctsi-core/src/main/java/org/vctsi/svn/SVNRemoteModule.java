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

import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNDiffClient;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.vctsi.internal.vcs.VCSThreadSpawner;
import org.vctsi.utils.OutputUtil;

import java.io.ByteArrayOutputStream;
import java.sql.SQLException;
import java.util.ArrayList;

public class SVNRemoteModule extends ASVNModule implements ISVNLogEntryHandler {

    private String currentBranch;

    /**
     * imports commits of a remote svn repository
     *
     * @param noUpdate will be ignored. not sync will happen
     * @return true if import is successful, else false
     */
    @Override
    protected boolean importCommits(boolean noUpdate) {
        if (!initializeSql(false) || !initialize()) {
            return false;
        }
        try {
            SVNRepository repo = getRepository(vcsSettings.getUsername(), vcsSettings.getPassword());
            importCommits(repo, 1);

            finish();
            return isSuccess();
        } catch (SQLException | SVNException | InterruptedException e) {
            OutputUtil.printError("An error occured" + e.getMessage());
            return false;
        }
    }

    /**
     * this method is not supported
     *
     * @return false
     */
    @Override
    protected boolean importNewCommits() {
        OutputUtil.printError("-onlyNew not supported with online svn");
        return false;
    }


    /**
     * creates a repository which contains the given credentials
     *
     * @param username username to log into svn remote
     * @param password password to log into svn remote
     * @return svn repository
     */
    private SVNRepository getRepository(String username, String password) {
        try {
            SVNRepository repo = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(vcsSettings.getRemotePath()));

            if (username != null) {
                ISVNAuthenticationManager authManager = getAuthenticationManager(username, password);
                if (authManager == null) {
                    return null;
                }
                repo.setAuthenticationManager(authManager);
            }
            return repo;
        } catch (SVNException e) {
            OutputUtil.printError("error occured" + e.getMessage());
        }
        return null;
    }

    /**
     * this method contains code from https://svn.svnkit.com/repos/svnkit/branches/ssh.ping/doc/examples/src/org/tmatesoft/svn/examples/repository/DisplayRepositoryTree.java
     * this will retrieve the actual revisions and queues them for import
     *
     * @param repository  the svn repository
     * @param oldRevision if -1 all revisions will be processed, else only revisions after this one
     * @throws SVNException will be thrown if problems occurs while getting the revision logs
     */
    private void importCommits(SVNRepository repository, long oldRevision) throws SVNException, InterruptedException {
        SVNNodeKind nodeKind = repository.checkPath("", -1);
        if (nodeKind == SVNNodeKind.NONE) {
            OutputUtil.printError("could not find remote path");
        } else if (nodeKind == SVNNodeKind.FILE) {
            OutputUtil.printError("remote path is a file. folder expected");
        }
        ArrayList<SVNDirEntry> entries = new ArrayList<>();
        repository.getDir(branchRootFolder, -1, null, entries);
        for (SVNDirEntry entry : entries) {
            currentBranch = branchRootFolder + "/" + entry.getRelativePath();
            repository.log(new String[]{currentBranch}, oldRevision, -1, true, true, 0, this);
        }
        currentBranch = "trunk";
        repository.log(new String[]{currentBranch}, oldRevision, -1, true, true, 0, this);
    }

    /**
     * this will start the import of the logentry
     *
     * @param logEntry which shall be imported
     */
    @Override
    public void handleLogEntry(SVNLogEntry logEntry) {
        processLogEntry(logEntry, currentBranch);
    }


    /**
     * creates the diff between the two revisions and returns the result in the outputStream
     *
     * @param svnFactory   the factory used to connect to svn
     * @param revision1    first revision
     * @param revision2    second revision
     * @param path         path to the file/folder of which the diff shall be made
     * @param outputStream here shall the results be added
     * @return the outputStream containing the diff
     * @throws SVNException if an error occures while creating the diff
     */
    @Override
    protected ByteArrayOutputStream getDiff(SvnOperationFactory svnFactory,
                                            SVNRevision revision1,
                                            SVNRevision revision2,
                                            String path,
                                            ByteArrayOutputStream outputStream) throws SVNException {
        if (vcsSettings.getRemotePath() == null) {
            OutputUtil.printError("you have to set remotePath when using -vcsModule=svnRemote");
            return null;
        }
        String uri = vcsSettings.getRemotePath() + path;
        SVNDiffClient diffClient = new SVNDiffClient(svnFactory);
        diffClient.setGitDiffFormat(useGitDiffFormat);
        diffClient.doDiff(
                SVNURL.parseURIEncoded(uri),
                revision1,
                SVNURL.parseURIEncoded(uri),
                revision2,
                SVNDepth.INFINITY,
                true,
                outputStream
        );
        return outputStream;
    }
}
