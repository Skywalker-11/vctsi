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
 * This stores a change of one file between two revisions
 */
public class FileChange {
    private String newCommit;
    private String oldCommit;
    private String newName;
    private String oldName;
    private int num;

    public FileChange(String oldCommit, String newName, String oldName) {
        this.oldCommit = oldCommit;
        this.newName = newName;
        this.oldName = oldName;
    }
    public FileChange(String newCommit, String oldCommit, String newName, String oldName, int num) {
        this.newCommit = newCommit;
        this.oldCommit = oldCommit;
        this.newName = newName;
        this.oldName = oldName;
        this.num = num;
    }

    public String getOldCommit() {
        return oldCommit;
    }

    public String getNewName() {
        return newName;
    }

    public String getOldName() {
        return oldName;
    }

    public String getNewCommit() {
        return newCommit;
    }

    public int getNum() {
        return num;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FileChange) {
            FileChange other = (FileChange) obj;
            return (oldCommit == null ? other.getOldCommit() == null : oldCommit.equals(other.getOldCommit())) &&
                    (newName == null ? other.getNewName() == null : newName.equals(other.getNewName())) &&
                    (oldName == null ? other.getOldName() == null : oldName.equals(other.getOldName()));
        } else {
            return false;
        }
    }
}
