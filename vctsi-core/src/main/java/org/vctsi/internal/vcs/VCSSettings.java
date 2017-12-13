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

public class VCSSettings {

    private String branchRootFolder;
    private String username;
    private String password;
    private String sshKey;
    private String localPath;
    private String project;
    private String remotePath;
    //if true only new revisions will be imported
    private boolean onlyNew = false;
    private boolean noUpdate = false;

    public VCSSettings() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSshKey() {
        return sshKey;
    }

    public void setSshKey(String sshKey) {
        this.sshKey = sshKey;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String path) {
        this.localPath = path;
    }

    public String getRemotePath() {
        return remotePath;
    }

    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getBranchRootFolder() {
        return branchRootFolder;
    }

    public void setBranchRootFolder(String branchRootFolder) {
        this.branchRootFolder = branchRootFolder;
    }

    public boolean shouldOnlyUpdateNew() {
        return onlyNew;
    }

    public void setOnlyNew(boolean onlyNew) {
        this.onlyNew = onlyNew;
    }

    public boolean isNoUpdate() {
        return noUpdate;
    }

    public void setNoUpdate(boolean noUpdate) {
        this.noUpdate = noUpdate;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VCSSettings) {
            VCSSettings other = (VCSSettings) obj;
            return (username == null ? other.getUsername() == null : username.equals(other.getUsername()))
                    && (password == null ? other.getPassword() == null : password.equals(other.getPassword()))
                    && (localPath == null ? other.getLocalPath() == null : localPath.equals(other.getLocalPath()))
                    && (remotePath == null ? other.getRemotePath() == null : remotePath.equals(other.getRemotePath()))
                    && (project == null ? other.getProject() == null : project.equals(other.getProject()))
                    && (onlyNew == other.shouldOnlyUpdateNew())
                    && (noUpdate == other.isNoUpdate())
                    && (branchRootFolder == null ? other.getBranchRootFolder() == null : branchRootFolder.equals(other.getBranchRootFolder()))
                    && (sshKey == null ? other.getSshKey() == null: sshKey.equals(other.getSshKey()));
        } else {
            return false;
        }
    }
}
