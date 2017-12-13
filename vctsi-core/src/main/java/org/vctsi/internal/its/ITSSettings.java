package org.vctsi.internal.its;

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

public class ITSSettings {

    private String username;
    private String password;
    private String path;
    private String project;
    private boolean online;

    public ITSSettings() {
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

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ITSSettings) {
            ITSSettings other = (ITSSettings) obj;
            return (username == null ? other.getUsername() == null : username.equals(other.getUsername())) &&
                    (password == null ? other.getPassword() == null : password.equals(other.getPassword())) &&
                    (project == null ? other.getProject() == null : project.equals(other.getProject())) &&
                    (path == null ? other.getPath() == null : path.equals(other.getPath())) &&
                    (online == other.isOnline());
        } else {
            return false;
        }
    }
}
