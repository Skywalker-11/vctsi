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

/**
 * Stores a diff between two revisions
 */
public class Diff {
    private String revision1;
    private String revision2;
    private String diff;

    //needed for json serialization
    @SuppressWarnings("unused")
    public Diff() {
    }

    public Diff(String revision1, String revision2, String diff) {
        this.revision1 = revision1;
        this.revision2 = revision2;
        this.diff = diff;
    }

    public String getRevision1() {
        return revision1;
    }

    public String getRevision2() {
        return revision2;
    }

    public String getDiff() {
        return diff;
    }
}
