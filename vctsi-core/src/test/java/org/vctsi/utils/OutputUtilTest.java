package org.vctsi.utils;

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

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vctsi.internal.its.Issue;
import org.vctsi.internal.its.IssueComment;
import org.vctsi.internal.its.IssueState;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OutputUtilTest {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private PrintStream out;
    private PrintStream err;

    @Before
    public void setUpStreams() {
        OutputUtil.DEBUG = true;
        out = System.out;
        err = System.err;
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @After
    public void cleanUpStreams() {
        System.setOut(out);
        System.setErr(err);

    }

    @Test
    public void testPrintIssueList() throws IOException {
        List<Issue> issues = createTestIssues();
        testIssueOutput(issues);
    }

    @Test
    public void testPrintIssueWithCommentsList() throws Exception {
        List<Issue> issues = createTestIssues();
        int j = 0;
        for (int i = 0; i < 3; i++) {
            List<IssueComment> comments = new ArrayList<>();
            for (int k = 0; k < 5; k++) {
                comments.add(new IssueComment(j++, "test author" + k, "test description" + k, LocalDateTime.of(2000 + j, 2, 5, 3, 6, 59)));
            }
            issues.get(i).setComments(comments);
        }
        testIssueOutput(issues);
    }

    private void testIssueOutput(List<Issue> issues) throws IOException {
        OutputUtil.printObjectList(issues);
        assertTrue(errContent.size() == 0);
        String output = outContent.toString();

        ObjectMapper mapper = new ObjectMapper();
        SimpleModule serializerModule = new SimpleModule("MyModule", new Version(2, 7, 0, null, "com.fasterxml.jackson.core", "jackson-databind"));
        serializerModule.addSerializer(LocalDateTime.class, new LocalDateTimeToJsonSerializer());
        serializerModule.addDeserializer(LocalDateTime.class, new JsonStringToLocalDateTimeDeserializer());
        mapper.registerModule(serializerModule);

        List<Issue> parsed = mapper.readValue(output, new TypeReference<List<Issue>>() {
        });
        assertTrue(parsed.size() == issues.size());
        for (int i = 0; i < parsed.size(); i++) {
            assertEquals(parsed.get(i), issues.get(i));
        }
    }

    private List<Issue> createTestIssues() {
        List<Issue> issues = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            issues.add(new Issue(
                    i,
                    "testname" + i,
                    "testtitle" + i,
                    "testdescription" + i,
                    "testauthor" + i,
                    LocalDateTime.of(2000 + i, 4, 30, 23, 40, 58),
                    IssueState.NEW.toString(),
                    "assignee" + i,
                    "targetversion" + i
            ));
        }
        issues.add(new Issue(6, "testname0", null, "testdescription0", "testauthor0", LocalDateTime.of(2016, 4, 30, 23, 40, 58), IssueState.ASSIGNED.toString(), "assignee0", "targetversion0"));
        issues.add(new Issue(6, "testname0", "testtitle0", null, "testauthor0", LocalDateTime.of(2016, 4, 30, 23, 40, 58), IssueState.REOPENED.toString(), "assignee0", "targetversion0"));
        issues.add(new Issue(6, "testname0", "testtitle0", "testdescription0", null, LocalDateTime.of(2016, 4, 30, 23, 40, 58), IssueState.SOLVED.toString(), "assignee0", "targetversion0"));
        issues.add(new Issue(6, "testname0", "testtitle0", "testdescription0", "testauthor0", LocalDateTime.of(2016, 4, 30, 23, 40, 58), IssueState.UNCONFIRMED.toString(), null, "targetversion0"));
        issues.add(new Issue(6, "testname0", "testtitle0", "testdescription0", "testauthor0", LocalDateTime.of(2016, 4, 30, 23, 40, 58), IssueState.VERIFIED.toString(), "assignee0", null));
        return issues;
    }
}
