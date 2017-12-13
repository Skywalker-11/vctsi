package org.vctsi.internal.vcs;

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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.newFixedThreadPool;

public class VCSThreadSpawner<MODULE extends VCSModule, ELEM> {

    private LinkedBlockingQueue<List<ELEM>> queue;
    private MODULE param;
    private final int threadPoolSize;
    private Class<? extends VCSRunnable<MODULE, ELEM>> vcsRunnable;
    private ExecutorService executor;

    public <R extends VCSRunnable<MODULE, ELEM>> VCSThreadSpawner(MODULE module, Class<R> vcsRunnable, int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
        this.param = module;
        queue = new LinkedBlockingQueue<List<ELEM>>();
        this.vcsRunnable = vcsRunnable;
    }

    /**
     * starts the threads of the pool by creating a new instance of the class of vcsRunnable and executing it
     *
     * @throws ReflectiveOperationException is thrown if no matching constructor could be found for the given runnable
     */
    public void start() throws ReflectiveOperationException {
        executor = newFixedThreadPool(threadPoolSize);
        for (int i = 0; i < threadPoolSize; i++) {
            VCSRunnable<MODULE, ELEM> r = vcsRunnable.newInstance();
            r.setModule(this.param);
            r.setSpawner(this);
            executor.execute(r);
        }
    }

    /**
     * takes an element from the queue
     *
     * @return elment from the queue
     * @throws InterruptedException can be thrown if thread is interrupted while waiting to get the lock on the queue
     */
    Collection<ELEM> takeElem() throws InterruptedException {
        return queue.take();
    }

    /**
     * adds an element to the queue
     *
     * @param elem list of element to put into the queue
     * @throws InterruptedException can be thrown if thread is interrupted while waiting to get the lock on the queue
     */
    public void putElem(List<ELEM> elem) throws InterruptedException {
        queue.put(elem);
    }

    /**
     * adds empty lists to queue to signal the worker threads that no more tasks will come and awaits their shutdown
     *
     * @throws InterruptedException can be thrown if thread is interrupted while waiting for the worker thread to stop
     */
    public void finish() throws InterruptedException {
        for (int i = 0; i < threadPoolSize; i++) {
            queue.put(Collections.emptyList());
        }
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.DAYS);
    }
}
