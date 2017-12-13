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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;

import static org.junit.Assert.*;

public class VCSSearchParametersTest {

    @Test
    public void testEmptyIds() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String testInput = "{"
                + "\"ids\":[]"
                + "}";
        VCSSearchParameters searchParameters = mapper.readValue(testInput, VCSSearchParameters.class);
        assertNull(searchParameters.getAuthor(), null);
        assertNotNull(searchParameters.getIds());
        assertEquals(searchParameters.getIds().length, 0);
        assertNull(searchParameters.getStartDate());
        assertNull(searchParameters.getEndDate());
        assertNull(searchParameters.getStartCommit());
        assertNull(searchParameters.getEndCommit());
        assertNull(searchParameters.getMessage());
        assertNull(searchParameters.getTicket());
    }

}
