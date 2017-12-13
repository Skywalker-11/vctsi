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

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.RevWalkException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.vctsi.internal.vcs.*;
import org.vctsi.utils.OutputUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class GitModule extends VCSModule {
    private static final int threadPoolSize = 10;
    private Git git;
    private Repository repo;
    private VCSThreadSpawner<GitModule, RevCommit> threadSpawner;
    private LinkedBlockingQueue<FileChange> fileChanges = new LinkedBlockingQueue<>();
    private Thread sqlDiffImporterThread;
    private boolean isFinished = false;
    List<String> failedDiffImports = Collections.synchronizedList(new ArrayList<>());


    /**
     * imports commits of a git repository
     *
     * @param noUpdate if true no update of the local repository will be done; else it will be synced with the remote
     * @return true if import is successful, else false
     */
    @Override
    protected boolean importCommits(boolean noUpdate) {
        if (!initializeSql(true) || !initialize(vcsSettings.getLocalPath())) {
            OutputUtil.printError(error);
            return false;
        }
        try {
            if (noUpdate || updateLocalRepo() != null) {
                List<Ref> branches = git.branchList().call();
                for (Ref branch : branches) {
                    if (!branch.isSymbolic()) { //symbolic eg. HEAD cannot be parsed later but should be part of other branch
                        importCommitsFromBranch(branch, null, sqlModule.get());
                    }
                }
                finish();
                if (failedDiffImports.isEmpty()) {
                    return isSuccess();
                } else {
                    OutputUtil.printError("some filechanges could not be generated");
                    return false;
                }
            } else {
                OutputUtil.printError("Error: Could not update repo");
                return false;
            }
        } catch (SQLException | IOException | GitAPIException | InterruptedException e) {
            OutputUtil.printError("Error occured: " + e.getMessage());
            try {//close sql connection if error occured
                finish();
            } catch (InterruptedException | SQLException e1) {
                //ignore
            }
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
        try {
            if (!initializeSql(false) || !initialize(vcsSettings.getLocalPath())) {
                return false;
            }

            HashMap<String, Ref> oldBranches = new HashMap<>();
            for (Ref branch : git.branchList().call()) {
                oldBranches.put(BranchTrackingStatus.of(repo, branch.getName()).getRemoteTrackingBranch(), branch);
            }

            PullResult result = updateLocalRepo();
            if (result != null) {
                Collection<Ref> remotes = result.getFetchResult().getAdvertisedRefs();
                for (Ref remoteBranch : remotes) {
                    //the HEAD and refs/tags branches are ignored
                    if (!remoteBranch.getName().equals("HEAD") && !remoteBranch.getName().startsWith("refs/tags/")) {
                        if (!oldBranches.containsKey(remoteBranch.getName())) {
                            //branch didn't exist yet, so do a full import
                            importCommitsFromBranch(remoteBranch, null, sqlModule.get());
                        } else if (!oldBranches.get(remoteBranch.getName()).getObjectId().equals(remoteBranch.getObjectId())) {
                            //branch was already imported before so only import new commits
                            importCommitsFromBranch(remoteBranch, oldBranches.get(remoteBranch.getName()), sqlModule.get());
                        }
                    }
                }
                finish();
                if (failedDiffImports.isEmpty()) {
                    return isSuccess();
                } else {
                    OutputUtil.printError("some filechanges could not be generated");
                    return false;
                }
            } else {
                OutputUtil.printError("Error: Could not update repo");
                return false;
            }
        } catch (TransportException e) {
            OutputUtil.printError("Check the settings and your internet connection: " + e.getMessage());
            return false;
        } catch (IOException | GitAPIException | SQLException | InterruptedException e) {
            OutputUtil.printError("An error occured" + e.getMessage());
            return false;
        }
    }

    /**
     * gets a diff between two revisions specified in the parameter
     *
     * @param diffSettings settings specifying from which revisions the diff shall be made.
     *                     if no second commit is specified it will return the changes made by the first commit
     * @return a diff object containing the changes between the revisions
     */
    @Override
    protected Diff getFileDiffs(VCSDiffSettings diffSettings) {
        initialize(vcsSettings.getLocalPath());
        if (diffSettings.getCommit1() == null) {
            OutputUtil.printError("You have to set a commit id");
            return null;
        }
        try {
            RevWalk revWalk = new RevWalk(repo);
            RevCommit commit = revWalk.parseCommit(repo.resolve(diffSettings.getCommit1()));
            CanonicalTreeParser newTree = new CanonicalTreeParser();
            CanonicalTreeParser oldTree = new CanonicalTreeParser();
            ObjectReader reader = repo.newObjectReader();
            newTree.reset(reader, commit.getTree().getId());
            if (diffSettings.getCommit2() == null) {
                List<DiffEntry> diffs = new ArrayList<>();
                for (int i = 0; i < commit.getParentCount(); i++) {
                    RevCommit oldCommit = revWalk.parseCommit(commit.getParent(i).getId());
                    oldTree.reset(reader, oldCommit.getTree().getId());
                    diffs.addAll(getDiffs(oldTree, newTree, diffSettings.getPath()));
                }
                return getDiff(diffs, diffSettings.getCommit1(), diffSettings.getCommit2());
            } else {
                RevCommit oldCommit = revWalk.parseCommit(repo.resolve(diffSettings.getCommit2()));
                oldTree.reset(reader, oldCommit.getTree().getId());
                return getDiff(getDiffs(oldTree, newTree, diffSettings.getPath()), diffSettings.getCommit1(), diffSettings.getCommit2());
            }
        } catch (GitAPIException | IOException e) {
            OutputUtil.printError("An error occured:" + e.getMessage());
            return null;
        }
    }

    /**
     * initializes the connection to the git repo and creates a thread spawner
     * that will be used to generate the diff reports while still importing more
     * commits
     *
     * @param localPath the local path of the git repository
     * @return true on success, else false
     */
    private boolean initialize(String localPath) {
        if (localPath == null) {
            OutputUtil.printError("Git requires a localPath set in the -vcsLocalPath");
            return false;
        }
        try {
            git = Git.open(new File(localPath));
            repo = git.getRepository();


            sqlDiffImporterThread = new SqlDiffImporter();
            sqlDiffImporterThread.start();
            threadSpawner = new VCSThreadSpawner<>(this, GitDiffRetriever.class, threadPoolSize);
            threadSpawner.start();
            return true;
        } catch (IOException | ReflectiveOperationException e) {
            OutputUtil.printError("Error connecting to git:" + e.getMessage());
            return false;
        }
    }

    /**
     * finishs the import by waiting for the importer threads to end and
     * flushing the sql connection
     *
     * @throws InterruptedException occures if the importer threads could not be
     *                              ended successfully
     * @throws SQLException         occures if the sql connection could not be closed or
     *                              the last import queries contain errors
     */
    private void finish() throws InterruptedException, SQLException {
        isFinished = true;
        threadSpawner.finish();
        sqlDiffImporterThread.interrupt();
        sqlDiffImporterThread.join();
        if (sqlModule != null) {
            sqlModule.get().finishImport();
        }
        repo.close();
        git.close();
    }

    /**
     * syncs the local repository with the remote one. The remote path will be
     * taken from the
     *
     * @return the result of the update
     */
    private PullResult updateLocalRepo() {
        FetchCommand fetchCommand = git.fetch();
        PullCommand pullCommand = git.pull();
        if (vcsSettings.getUsername() != null) {
            final UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider(
                    vcsSettings.getUsername(),
                    (vcsSettings.getPassword() == null ? "" : vcsSettings.getPassword())
            );
            fetchCommand.setCredentialsProvider(credentials);
            pullCommand.setCredentialsProvider(credentials);
        }
        try {
            fetchCommand.call();
            return pullCommand.call();
        } catch (TransportException e) {
            OutputUtil.printError("You probably have to set username and password: " + e.getMessage());
            return null;
        } catch (GitAPIException e) {
            OutputUtil.printError("Could not get updates from remote repository");
            return null;
        }
    }

    /**
     * creates a diff of two commits (represented by their tree) and filters
     * them
     *
     * @param newTree tree representing newer commit
     * @param oldTree tree representing older commit
     * @param path    file path the diffs should be filtered for; if null they will
     *                not be filtered
     * @return returns the diff entries for the commits filtered by their path
     * @throws GitAPIException error occured while creating the diff of the
     *                         trees
     */
    private List<DiffEntry> getDiffs(CanonicalTreeParser newTree, CanonicalTreeParser oldTree, String path) throws GitAPIException {
        DiffCommand command = git.diff().setNewTree(newTree).setOldTree(oldTree);
        List<DiffEntry> diffs = command.call();
        if (path != null) {
            diffs = filterDiffEntries(diffs, path);
        }
        return diffs;
    }

    /**
     * filters diff entries for their file names
     *
     * @param diffEntries the entries that should be filtered
     * @param path        only diffs on file starting with this will be returned
     * @return the filtered diffEntries
     */
    private List<DiffEntry> filterDiffEntries(List<DiffEntry> diffEntries, String path) {
        List<DiffEntry> filtered = new ArrayList<>();
        for (DiffEntry entry : diffEntries) {
            if (entry.getOldPath().startsWith(path) || entry.getNewPath().startsWith(path)) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    /**
     * imports commits from a branch
     *
     * @param branch    the branch where commits shall be imported for
     * @param oldBranch old local revision of the branch from which on the
     *                  commits shall be imported; if null every commit of the branch is getting
     *                  imported
     * @param sqlModule the sqlmodule to use for importing the commits
     * @throws SQLException         if errors occures while executing sql queries (will
     *                              be retried once before)
     * @throws GitAPIException      error with git while getting the commits
     * @throws IOException          error with git while getting the commits
     * @throws InterruptedException occurs if commit could not be queued
     */
    private void importCommitsFromBranch(Ref branch, Ref oldBranch, VCSSqlModule sqlModule) throws SQLException, GitAPIException, IOException, InterruptedException {
        Repository r = git.getRepository();
        ObjectId repoId = r.resolve(branch.getName());
        LogCommand logCommand = git.log();
        try {
            if (oldBranch == null) {
                logCommand.add(repoId);
            } else {
                logCommand.addRange(repo.resolve(oldBranch.getName()), repo.resolve(branch.getName()));
            }
        } catch (RevWalkException | MissingObjectException e) {
            //branch is not checked out yet so we can't process it.
            return;
        }

        Iterable<RevCommit> commitIterator;
        try {
            //Due to a bug in jgit it occures that after a reboot of the computer the first time this command is run it
            // produces a RevWalkException. On second run this should work fine.
            commitIterator = logCommand.call();
        } catch (RevWalkException e) {
            commitIterator = logCommand.call();
        }

        Iterator<RevCommit> iterator = commitIterator.iterator();
        while (true) {
            RevCommit commit;
            try {
                if (!iterator.hasNext()) {
                    break;
                }
                commit = iterator.next();
            } catch (RevWalkException e) {
                continue;
            }

            sqlModule.importCommit(
                    commit.getName(),
                    branch.getName(),
                    commit.getFullMessage(),
                    commit.getAuthorIdent().getName(),
                    commit.getAuthorIdent().getWhen()
            );
            List<RevCommit> l = new LinkedList<>();
            l.add(commit);
            threadSpawner.putElem(l);
        }

    }

    /**
     * imports the changed files of a commit
     *
     * @param newCommit the commit where the diff entry should be imported for
     */

    void importDiffs(RevCommit newCommit) throws SQLException, GitAPIException, IOException, InterruptedException {
        CanonicalTreeParser newTree = new CanonicalTreeParser();
        CanonicalTreeParser oldTree = new CanonicalTreeParser();
        //only use ObjectReader as local variable, else this will break the log command
        ObjectReader reader = repo.newObjectReader();
        int parentCount = newCommit.getParentCount();
        if (parentCount != 0) { //the commit has a predecessor
            for (int i = 0; i < parentCount; i++) {
                RevCommit oldCommit = newCommit.getParent(i);
                newTree.reset(reader, newCommit.getTree().getId());
                oldTree.reset(reader, oldCommit.getTree().getId());
                List<DiffEntry> diffs = git.diff().setNewTree(newTree).setOldTree(oldTree).setShowNameAndStatusOnly(true).call();

                for (int j = 0; j < diffs.size(); j++) {
                    DiffEntry entry = diffs.get(j);
                    String oldRevision = (entry.getChangeType() == DiffEntry.ChangeType.ADD ? null : oldCommit.getName());
                    String oldPath = (entry.getChangeType() == DiffEntry.ChangeType.ADD ? null : entry.getOldPath());
                    String newPath = (entry.getChangeType() == DiffEntry.ChangeType.DELETE ? null : entry.getNewPath());
                    fileChanges.put(new FileChange(newCommit.getName(), oldRevision, newPath, oldPath, j));
                }
            }
        }
    }

    /**
     * creates a full diff from diffentries
     *
     * @param diffs       each contains a single file diff
     * @param revisionId1 first revision of diff
     * @param revisionId2 second revision of diff
     * @return the diff of all diffentries
     */
    private Diff getDiff(List<DiffEntry> diffs, String revisionId1, String revisionId2) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DiffFormatter df = new DiffFormatter(out);
        df.setRepository(git.getRepository());
        try {
            String diffOutput = "";
            for (DiffEntry diff : diffs) {
                df.format(diff);
                diffOutput += out.toString();
                out.reset();
            }
            return new Diff(revisionId1, revisionId2, diffOutput);
        } catch (IOException e) {
            OutputUtil.printError("An error occured: " + e.getMessage());
            return null;
        }
    }

    /**
     * imports the filechanges in a single thread. this prevents timeouts which may occure if they are done in multiple threads.
     */
    private class SqlDiffImporter extends Thread {
        @Override
        public void run() {
            VCSSqlModule module = sqlModule.get();
            while (!isFinished || !fileChanges.isEmpty()) {
                try {
                    FileChange fileChange = fileChanges.take();
                    module.importDiff(
                            fileChange.getNewCommit(),
                            fileChange.getOldCommit(),
                            fileChange.getNum(),
                            fileChange.getNewName(),
                            fileChange.getOldName()
                    );
                } catch (SQLException e) {
                    error += e.getMessage();
                } catch (InterruptedException e) {
                    //this will be reached if the module is finished and this thread shall stop but is still waiting at
                    //fileChanges.take(); So it will continue to the next round.
                    OutputUtil.debug("interrupted" + e.getMessage());
                } catch (Exception e) {
                    error += e.getMessage();
                }
            }
            try {
                module.finishImport();
            } catch (SQLException e) {
                error += e.getMessage();
            }
        }
    }
}
