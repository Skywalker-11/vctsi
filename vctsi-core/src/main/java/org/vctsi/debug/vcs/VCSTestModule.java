package org.vctsi.debug.vcs;

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

import org.vctsi.internal.tasks.ImportCommitsTask;
import org.vctsi.internal.vcs.Diff;
import org.vctsi.internal.vcs.VCSDiffSettings;
import org.vctsi.internal.vcs.VCSModule;
import org.vctsi.utils.OutputUtil;

public class VCSTestModule extends VCSModule {

    public static final String message = "You called the debug module for vcs!!! The vcs module would import commits ";

    @Override
    protected boolean importCommits(boolean noUpdate) {
        OutputUtil.printInfo(message);
        return true;
    }

    @Override
    protected boolean importNewCommits() {
        OutputUtil.printInfo(message);
        return true;
    }

    @Override
    protected Diff getFileDiffs(VCSDiffSettings diffSettings) {
        OutputUtil.printInfo("This would print the file diffs for " + diffSettings.toString());
        return null;
    }
}
