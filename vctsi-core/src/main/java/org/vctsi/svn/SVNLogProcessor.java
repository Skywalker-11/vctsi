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

import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.vctsi.internal.vcs.VCSRunnable;

import java.util.Collection;

public class SVNLogProcessor extends VCSRunnable<ASVNModule, SVNLogEntry> {

    public SVNLogProcessor() {
    }

    /**
     * Processes the logEntries by executing the appropriate method in the SVNModule. If errors occurs the failed entries
     * are stored in SVNModule.failedImports
     *
     * @param logEntries the log entries to process
     */
    @Override
    public void execute(Collection<SVNLogEntry> logEntries) {
        for (SVNLogEntry logEntry : logEntries) {
            module.processLogEntry(logEntry, null);
        }
    }
}
