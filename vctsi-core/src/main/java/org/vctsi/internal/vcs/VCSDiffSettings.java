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

public class VCSDiffSettings {
    private String commit1;
    private String commit2;
    private String path;

    public VCSDiffSettings() {

    }

    public String getCommit1() {
        return commit1;
    }

    public void setCommit1(String commit1) {
        this.commit1 = commit1;
    }

    public String getCommit2() {
        return commit2;
    }

    public void setCommit2(String commit2) {
        this.commit2 = commit2;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return "DiffSettings for " + commit1 + " <-> " + commit2 + " and path " + path;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VCSDiffSettings) {
            VCSDiffSettings other = (VCSDiffSettings) obj;
            return (commit1 == null ? other.getCommit1() == null : commit1.equals(other.commit1)) &&
                    (commit2 == null ? other.getCommit2() == null : commit2.equals(other.getCommit2())) &&
                    (path == null ? other.getPath() == null : path.equals(other.getPath()));
        } else {
            return false;
        }
    }
}
