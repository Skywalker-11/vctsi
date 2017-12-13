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

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.wc.SVNDiffClient;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.tmatesoft.svn.core.wc2.SvnCheckout;
import org.tmatesoft.svn.core.wc2.SvnDiff;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnTarget;
import org.vctsi.internal.vcs.Diff;
import org.vctsi.internal.vcs.VCSDiffSettings;
import org.vctsi.internal.vcs.VCSModule;
import org.vctsi.utils.OutputUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.sql.SQLException;
import java.util.Map;

public abstract class ASVNModule extends VCSModule {

    protected String branchRootFolder = "branches";
    protected static final boolean useGitDiffFormat = true;

    /**
     * Creates a diff between revisions and prints it out. The diff for svn requires two valid revisions else an error
     * message will be printed out
     *
     * @param diffSettings the settings for the diff
     * @return returns the diff to the given settings
     */
    @Override
    protected Diff getFileDiffs(VCSDiffSettings diffSettings) {
        if (diffSettings.getCommit1() == null || diffSettings.getCommit2() == null) {
            OutputUtil.printError("error: You have to enter two revisions to get a diff from");
            return null;
        }
        SVNRevision revision1 = SVNRevision.parse(diffSettings.getCommit1());
        SVNRevision revision2 = SVNRevision.parse(diffSettings.getCommit2());
        if (revision1 == SVNRevision.UNDEFINED) {
            OutputUtil.printError("The first revision id is not valid");
            return null;
        }
        if (revision2 == SVNRevision.UNDEFINED) {
            OutputUtil.printError("The second revision id is not valid");
            return null;
        }

        SvnOperationFactory svnOperationFactory = getFactory(vcsSettings.getUsername(), vcsSettings.getPassword());
        if (svnOperationFactory == null) {
            return null;
        }
        String path = "";
        if (diffSettings.getPath() != null) {
            path = "/" + diffSettings.getPath();
        }
        try {
            ByteArrayOutputStream outputStream = getDiff(svnOperationFactory, revision1, revision2, path, new ByteArrayOutputStream());
            if (outputStream == null) {
                return null;
            }
            return new Diff(diffSettings.getCommit1(), diffSettings.getCommit2(), outputStream.toString());
        } catch (SVNException e) {
            OutputUtil.printError("Diff failed: " + e.getMessage());
            return null;
        }
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
    protected abstract ByteArrayOutputStream getDiff(
            SvnOperationFactory svnFactory,
            SVNRevision revision1,
            SVNRevision revision2,
            String path,
            ByteArrayOutputStream outputStream
    ) throws SVNException;

    /**
     * initializes the module
     *
     * @return true if it was successful, false else
     */
    protected boolean initialize() {
        if (vcsSettings.getBranchRootFolder() != null) {
            branchRootFolder = vcsSettings.getBranchRootFolder();
        }
        return true;
    }

    /**
     * finishs the import by flushing the sql connection
     *
     * @throws InterruptedException occures if the importer threads could not be ended successfully
     * @throws SQLException         occures if the sql connection could not be closed or the last import queries contain errors
     */
    protected void finish() throws InterruptedException, SQLException {
        sqlModule.get().finishImport();
    }

    /**
     * creates a factory which contains the given credentials
     *
     * @param username username to log into svn remote
     * @param password password to log into svn remote
     * @return factory providing authentication credentials
     */
    protected SvnOperationFactory getFactory(String username, String password) {
        SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        if (username != null) {
            ISVNAuthenticationManager authManager = getAuthenticationManager(username, password);
            if (authManager == null) {
                return null;
            }
            svnOperationFactory.setAuthenticationManager(authManager);
        }
        return svnOperationFactory;
    }

    /**
     * creates the authentication manager which will handle the login if it is necessary
     *
     * @param username username to login
     * @param password password to login or password for ssh key
     * @return the authentication manager
     */
    protected ISVNAuthenticationManager getAuthenticationManager(String username, String password) {
        boolean options = SVNWCUtil.createDefaultOptions(null, true).isAuthStorageEnabled();
        File sshkey = null;
        if (vcsSettings.getSshKey() != null) {
            sshkey = new File(vcsSettings.getSshKey());
            if (!sshkey.exists()) {
                OutputUtil.printError("-vcsSshKey has to be a file path to a private key");
                return null;
            }
        }

        char[] pass = null;
        if (password != null) {
            pass = password.toCharArray();
        }

        return SVNWCUtil.createDefaultAuthenticationManager(
                null,
                username,
                pass,//will only be used if sshkey is null
                sshkey,
                pass,//will only be used if sshkey is not null
                options
        );
    }

    /**
     * this will import the commit and changed files represented by the logEntry
     *
     * @param logEntry which shall be imported
     * @param branch   the branch this commit belongs to (can be null)
     */
    public void processLogEntry(SVNLogEntry logEntry, String branch) {
        if (logEntry.getRevision() == -1) {
            return;
        }
        try {
            sqlModule.get().importCommit(
                    "" + logEntry.getRevision(),
                    branch,
                    logEntry.getMessage(),
                    logEntry.getAuthor(),
                    logEntry.getDate()
            );
            int i = 0;
            for (Map.Entry<String, SVNLogEntryPath> entry : logEntry.getChangedPaths().entrySet()) {
                SVNLogEntryPath path = entry.getValue();
                String newPath = null;
                String oldPath = null;
                switch (path.getType()) {
                    case SVNLogEntryPath.TYPE_ADDED:
                        newPath = path.getPath();
                        oldPath = path.getCopyPath();
                        break;
                    case SVNLogEntryPath.TYPE_MODIFIED:
                        newPath = path.getPath();
                        oldPath = path.getPath();
                        break;
                    case SVNLogEntryPath.TYPE_DELETED:
                        newPath = null;
                        oldPath = path.getPath();
                        break;
                    case SVNLogEntryPath.TYPE_REPLACED:
                        newPath = path.getPath();
                        oldPath = path.getCopyPath();
                        break;

                }
                sqlModule.get().importDiff(
                        "" + logEntry.getRevision(),
                        (path.getCopyRevision() == -1 ? null : "" + path.getCopyRevision()),
                        i++,
                        newPath,
                        oldPath
                );
            }
        } catch (SQLException e) {
            error += e;
        }
    }

    /**
     * synchronizes the local repo with the remote one
     *
     * @param svnOperationFactory factory providing authentication credentials
     * @return the latest revision received by sync
     * @throws SVNException occures if the sync fails
     */
    protected long sync(SvnOperationFactory svnOperationFactory) throws SVNException {
        SvnCheckout checkout = svnOperationFactory.createCheckout();
        checkout.setSource(SvnTarget.fromURL(SVNURL.parseURIEncoded(vcsSettings.getRemotePath())));
        checkout.setSingleTarget(SvnTarget.fromFile(new File(vcsSettings.getLocalPath())));
        return checkout.run();
    }
}
