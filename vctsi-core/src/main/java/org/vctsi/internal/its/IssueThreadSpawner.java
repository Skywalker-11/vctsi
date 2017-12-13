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

import org.vctsi.internal.DBSettings;
import org.vctsi.utils.OutputUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.newFixedThreadPool;

public class IssueThreadSpawner<ISSUE, COMMENT> {
    private final int threadPoolSize;

    private ExecutorService executor;

    private LinkedBlockingQueue<IssueQueueElement> queue;
    private IssueConverter<ISSUE> issueConverter;
    private CommentRetriever<COMMENT, ISSUE> commentRetriever;
    private CommentConverter<COMMENT, ISSUE> commentConverter;
    private IssueFilter<ISSUE> issueFilter = null;
    private DBSettings dbSettings;
    private String errors = "";
    private String project;
    private boolean stop = false;
    private HashMap<Integer, List<Issue>> results;


    /**
     * creates a thread spawner that can be used to start threads for processing issues and commits
     *
     * @param issueConverter   a converter for issues
     * @param commentRetriever a utility that can be used to receive the comments for issues
     * @param commentConverter a converter for comments
     * @param dbSettings       database settings
     * @param project          the project of which data is processed
     * @param threadPoolSize   the number of worker threads that shall be created
     * @param doImport         if true the issues will be imported, if false they will stored and can be accessed by getResults()
     */
    public IssueThreadSpawner(
            IssueConverter<ISSUE> issueConverter,
            CommentRetriever<COMMENT, ISSUE> commentRetriever,
            CommentConverter<COMMENT, ISSUE> commentConverter,
            DBSettings dbSettings,
            String project,
            int threadPoolSize,
            boolean doImport) {
        this.queue = new LinkedBlockingQueue<>();
        this.issueConverter = issueConverter;
        this.commentRetriever = commentRetriever;
        this.commentConverter = commentConverter;
        this.dbSettings = dbSettings;
        this.project = project;
        this.threadPoolSize = threadPoolSize;
        this.executor = newFixedThreadPool(threadPoolSize);
        if (!doImport) {
            results = new HashMap<>();
        }
        start(doImport);
    }

    /**
     * creates a thread spawner that can be used to start threads for processing issues and commits
     *
     * @param issueConverter   a converter for issues
     * @param issueFilter      a filter that can be used to filter issues locally; can be null if they shall not be filtered
     * @param commentRetriever a utility that can be used to receive the comments for issues
     * @param commentConverter a converter for comments
     * @param dbSettings       database settings
     * @param project          the project of which data is processed
     * @param threadPoolSize   the number of worker threads that shall be created
     * @param doImport         if true the issues will be imported, if false they will stored and can be accessed by getResults()
     */
    public IssueThreadSpawner(IssueConverter<ISSUE> issueConverter,
                              IssueFilter<ISSUE> issueFilter,
                              CommentRetriever<COMMENT, ISSUE> commentRetriever,
                              CommentConverter<COMMENT, ISSUE> commentConverter,
                              DBSettings dbSettings,
                              String project,
                              int threadPoolSize,
                              boolean doImport) {
        this.issueFilter = issueFilter;
        this.queue = new LinkedBlockingQueue<>();
        this.issueConverter = issueConverter;
        this.commentRetriever = commentRetriever;
        this.commentConverter = commentConverter;
        this.dbSettings = dbSettings;
        this.project = project;
        this.threadPoolSize = threadPoolSize;
        this.executor = newFixedThreadPool(threadPoolSize);
        if (!doImport) {
            results = new HashMap<>();
        }
        start(doImport);

    }

    /**
     * creates the threads that will process the queue
     *
     * @param doImport if true the queue elements will be imported to the db else they will be converted and put into the
     *                 results HashMap
     */
    private void start(boolean doImport) {
        for (int i = 0; i < threadPoolSize; i++) {
            if (doImport) {
                executor.execute(new IssueImporter());
            } else {
                executor.execute(new IssueRetriever());
            }
        }
    }

    /**
     * adds elements to the work queue to signal the worker thread that no more batches will be added and waits for them to stop
     *
     * @throws InterruptedException occurs if the thread is interrupted while stopping the other threads
     */
    public void finish() throws InterruptedException {
        for (int i = 0; i < threadPoolSize; i++) {
            queue.put(new IssueQueueElement(Collections.emptyList(), -1));
        }
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.DAYS);
    }

    /**
     * tests if the executor is shutdown or shall be shutdown as soon as possible
     *
     * @return true if it is shutdown or is going to shutdown as soon as possible; else false
     */
    public boolean isShutdown() {
        return executor.isShutdown() || stop;
    }

    /**
     * adds an batch of issues to the working queue
     *
     * @param issues list of issues that will be executed as a batch
     * @param index  the index of this batch block
     * @throws InterruptedException occurs if the thread is interrupted while waiting for adding it to the queue
     */
    public void put(List<ISSUE> issues, int index) throws InterruptedException {
        this.queue.put(new IssueQueueElement(issues, index));
    }

    /**
     * all worker threads will finish their current batch and then will stop. this method will block till all threads are stopped
     *
     * @throws InterruptedException occurs if the thread is interrupted while stopping the other threads
     */
    public void stopNow() throws InterruptedException {
        stop = true;
        for (int i = 0; i < threadPoolSize; i++) {
            queue.put(new IssueQueueElement(Collections.emptyList(), -1));
        }
        executor.shutdown();
    }

    /**
     * this will return all elements that were processed from the worker threads in their correct order
     *
     * @param elemSize number of expected elements in each value of the results HashMap, if unknown set it to &lt;=0
     * @return the retrieved elements in correct order
     */
    public List<Issue> getResults(int elemSize) {
        List<Issue> result;
        if (elemSize > 0) {
            result = new ArrayList<>(elemSize * results.size());
        } else {
            result = new ArrayList<>();
        }
        for (int j = 0; j < results.size(); j++) {
            result.addAll(results.get(j));
        }
        return result;
    }

    /**
     * @return errors that occured while executing the workers
     */
    public String getErrors() {
        return errors;
    }

    /**
     * this class will take issues from the queue and imports them with their comments to the db
     */
    private class IssueImporter implements Runnable {
        private ITSSqlModule sqlModule;

        @Override
        public void run() {
            try {
                try {//setup sql module
                    if (sqlModule == null) {
                        sqlModule = new ITSSqlModule(dbSettings);
                        sqlModule.prepareStmts(project);
                    }
                    //takes elements from the queue and processes them
                    while (!stop) {
                        IssueQueueElement elem = queue.take();
                        if (elem.index == -1) {
                            //empty list, so no more data to process and we can stop
                            break;
                        }
                        List<ISSUE> issues = elem.getWork();
                        for (ISSUE issue : issues) {
                            sqlModule.importIssue(
                                    issueConverter.getTicketId(issue),
                                    issueConverter.getName(issue),
                                    issueConverter.getTitle(issue),
                                    issueConverter.getDescription(issue),
                                    issueConverter.getAuthor(issue),
                                    issueConverter.getCreationDate(issue),
                                    issueConverter.getState(issue),
                                    issueConverter.getAssignee(issue),
                                    issueConverter.getTargetVersion(issue)
                            );

                            List<COMMENT> comments = commentRetriever.getComments(issue);
                            if (stop || comments == null) {
                                //an error occured so stop this and other running threads
                                stopNow();
                                break;
                            }
                            for (COMMENT comment : comments) {
                                sqlModule.importComment(
                                        commentConverter.getCommentId(comment, issue),
                                        commentConverter.getTicketId(comment, issue),
                                        commentConverter.getDescription(comment, issue),
                                        commentConverter.getAuthor(comment, issue),
                                        commentConverter.getCreationDate(comment, issue)
                                );
                            }
                        }
                    }
                    sqlModule.finishImport();
                } catch (SQLException e) {
                    errors += e.getMessage();
                    stopNow();
                }
            } catch (InterruptedException e) {
                OutputUtil.debug("Thread " + Thread.currentThread().getName() + " was interrupted");
            }
        }
    }

    /**
     * this will take issues from the queue, filters and converts them, retrieve the comments for them
     * and puts them with their index to the results HashMap
     */
    private class IssueRetriever implements Runnable {
        @Override
        public void run() {
            try {
                //takes elements from the queue and processes them
                while (!stop) {
                    IssueQueueElement elem = queue.take();
                    if (elem.index == -1) {
                        //empty list, so no more data to process and we can stop
                        break;
                    }

                    List<ISSUE> issues = elem.getWork();
                    List<Issue> tmpList = new ArrayList<>();
                    for (ISSUE issue : issues) {
                        //if a filter is provisioned test if the issue shall be added to results
                        if (issueFilter != null && !issueFilter.evaluateIssue(issue)) {
                            continue;
                        }

                        Issue vctsiIssue = issueConverter.convertToIssue(issue);
                        List<COMMENT> comments = commentRetriever.getComments(issue);
                        if (stop || comments == null) {
                            //an error occured so stop this and other running threads
                            stopNow();
                            break;
                        }
                        List<IssueComment> issueComments = new ArrayList<>(comments.size());
                        for (COMMENT comment : comments) {
                            issueComments.add(commentConverter.convertToIssueComment(comment));
                        }
                        vctsiIssue.setComments(issueComments);
                        tmpList.add(vctsiIssue);
                    }
                    results.put(elem.getIndex(), tmpList);
                }
            } catch (InterruptedException e) {
                OutputUtil.debug("Thread " + Thread.currentThread().getName() + " was interrupted");
            }
        }
    }

    /**
     * encapsulation of a batch of elements that shall be processed and the index the batch
     */
    private class IssueQueueElement {
        private int index;
        private List<ISSUE> work;

        IssueQueueElement(List<ISSUE> work, int index) {
            this.work = work;
            this.index = index;
        }

        int getIndex() {
            return index;
        }

        List<ISSUE> getWork() {
            return work;
        }
    }
}
