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
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.*;
import org.vctsi.internal.vcs.VCSModule;
import org.vctsi.internal.vcs.VCSThreadSpawner;
import org.vctsi.utils.OutputUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class SVNModule extends ASVNModule implements ISvnObjectReceiver<SVNLogEntry> {

    private List<SVNLogEntry> logEntries = new ArrayList<>();
    private VCSThreadSpawner<ASVNModule, SVNLogEntry> threadSpawner;

    /**
     * imports commits of a local svn working copy
     *
     * @param noUpdate if true no update of the local repository will be done; else it will be synced with the remote
     * @return true if import is successful, else false
     */
    @Override
    protected boolean importCommits(boolean noUpdate) {
        if (!initializeSql(false) || !initialize()) {
            return false;
        }
        try {
            SvnOperationFactory svnOperationFactory = getFactory(vcsSettings.getUsername(), vcsSettings.getPassword());
            SvnTarget localPath = SvnTarget.fromFile(new File(vcsSettings.getLocalPath()));

            if (!noUpdate) {
                sync(svnOperationFactory);
            }
            importCommits(svnOperationFactory, localPath, -1);
            finish();
            return isSuccess();
        } catch (SQLException | SVNException | InterruptedException e) {
            OutputUtil.printError("An error occured" + e.getMessage());
            return false;
        }
    }

    /**
     * imports new commits. this will update the local repository and synchronizes it with the remote.
     * it will only import the new synchronized commits.
     *
     * @return true if import is successful, else false
     */
    @Override
    protected boolean importNewCommits() {
        if (!initializeSql(true) || !initialize()) {
            return false;
        }
        try {
            SvnOperationFactory svnOperationFactory = getFactory(vcsSettings.getUsername(), vcsSettings.getPassword());
            SvnTarget localPath = SvnTarget.fromFile(new File(vcsSettings.getLocalPath()));

            SvnGetInfo status = svnOperationFactory.createGetInfo();
            status.addTarget(localPath);
            SvnInfo info = status.run();
            long currentLocalRevision = info.getRevision();
            long newLocalRevision = sync(svnOperationFactory);
            if (currentLocalRevision != newLocalRevision) {
                importCommits(svnOperationFactory, localPath, currentLocalRevision);
                return isSuccess();
            } else {
                OutputUtil.printInfo(VCSModule.IMPORT_SUCCESS + " Local repository is equal to remote repository");
                return true;
            }
        } catch (SVNException | InterruptedException e) {
            OutputUtil.printError("An error occured: " + e.getMessage());
            return false;
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
    @Override
    protected ByteArrayOutputStream getDiff(
            SvnOperationFactory svnFactory,
            SVNRevision revision1,
            SVNRevision revision2,
            String path,
            ByteArrayOutputStream outputStream) throws SVNException {
        if (vcsSettings.getLocalPath() == null) {
            OutputUtil.printError("you have to set localPath when using -vcsModule=svn");
            return null;
        }
        SvnDiff diffCommand = svnFactory.createDiff();
        diffCommand.setSource(SvnTarget.fromFile(new File(vcsSettings.getLocalPath() + path)), revision1, revision2);
        diffCommand.setUseGitDiffFormat(useGitDiffFormat);
        diffCommand.setOutput(outputStream);
        diffCommand.run();
        return outputStream;
    }


    /**
     * initializes the thread spawner which will be used to process the revisions while the main thread still is getting
     * them from the repo
     *
     * @return true if the spawner could be successfully created, false else
     */
    @Override
    protected boolean initialize() {
        threadSpawner = new VCSThreadSpawner<>(this, SVNLogProcessor.class, 1);
        try {
            threadSpawner.start();
        } catch (ReflectiveOperationException e) {
            OutputUtil.printError("Es ist ein Fehler aufgetreten: " + e.getMessage());
            return false;
        }
        return super.initialize();
    }

    /**
     * finishs the import by waiting for the importer threads to end and flushing the sql connection
     *
     * @throws InterruptedException occures if the importer threads could not be ended successfully
     * @throws SQLException         occures if the sql connection could not be closed or the last import queries contain errors
     */
    protected void finish() throws InterruptedException, SQLException {
        threadSpawner.finish();
        super.finish();
    }


    /**
     * this will retrieve the actual revisions and queues them for import
     *
     * @param svnOperationFactory factory providing authentication credentials
     * @param localPath           the path for which revisions will be retrieved
     * @param oldRevision         if -1 all revisions will be processed, else only revisions after this one
     * @throws SVNException will be thrown if problems occurs while getting the revision logs
     */
    private void importCommits(SvnOperationFactory svnOperationFactory, SvnTarget localPath, long oldRevision) throws SVNException, InterruptedException {
        SvnLog svnLog = svnOperationFactory.createLog();
        svnLog.addTarget(localPath);
        SvnRevisionRange range;
        if (oldRevision != -1) {
            range = SvnRevisionRange.create(SVNRevision.create(oldRevision), SVNRevision.HEAD);
        } else {
            range = SvnRevisionRange.create(SVNRevision.create(1), SVNRevision.HEAD);
        }
        // SVNLogReceiver receiver = new SVNLogReceiver(threadSpawner);
        svnLog.addRange(range);
        svnLog.setDiscoverChangedPaths(true);
        //only return for the actual branch (no not process revisions before creation of the path)
        svnLog.setUseMergeHistory(true);
        svnLog.setStopOnCopy(true);

        //this adds a receiver that will start processing the revisions while still receiving (useful for large repos)
        svnLog.setReceiver(this);

        //starts the receiving
        svnLog.run(new LinkedList<>());
        queueLastEntries();
        //receiver.queueLastEntries();
    }

    /**
     * queues the lest logEntries in the logEntries list for import
     *
     * @throws InterruptedException if the thread is interrupted while waiting for adding the elements to the thread spawner
     */
    private void queueLastEntries() throws InterruptedException {
        threadSpawner.putElem(logEntries);
    }

    /**
     * this will store the logentry in a list and queues a set of 100 of them for import.
     *
     * @param logEntry the logentry to store and import
     */
    @Override
    public void receive(SvnTarget target, SVNLogEntry logEntry) throws SVNException {
        if (logEntries.size() > 100) {
            try {
                threadSpawner.putElem(logEntries);
                logEntries = new ArrayList<>();
            } catch (InterruptedException e) {
                OutputUtil.debug("SVNLogReceiver could not add elemen:t" + e.getMessage());
                //ignore it we will add the element to the list and try to queue it at the next call
            }
        }
        logEntries.add(logEntry);
    }
}
