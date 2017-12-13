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

import org.junit.After;
import org.junit.Before;
import org.vctsi.utils.OutputUtil;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class VctsiTest {
    protected final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    protected final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private PrintStream out;
    private PrintStream err;
    public static boolean printOutput = false; //for debug

    @Before
    public void setUpStreams() {
        out = System.out;
        err = System.err;
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        OutputUtil.DEBUG = false;
    }

    @After
    public void cleanUpStreams() {
        System.setOut(out);
        System.setErr(err);
        if(printOutput) {
            System.out.print(outContent.toString());
            System.err.print(errContent.toString());
            printOutput = false;
        }
    }
}
