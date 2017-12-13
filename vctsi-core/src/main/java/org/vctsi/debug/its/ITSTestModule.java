package org.vctsi.debug.its;

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

import org.vctsi.internal.its.ITSModule;
import org.vctsi.internal.its.Issue;
import org.vctsi.internal.tasks.SearchIssueTask;
import org.vctsi.utils.OutputUtil;

import java.util.Collections;
import java.util.List;

public class ITSTestModule extends ITSModule {

    public static final String onlineSearchmessage = "You called the DEBUG module for its! This would execute the online search for: ";
    public static final String importMessage = "You called the DEBUG module for its! This would import Issues.";
    public static final String onlineIssue = "You called the DEBUG module for its! This would get the issue for id ";

    @Override
    protected boolean importIssues() {
        OutputUtil.printInfo(importMessage);
        return true;
    }

    @Override
    protected List<Issue> onlineSearch(SearchIssueTask task) {
        OutputUtil.printInfo(onlineSearchmessage + task.toString());
        return Collections.emptyList();
    }

    @Override
    protected List<Issue> getOnlineIssue(int id) {
        OutputUtil.printInfo(onlineIssue + id);
        return Collections.emptyList();
    }
}
