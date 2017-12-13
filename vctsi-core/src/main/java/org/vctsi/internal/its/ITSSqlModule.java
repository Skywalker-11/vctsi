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

import org.vctsi.internal.DBSettings;
import org.vctsi.internal.SqlType;
import org.vctsi.utils.OutputUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ITSSqlModule {

    private Connection connection;
    private PreparedStatement preparedInsertTicketStmt;
    private PreparedStatement preparedInsertCommentsStmt;
    private int addedStmts = 0;
    private final SqlType sqlType;

    /**
     * this will create the sql module for its and the passed db settings and connect to the database
     *
     * @param dbSettings the settings for the database connection
     * @throws SQLException if no connection to the database could be established
     */
    public ITSSqlModule(DBSettings dbSettings) throws SQLException {
        try {
            this.connection = DriverManager.getConnection(
                    "jdbc:" + dbSettings.getServer() + ":" + dbSettings.getPort() + "/" + dbSettings.getDb() + "?useUnicode=true&rewriteBatchedStatements=true",
                    dbSettings.getUsername(), dbSettings.getPassword()
            );
            if (dbSettings.getServer().contains("mysql")) {
                sqlType = SqlType.MYSQL;
            } else if (dbSettings.getServer().contains("postgresql")) {
                sqlType = SqlType.POSTGRESQL;
            } else {
                sqlType = SqlType.OTHER;
            }
            this.connection.setAutoCommit(false);
        } catch (SQLException e) {
            OutputUtil.printError("Could not connect to database");
            throw e;
        }
    }

    /**
     * prepares the sql module to import issues, if necessary tables are created
     *
     * @param project the project for which the sql module shall be prepared
     * @throws SQLException if the database connection failed or an error exist in a sql query
     */
    public void prepareTicketImport(String project) throws SQLException {
        checkAndPrepareTicketTable(project);
        checkAndPrepareCommentTable(project);
        prepareStmts(project);
    }

    /**
     * tests if the ticket table has the correct format and if it does not exist it will be created
     *
     * @param project the project of which the table shall be checked
     * @throws SQLException if the table has the wrong format, the database connection failed or an error exist in a sql query
     */
    private void checkAndPrepareTicketTable(String project) throws SQLException {
        try {
            ResultSet structure = connection.prepareStatement("select * from `its_" + project + "` limit 0").executeQuery();
            ResultSetMetaData metaData = structure.getMetaData();
            if (metaData.getColumnCount() != 9
                    || !metaData.getColumnLabel(1).equals("id") || metaData.getColumnType(1) != 4
                    || !metaData.getColumnLabel(2).equals("name")
                    || !metaData.getColumnLabel(3).equals("title") || metaData.getColumnDisplaySize(3) < 1024
                    || !metaData.getColumnLabel(4).equals("description")
                    || !metaData.getColumnLabel(5).equals("author") || metaData.getColumnDisplaySize(5) < 512
                    || !metaData.getColumnLabel(6).equals("creation_date") || !metaData.getColumnTypeName(6).equals("TIMESTAMP")
                    || !metaData.getColumnLabel(7).equals("state") || metaData.getColumnDisplaySize(7) < 256
                    || !metaData.getColumnLabel(8).equals("assignee") || metaData.getColumnDisplaySize(8) < 512
                    || !metaData.getColumnLabel(9).equals("target_version")) {
                throw new SQLException("its_" + project + " table for project has wrong format");
            } else {
                OutputUtil.debug("its" + project + " table has correct format.");
            }
        } catch (SQLSyntaxErrorException e) {
            createIssueTable(project);
        }
    }

    /**
     * creates the issue table for a project
     *
     * @param project the project for which the table shall be created
     * @throws SQLException if the table cannot be created, the database connection failed or an error exist in a sql query
     */
    private void createIssueTable(String project) throws SQLException {
        if (connection.prepareStatement("CREATE TABLE IF NOT EXISTS `its_" + project + "` ("
                + "`id` INTEGER, "
                + "`name` VARCHAR(512), "
                + "`title` VARCHAR(1024) NOT NULL, "
                + "`description` TEXT, "
                + "`author` VARCHAR(512) , "
                + "`creation_date` TIMESTAMP, "
                + "`state` VARCHAR(256), "
                + "`assignee` VARCHAR(512), "
                + "`target_version` VARCHAR(512), "
                + "PRIMARY KEY (`id`)"
                + ")").executeUpdate() != 0) {
            throw new SQLException("Can't create sql table for project");
        } else {
            OutputUtil.debug("Sql table created");
        }
    }

    /**
     * tests if the comment table has the correct format and if it does not exist it will be created
     *
     * @param project the project of which the table shall be checked
     * @throws SQLException if the table has the wrong format, the database connection failed or an error exist in a sql query
     */
    private void checkAndPrepareCommentTable(String project) throws SQLException {
        try {
            ResultSet structure = connection.prepareStatement("select * from `its_" + project + "_comments` limit 0").executeQuery();
            ResultSetMetaData metaData = structure.getMetaData();
            if (metaData.getColumnCount() != 5
                    || !metaData.getColumnLabel(1).equals("commentId") || metaData.getColumnType(1) != 4
                    || !metaData.getColumnLabel(2).equals("ticketId") || metaData.getColumnType(2) != 4
                    || !metaData.getColumnLabel(3).equals("description")
                    || !metaData.getColumnLabel(4).equals("author") || metaData.getColumnDisplaySize(4) < 512
                    || !metaData.getColumnLabel(5).equals("creation_date") || metaData.getColumnType(5) != 93) {
                throw new SQLException("Comment table for project has wrong format");
            } else {
                OutputUtil.debug("its_" + project + "_comment table has correct format.");
            }
        } catch (SQLSyntaxErrorException e) {
            createCommentTable(project);
        }
    }

    /**
     * creates the comment table for a project
     *
     * @param project the project for which the table shall be created
     * @throws SQLException if the table cannot be created, the database connection failed or an error exist in a sql query
     */
    private void createCommentTable(String project) throws SQLException {
        if (connection.prepareStatement("CREATE TABLE IF NOT EXISTS `its_" + project + "_comments` ("
                + "`commentId` INTEGER,"
                + "`ticketId` INTEGER,"
                + "`description` TEXT,"
                + "`author` VARCHAR(512),"
                + "`creation_date` TIMESTAMP,"
                + "PRIMARY KEY (`commentId`, `ticketId`) "
                + ")").executeUpdate() != 0) {
            throw new SQLException("Can't create sql table for project");
        } else {
            OutputUtil.debug("its_" + project + "_comments table created");
        }
    }

    /**
     * this will delete all tables of a project and recreates them
     *
     * @param project the project of which the tables shall be recreated
     * @throws SQLException if the database connection failed or an error exist in a sql query
     */
    public void clearTables(String project) throws SQLException {
        connection.prepareStatement("DROP TABLE IF EXISTS `its_" + project + "`").executeUpdate();
        connection.prepareStatement("DROP TABLE IF EXISTS `its_" + project + "_comments`").executeUpdate();
        createCommentTable(project);
        createIssueTable(project);
    }

    /**
     * creates the prepared statements that will be used to import data
     *
     * @param project the project for which the data will be imported
     * @throws SQLException if the database connection failed or an error exist in a sql query
     */
    public void prepareStmts(String project) throws SQLException {
        String insertTicketStmt = "INSERT " + getIgnore() + " INTO `its_" + project + "` "
                + "(`id`, `name`, `title`, `description`, `author`, `creation_date`, `state`, `assignee`, `target_version`)"
                + " VALUES (?, ?, ?, ?, ? , ?, ?, ?, ?)" + getOnConflict("`id`");
        preparedInsertTicketStmt = connection.prepareStatement(insertTicketStmt);
        String insertCommentsStmt = "INSERT " + getIgnore() + " INTO `its_" + project + "_comments` "
                + "(`commentId`, `ticketId`, `description`, `author`, `creation_date`) "
                + "VALUES (?, ?, ?, ?, ?)" + getOnConflict("`commentId`", "`ticketId`");
        preparedInsertCommentsStmt = connection.prepareStatement(insertCommentsStmt);
    }


    /**
     * adds a statement to the batch queue to import an issue with the given arguments
     *
     * @param ticketId      id of the ticket
     * @param name          name of the ticket
     * @param title         title of the ticket
     * @param description   description of the ticket
     * @param author        author of the ticket
     * @param creationDate  date of creation of the ticket
     * @param state         state of the ticket
     * @param assignee      assignee of the ticket
     * @param targetVersion targetversion / milestone of the ticket
     * @throws SQLException if the database connection failed or an error exist in a sql query
     */
    public void importIssue(int ticketId, String name, String title, String description, String author, Date creationDate, String state, String assignee, String targetVersion) throws SQLException {
        preparedInsertTicketStmt.setTimestamp(6, Timestamp.from(creationDate.toInstant()));
        importIssue(ticketId, name, title, description, author, state, assignee, targetVersion);
    }

    /**
     * adds a statement to the batch queue to import an issue with the given arguments
     *
     * @param ticketId      id of the ticket
     * @param name          name of the ticket
     * @param title         title of the ticket
     * @param description   description of the ticket
     * @param author        author of the ticket
     * @param creationDate  date of creation of the ticket
     * @param state         state of the ticket
     * @param assignee      assignee of the ticket
     * @param targetVersion targetversion / milestone of the ticket
     * @throws SQLException if the database connection failed or an error exist in a sql query
     */
    public void importIssue(int ticketId, String name, String title, String description, String author, LocalDateTime creationDate, String state, String assignee, String targetVersion) throws SQLException {
        preparedInsertTicketStmt.setTimestamp(6, (creationDate == null ? null : Timestamp.from(creationDate.toInstant(ZoneOffset.ofHours(2)))));
        importIssue(ticketId, name, title, description, author, state, assignee, targetVersion);

    }

    /**
     * adds a statement to the batch queue to import an issue with the given arguments
     * and executes the batch of sql queries for the issues and comments each 500 statements that were added
     *
     * @param ticketId      id of the ticket
     * @param name          name of the ticket
     * @param title         title of the ticket
     * @param description   description of the ticket
     * @param author        author of the ticket
     * @param state         state of the ticket
     * @param assignee      assignee of the ticket
     * @param targetVersion targetversion / milestone of the ticket
     * @throws SQLException if the database connection failed or an error exist in a sql query
     */
    private synchronized void importIssue(int ticketId, String name, String title, String description, String author, String state, String assignee, String targetVersion) throws SQLException {
        preparedInsertTicketStmt.setInt(1, ticketId);
        preparedInsertTicketStmt.setString(2, name);
        preparedInsertTicketStmt.setString(3, title);
        preparedInsertTicketStmt.setString(4, description);
        preparedInsertTicketStmt.setString(5, author);
        preparedInsertTicketStmt.setString(7, state);
        preparedInsertTicketStmt.setString(8, assignee);
        preparedInsertTicketStmt.setString(9, targetVersion);
        preparedInsertTicketStmt.addBatch();
        addedStmts++;
        executeBatches();
    }

    /**
     * adds a statement to the batch queue to import a comment with the given arguments
     * and executes the batch of sql queries for the issues and comments each 500 statements that were added
     *
     * @param commentId    id of the comment
     * @param ticketId     id of the ticket the comment belongs to
     * @param description  description of the comment
     * @param author       author of the comment
     * @param creationDate date the comment was created
     * @throws SQLException if the database connection failed or an error exist in a sql query
     */
    public void importComment(int commentId, int ticketId, String description, String author, Date creationDate) throws SQLException {
        preparedInsertCommentsStmt.setInt(1, commentId);
        preparedInsertCommentsStmt.setInt(2, ticketId);
        importComment(description, author, creationDate);
    }

    /**
     * adds a statement to the batch queue to import a comment with the given arguments
     * and executes the batch of sql queries for the issues and comments each 500 statements that were added
     *
     * @param commentId    id of the comment
     * @param ticketId     id of the ticket the comment belongs to
     * @param description  description of the comment
     * @param author       author of the comment
     * @param creationDate date the comment was created
     * @throws SQLException if the database connection failed or an error exist in a sql query
     */
    public void importComment(long commentId, long ticketId, String description, String author, Date creationDate) throws SQLException {
        preparedInsertCommentsStmt.setLong(1, commentId);
        preparedInsertCommentsStmt.setLong(2, ticketId);
        importComment(description, author, creationDate);
    }

    /**
     * adds a statement to the batch queue to import a comment with the given arguments
     * and executes the batch of sql queries for the issues and comments each 500 statements that were added
     *
     * @param description  description of the comment
     * @param author       author of the comment
     * @param creationDate date the comment was created
     * @throws SQLException if the database connection failed or an error exist in a sql query
     */
    private void importComment(String description, String author, Date creationDate) throws SQLException {
        preparedInsertCommentsStmt.setString(3, description);
        preparedInsertCommentsStmt.setString(4, author);
        preparedInsertCommentsStmt.setTimestamp(5, (creationDate == null ? null : Timestamp.from(creationDate.toInstant())));
        preparedInsertCommentsStmt.addBatch();
        addedStmts++;
        executeBatches();
    }

    /**
     * executes the batch of sql queries for issues and commits each 500 statements that were added
     *
     * @throws SQLException if the database connection failed or an error exist in a sql query
     */
    private void executeBatches() throws SQLException {
        if (addedStmts % 300 == 0) {
            preparedInsertTicketStmt.executeBatch();
            preparedInsertCommentsStmt.executeBatch();
            OutputUtil.debug(".");
        }
    }

    /**
     * writes all pending changes to the database and closes the database connection after it
     *
     * @throws SQLException if the database connection failed or an error exist in a sql query
     */
    public void finishImport() throws SQLException {
        preparedInsertTicketStmt.executeBatch();
        preparedInsertCommentsStmt.executeBatch();
        close();
    }

    /**
     * closes the connection to the database
     *
     * @throws SQLException if the database connection failed or an error exist in a sql query
     */
    public void close() throws SQLException {
        connection.commit();
        connection.close();
    }

    /**
     * searches issues from a project that matches the search parameters
     *
     * @param project the project from which the issues shall be retrieved
     * @param params  the parameters of the search
     * @return a list of issues matching the searchparameters or null if an error occurred
     */
    public List<Issue> getIssues(ITSSearchParameters params, String project) {
        try {
            String query = getSearchQuery(params, project);
            PreparedStatement searchTicketStmt = connection.prepareStatement(query);
            int i = 1;
            if (params.getAuthor() != null) {
                searchTicketStmt.setString(i++, params.getAuthor());
            }
            if (params.getTitle() != null) {
                searchTicketStmt.setString(i++, "%" + params.getTitle() + "%");
            }
            if (params.getDescription() != null) {
                searchTicketStmt.setString(i++, "%" + params.getDescription() + "%");
            }
            if (params.getCommit() != null) {
                searchTicketStmt.setString(i++, "[^[:alnum:]]" + escapeRegExp(params.getCommitPrefix() + params.getCommit() + params.getCommitSuffix()) + "[^[:alnum:]]");
                searchTicketStmt.setString(i++, "[^[:alnum:]]" + escapeRegExp(params.getCommitPrefix() + params.getCommit() + params.getCommitSuffix()) + "[^[:alnum:]]");
            }
            if (params.getAssignee() != null) {
                searchTicketStmt.setString(i++, params.getAssignee());
            }
            if (params.getIds() != null) {
                searchTicketStmt.setArray(i++, connection.createArrayOf("integer", params.getIds()));
            }
            if (params.getNames() != null) {
                searchTicketStmt.setArray(i++, connection.createArrayOf("text", params.getNames()));
            }
            if (params.getState() != null) {
                searchTicketStmt.setString(i++, params.getState());
            }
            if (params.getStartDate() != null) {
                searchTicketStmt.setTimestamp(i++, Timestamp.from(params.getStartDate().toInstant(ZoneOffset.ofHours(2))));
                searchTicketStmt.setTimestamp(i++, Timestamp.from(params.getEndDate().toInstant(ZoneOffset.ofHours(2))));
            }

            List<Issue> issues = getIssuesFromResult(searchTicketStmt.executeQuery());
            PreparedStatement getCommentsStmt = connection.prepareStatement(
                    "SELECT * FROM `its_" + project + "_comments` WHERE `ticketId` LIKE ?"
            );
            for (Issue issue : issues) {
                getCommentsStmt.setInt(1, issue.getId());
                List<IssueComment> comments = getCommentsFromResult(getCommentsStmt.executeQuery());
                issue.setComments(comments);
            }
            connection.close();
            return issues;
        } catch (SQLException e) {
            OutputUtil.printError("An error occured: " + e.getMessage());
            return null;
        }
    }

    /**
     * creates the sql query for the passed search parameters that will contain placeholders for the actual search values
     *
     * @param project the project from which the issues shall be retrieved
     * @param params  the parameters of the search
     * @return an sql query representing the search parameters and containing placeholders for the actual search values
     */
    private String getSearchQuery(ITSSearchParameters params, String project) {
        String query = "SELECT * FROM `its_" + project + "` WHERE 1 ";
        if (params.getAuthor() != null) {
            query += " AND `author` LIKE ? ";
        }
        if (params.getTitle() != null) {
            query += " AND `title` LIKE ? ";
        }
        if (params.getDescription() != null) {
            query += " AND `description` LIKE ? ";
        }
        if (params.getCommit() != null) {
            query += " AND (`title` " + getRegExpIdentifier() + " ? OR `description` " + getRegExpIdentifier() + " ? )";
        }
        if (params.getAssignee() != null) {
            query += " AND `assignee` LIKE ? ";
        }
        if (params.getIds() != null) {
            query += " AND `id` IN ( ? )";
        }
        if (params.getNames() != null) {
            query += " AND `name` IN ( ? )";
        }
        if (params.getState() != null) {
            query += " AND `state` LIKE ?";
        }
        if (params.getStartDate() != null) {
            query += " AND (`creation_date` BETWEEN ? AND ? )";
        }
        return query;
    }

    /**
     * gets the issues from the resultset and returns them as a list
     *
     * @param resultSet a sql resultset from the issue table that contains a list of issues
     * @return a list of Issue from the resultSet
     * @throws SQLException if the resultset does not contain issue data, the resultset is closed or the database connection failed
     */
    private List<Issue> getIssuesFromResult(ResultSet resultSet) throws SQLException {
        List<Issue> issues = new ArrayList<>();
        while (resultSet.next()) {
            issues.add(new Issue(
                    resultSet.getInt("id"),
                    resultSet.getString("name"),
                    resultSet.getString("title"),
                    resultSet.getString("description"),
                    resultSet.getString("author"),
                    resultSet.getTimestamp("creation_date").toLocalDateTime(),
                    resultSet.getString("state"),
                    resultSet.getString("assignee"),
                    resultSet.getString("target_version")
            ));
        }
        return issues;
    }

    /**
     * gets the comments from the resultset and returns them as a list
     *
     * @param resultSet a sql resultset from the comment table that contains a list of comments
     * @return a list of IssueComment from the resultSet
     * @throws SQLException if the resultset does not contain comment data, the resultset is closed or the database connection failed
     */

    private List<IssueComment> getCommentsFromResult(ResultSet resultSet) throws SQLException {
        List<IssueComment> comments = new ArrayList<>();
        while (resultSet.next()) {
            comments.add(new IssueComment(
                    resultSet.getInt("commentId"),
                    resultSet.getString("description"),
                    resultSet.getString("author"),
                    resultSet.getTimestamp("creation_date").toLocalDateTime()
            ));
        }
        return comments;
    }

    /**
     * this will return a query part for mysql so that existing entries will be ignored on insert
     *
     * @return string containing the keyword for mysql to ignore existing entries
     */
    private String getIgnore() {
        if (sqlType == SqlType.MYSQL) {
            return " IGNORE ";
        }
        return "";
    }

    /**
     * this will return a query part for postgresql so that existing entries will be ignored on insert
     *
     * @param keys the columns that define the primary key columns
     * @return string containing the keywords for postgresql to ignore existing entries
     */
    private String getOnConflict(String... keys) {
        if (sqlType == SqlType.POSTGRESQL) {
            return " ON CONFLICT (" + String.join(", ", (CharSequence[]) keys) + " ) DO NOTHING";
        }
        return "";
    }

    /**
     * gets the identifier for regExp in a query for the current used sql type
     *
     * @return the identifier
     */
    private String getRegExpIdentifier() {
        if (sqlType == SqlType.POSTGRESQL) {
            return " ~ ";
        } else if (sqlType == SqlType.MYSQL) {
            return " REGEXP ";
        } else {
            return " LIKE ";
        }
    }


    /**
     * escapes a string for use with regexp
     *
     * @param input the input that shall be escaped
     * @return the escaped input
     */
    private String escapeRegExp(String input) {
        return input.replace("*", "[*]").replace(".", "[.]").replace("$", "[$]").replace("+", "[+]"
                .replace("^", "[^]").replace("?", "[?]").replace("|", "[|]"));
    }
}
