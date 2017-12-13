package org.vctsi;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;
import org.vctsi.utils.OutputUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MainTest extends VctsiTest {

    @Test
    public void testSearchCommitNoVCSModule() throws JsonProcessingException {
        Main.main(new String[]{
                "-vcsLocalPath=localhost",
                "-searchCommitMessage=abc"
        });
        assertEquals(OutputUtil.getErrorMessageAsJsonString(Main.ERROR_MISSING_VCS_MODULE) + System.lineSeparator(), errContent.toString());
        assertTrue(outContent.size() == 0);
    }

    @Test
    public void testSearchIssueNoITSModule() throws JsonProcessingException {
        Main.main(new String[]{
                "-itsPath=localhost",
                "-searchIssueTitle=abc"
        });
        assertEquals(OutputUtil.getErrorMessageAsJsonString(Main.ERROR_MISSING_ITS_MODULE) + System.lineSeparator(), errContent.toString());
        assertTrue(outContent.size() == 0);
    }
}

