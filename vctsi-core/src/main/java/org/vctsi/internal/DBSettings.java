package org.vctsi.internal;

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

public class DBSettings {

    private String server = "mysql://localhost";
    private int port = 3306;
    private String db = "vctsi";
    private String username;
    private String password;

    public DBSettings() {
    }

    public DBSettings(String server, int port, String db, String username, String password) {
        this.server = server;
        this.port = port;
        this.db = db;
        this.username = username;
        this.password = password;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDb() {
        return db;
    }

    public void setDb(String db) {
        this.db = db;
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

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DBSettings) {
            DBSettings other = (DBSettings) obj;
            return (server == null ? other.getServer() == null : server.equals(other.getServer())) &&
                    (port == other.getPort()) &&
                    (db == null ? other.getDb() == null : db.equals(other.getDb())) &&
                    (username == null ? other.getUsername() == null : username.equals(other.getUsername())) &&
                    (password == null ? other.getPassword() == null : password.equals(other.getPassword()));
        } else {
            return false;
        }
    }
}
