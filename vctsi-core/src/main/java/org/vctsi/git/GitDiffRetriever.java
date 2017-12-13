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

import org.eclipse.jgit.revwalk.RevCommit;
import org.vctsi.internal.vcs.VCSRunnable;

import java.util.Collection;

public class GitDiffRetriever extends VCSRunnable<GitModule, RevCommit> {

    public GitDiffRetriever() {
    }

    @Override
    public void execute(Collection<RevCommit> commits) {
        for (RevCommit commit : commits) {
            try {
                module.importDiffs(commit);
            } catch (Exception e) {
                module.failedDiffImports.add(commit.getName());
            }
        }
    }
}
