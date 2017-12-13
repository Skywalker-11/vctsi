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

import org.vctsi.internal.DBSettings;
import org.vctsi.internal.SqlType;
import org.vctsi.utils.OutputUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class VCSSqlModule {

    private Connection connection;
    private PreparedStatement preparedInsertCommitStmt;
    private PreparedStatement preparedInsertCommitBranchStmt;
    private PreparedStatement preparedInsertDiffStmt;
    private int addedStmts = 0;
    private int addedDiffStmts = 0;
    private SqlType sqlType;


    /**
     * this will create the sql module for vcs and the passed db settings and connect to the database
     *
     * @param dbSettings the settings for the database connection
     * @throws SQLException if no connection to the database could be established
     */
    public VCSSqlModule(DBSettings dbSettings) throws SQLException {
        this.connection = DriverManager.getConnection(
                "jdbc:" + dbSettings.getServer() + ":" + dbSettings.getPort() + "/" + dbSettings.getDb() + "?useUnicode=true&characterEncoding=utf8mb4&rewriteBatchedStatements=true",
                dbSettings.getUsername(), dbSettings.getPassword()
        );
        if (dbSettings.getServer().contains("mysql")) {
            sqlType = SqlType.MYSQL;
        } else if (dbSettings.getServer().contains("postgresql")) {
            sqlType = SqlType.POSTGRESQL;
        }
        this.connection.setAutoCommit(false);
    }

    /**
     * prepares the sql module to import commits, if necessary tables are created
     *
     * @param project the project for which the sql module shall be prepared
     * @throws SQLException if the database connection failed or an error exist in a sql query
     */
    public void prepareCommitImport(String project) throws SQLException {
        checkAndPrepareCommitTable(project);
        checkAndPrepareCommitBranchTable(project);
        checkAndPrepareDiffTable(project);
        prepareStmts(project);
    }

    /**
     * creates the prepared statements that will be used to import data
     *
     * @param project the project for which the data will be imported
     * @throws SQLException if the database connection failed or an error exist in a sql query
     */
    public void prepareStmts(String project) throws SQLException {
        String insertDiffStmt = "INSERT " + getIgnore() + " INTO `vcs_" + project + "_diff` "
                + "(`new_commit`, `old_commit`, `diff_entry`, `new_name`, `old_name`) "
                + "VALUES (?, ?, ?, ?, ?)" + getOnConflict("`new_commit`", "`old_commit`", "`diff_entry`");
        preparedInsertDiffStmt = connection.prepareStatement(insertDiffStmt);
        String insertCommitStmt = "INSERT " + getIgnore() + " INTO `vcs_" + project + "` "
                + "(`id`, `message`, `author`, `creation_date`) "
                + "VALUES (?, ?, ? , ?)" + getOnConflict("`id`");
        preparedInsertCommitStmt = connection.prepareStatement(insertCommitStmt);
        String insertCommitBranchStmt = "INSERT " + getIgnore() + " INTO `vcs_" + project + "_branches` "
                + "(`commit`, `branch`) "
                + "VALUES (?, ?)" + getOnConflict("`commit`", "`branch`");
        preparedInsertCommitBranchStmt = connection.prepareStatement(insertCommitBranchStmt);

    }

    /**
     * tests if the commit table has the correct format and if it does not exist it will be created
     *
     * @param project the project of which the table shall be checked
     * @throws SQLException if the table has the wrong format, the database connection failed or an error exist in a sql query
     */
    private void checkAndPrepareCommitTable(String project) throws SQLException {
        try {
            ResultSet structure = connection.prepareStatement("select * from `vcs_" + project + "` limit 0").executeQuery();
            ResultSetMetaData metaData = structure.getMetaData();
            if (metaData.getColumnCount() != 4
                    || !metaData.getColumnLabel(1).equals("id") || metaData.getColumnDisplaySize(1) < 50
                    || !metaData.getColumnLabel(2).equals("message") || metaData.getColumnDisplaySize(2) < 65535
                    || !metaData.getColumnLabel(3).equals("author") || metaData.getColumnDisplaySize(3) < 500
                    || !metaData.getColumnLabel(4).equals("creation_date") || metaData.getColumnType(4) != 93) {
                throw new SQLException("Sql table for project has wrong format");
            } else {
                OutputUtil.debug("Sql table has correct format.");
            }
        } catch (SQLSyntaxErrorException e) {
            OutputUtil.debug("Table vcs_" + project + " does not exist or has wrong format so recreate it");
            recreateCommitTable(project);
        }
    }

    /**
     * this will delete the commit table of a project and recreates it
     *
     * @param project the project of which the table shall be recreated
     * @throws SQLException if the database connection failed or an error exist in a sql query
     */
    private void recreateCommitTable(String project) throws SQLException {
        connection.prepareStatement("DROP TABLE IF EXISTS `vcs_" + project + "`").executeUpdate();
        createCommitTable(project);
    }

    /**
     * creates the commit table for a project
     *
     * @param project the project for which the table shall be created
     * @throws SQLException if the table cannot be created, the database connection failed or an error exist in a sql query
     */
    private void createCommitTable(String project) throws SQLException {
        if (connection.prepareStatement("CREATE TABLE IF NOT EXISTS `vcs_" + project + "` ("
                + "`id` VARCHAR(50) NOT NULL, "
                + "`message` TEXT, "
                + "`author` VARCHAR(500), "
                + "`creation_date` TIMESTAMP, "
                + "PRIMARY KEY (`id`), "
                + "UNIQUE KEY `id` (`id`) "
                + ")").executeUpdate() != 0) {
            throw new SQLException("Can't create sql table for project");
        } else {
            OutputUtil.debug("Sql table created");
        }
    }

    /**
     * tests if the branch table has the correct format and if it does not exist it will be created
     *
     * @param project the project of which the table shall be checked
     * @throws SQLException if the table has the wrong format, the database connection failed or an error exist in a sql query
     */
    private void checkAndPrepareCommitBranchTable(String project) throws SQLException {
        try {
            ResultSet structure = connection.prepareStatement("select * from `vcs_" + project + "_branches` limit 0").executeQuery();
            ResultSetMetaData metaData = structure.getMetaData();
            if (metaData.getColumnCount() != 2
                    || !metaData.getColumnLabel(1).equals("commit") || metaData.getColumnDisplaySize(1) < 50
                    || !metaData.getColumnLabel(2).equals("branch") || metaData.getColumnDisplaySize(2) < 190) {
                throw new SQLException("Sql table for project has wrong format");
            } else {
                OutputUtil.debug("Sql table has correct format. ");
            }
        } catch (SQLSyntaxErrorException e) {
            OutputUtil.debug("Table vcs_" + project + "_branches does not exist or has wrong format so recreate it");
            recreateBranchTable(project);
        }
    }

    /**
     * this will delete the branch table of a project and recreates it
     *
     * @param project the project of which the table shall be recreated
     * @throws SQLException if the database connection failed or an error exist in a sql query
     */
    private void recreateBranchTable(String project) throws SQLException {
        connection.prepareStatement("DROP TABLE IF EXISTS `vcs_" + project + "_branches`").executeUpdate();
        createBranchTable(project);
    }

    /**
     * creates the branch table for a project
     *
     * @param project the project for which the table shall be created
     * @throws SQLException if the table cannot be created, the database connection failed or an error exist in a sql query
     */
    private void createBranchTable(String project) throws SQLException {
        if (connection.prepareStatement("CREATE TABLE IF NOT EXISTS `vcs_" + project + "_branches` ("
                + "`commit` VARCHAR(50) NOT NULL,"
                + "`branch` VARCHAR(190),"
                + "PRIMARY KEY (`commit`, `branch`), "
                + "UNIQUE KEY `id` (`commit`, `branch`) "
                + ")").executeUpdate() != 0) {
            throw new SQLException("Can't create sql table for project");
        } else {
            OutputUtil.debug("Sql table created");
        }
    }

    /**
     * tests if the diff table has the correct format and if it does not exist it will be created
     *
     * @param project the project of which the table shall be checked
     * @throws SQLException if the table has the wrong format, the database connection failed or an error exist in a sql query
     */
    private void checkAndPrepareDiffTable(String project) throws SQLException {
        try {
            ResultSet structure = connection.prepareStatement("select * from `vcs_" + project + "_diff` limit 0").executeQuery();
            ResultSetMetaData metaData = structure.getMetaData();
            if (metaData.getColumnCount() != 5
                    || !metaData.getColumnLabel(1).equals("new_commit") || metaData.getColumnDisplaySize(1) < 50
                    || !metaData.getColumnLabel(2).equals("old_commit") || metaData.getColumnDisplaySize(2) < 50
                    || !metaData.getColumnLabel(3).equals("diff_entry") || metaData.getColumnType(3) != 4
                    || !metaData.getColumnLabel(4).equals("new_name") || metaData.getColumnDisplaySize(4) < 2048
                    || !metaData.getColumnLabel(5).equals("old_name") || metaData.getColumnDisplaySize(5) < 2048) {
                throw new SQLException("Diff table for project has wrong format");
            } else {
                OutputUtil.debug("Sql table has correct format. ");
            }
        } catch (SQLSyntaxErrorException e) {
            OutputUtil.debug("Table vcs_" + project + "_diff does not exist or has wrong format so recreate it");
            recreateDiffTable(project);
        }
    }


    /**
     * this will delete the diff table of a project and recreates it
     *
     * @param project the project of which the table shall be recreated
     * @throws SQLException if the database connection failed or an error exist in a sql query
     */
    private void recreateDiffTable(String project) throws SQLException {
        connection.prepareStatement("DROP TABLE IF EXISTS `vcs_" + project + "_diff`").executeUpdate();
        createDiffTable(project);
    }

    /**
     * creates the diff table for a project
     *
     * @param project the project for which the table shall be created
     * @throws SQLException if the table cannot be created, the database connection failed or an error exist in a sql query
     */
    private void createDiffTable(String project) throws SQLException {
        if (connection.prepareStatement("CREATE TABLE IF NOT EXISTS `vcs_" + project + "_diff` ("
                + "`new_commit` varchar(50) NOT NULL,"
                + "`old_commit` varchar(50) NOT NULL,"
                + "`diff_entry` INT,"
                + "`new_name` VARCHAR(2048),"
                + "`old_name` VARCHAR(2048),"
                + "PRIMARY KEY (`new_commit`, `old_commit`, `diff_entry`) USING HASH, "
                + "UNIQUE KEY `id` (`new_commit`, `old_commit`, `diff_entry`) "
                + ")").executeUpdate() != 0) {
            throw new SQLException("Can't create sql table for project");
        } else {
            OutputUtil.debug("Sql table created");
        }
    }

    /**
     * this will delete all tables of a project and recreates them
     *
     * @param project the project of which the tables shall be recreated
     * @throws SQLException if the database connection failed or an error exist in a sql query
     */
    public void recreateVcsTables(String project) throws SQLException {
        recreateCommitTable(project);
        recreateBranchTable(project);
        recreateDiffTable(project);
    }


    /**
     * adds a statement to the batch queue to import a commit and branch with the given arguments
     * and executes the batch of sql queries for the commits and branches each 500 statements that were added
     *
     * @param commitId     the name of the commitId (eg. hash)
     * @param branch       the name of the branch; if null no entry for the commit will be created in the branch list
     * @param message      the commitId message
     * @param author       the author of the commit
     * @param creationDate the date the commit was created
     * @throws SQLException if the database connection failed or an error exist in a sql query
     */
    public void importCommit(String commitId, String branch, String message, String author, Date creationDate) throws SQLException {
        preparedInsertCommitStmt.setString(1, commitId);
        preparedInsertCommitStmt.setString(2, message);
        preparedInsertCommitStmt.setString(3, author);
        preparedInsertCommitStmt.setTimestamp(4, Timestamp.from(creationDate.toInstant()));
        preparedInsertCommitStmt.addBatch();
        if (branch != null) {
            preparedInsertCommitBranchStmt.setString(1, commitId);
            preparedInsertCommitBranchStmt.setString(2, branch);
            preparedInsertCommitBranchStmt.addBatch();
        }
        addedStmts++;
        executeBatches();
    }


    /**
     * adds a statement to the batch queue to import a commit and branch with the given arguments
     * and executes the batch of sql queries for the commits and branches each 500 statements that were added
     *
     * @param commitId     the name of the commitId (eg. hash)
     * @param branch       the name of the branch; if null no entry for the commit will be created in the branch list
     * @param message      the commitId message
     * @param author       the author of the commit
     * @param creationDate the date the commit was created
     * @throws SQLException if the database connection failed or an error exist in a sql query
     */
    public void importCommit(String commitId, String branch, String message, String author, LocalDateTime creationDate) throws SQLException {
        preparedInsertCommitStmt.setString(1, commitId);
        preparedInsertCommitStmt.setString(2, message);
        preparedInsertCommitStmt.setString(3, author);
        preparedInsertCommitStmt.setTimestamp(4, Timestamp.from(creationDate.toInstant(ZoneOffset.ofHours(2))));
        preparedInsertCommitStmt.addBatch();
        if (branch != null) {
            preparedInsertCommitBranchStmt.setString(1, commitId);
            preparedInsertCommitBranchStmt.setString(2, branch);
            preparedInsertCommitBranchStmt.addBatch();
        }
        addedStmts += 1;
        executeBatches();
    }


    /**
     * adds a statement for importing the diff represented by the parameters to a batch of such queries
     * and executes the batch of sql queries for the diffs each 500 statements that were added
     *
     * @param newCommit the id or hash of the newer revision
     * @param oldCommit the id or hash of the old revision
     * @param num the number of this change entry
     * @param newName new name of the file
     * @param oldName old name of the file
     * @throws SQLException if the database connection failed or an error exist in a sql query
     */
    public void importDiff(String newCommit, String oldCommit, int num, String newName, String oldName) throws SQLException {
        preparedInsertDiffStmt.setString(1, newCommit);
        preparedInsertDiffStmt.setString(2, oldCommit);
        preparedInsertDiffStmt.setInt(3, num);
        preparedInsertDiffStmt.setString(4, newName);
        preparedInsertDiffStmt.setString(5, oldName);
        addedDiffStmts++;
        try {
            preparedInsertDiffStmt.addBatch();
            if (addedDiffStmts % 500 == 0) {
                preparedInsertDiffStmt.execute();
            }
        } catch (SQLException e) {
            throw e;
        }
    }

    /**
     * executes the batch of sql queries for commits and branches each 500 statements that were added
     *
     * @throws SQLException if the database connection failed or an error exist in a sql query
     */
    private synchronized void executeBatches() throws SQLException {
        if (addedStmts % 500 == 0) {
            preparedInsertCommitStmt.executeBatch();
            preparedInsertCommitBranchStmt.executeBatch();
        }
    }

    /**
     * writes all pending changes to the database and closes the database connection after it
     *
     * @throws SQLException if the database connection failed or an error exist in a sql query
     */
    public void finishImport() throws SQLException {
        preparedInsertCommitStmt.executeBatch();
        preparedInsertCommitBranchStmt.executeBatch();
        preparedInsertDiffStmt.executeBatch();
        connection.commit();
        connection.close();
    }

    /**
     * searches commits from a project that matches the search parameters
     *
     * @param project the project from which the commits shall be retrieved
     * @param params  the parameters of the search
     * @return a list of commits matching the searchparameters
     * @throws SQLException if the database connection failed or an error exist in a sql query
     */
    public List<Commit> getCommitsForSearch(String project, VCSSearchParameters params) throws SQLException {
        String query = getQuery(project, params);
        PreparedStatement stmt = connection.prepareStatement(query);
        int i = 1;
        if (params.getAuthor() != null) {
            stmt.setString(i++, params.getAuthor());
        }
        if (params.getMessage() != null) {
            stmt.setString(i++, params.getMessage());
        }
        if (params.getTicket() != null) {
            stmt.setString(i++,  "[^[:alnum:]]" + params.getTicket() + "[^[:alnum:]]");
        }
        if (params.getIds() != null) {
            stmt.setArray(i++, connection.createArrayOf("text", params.getIds()));
        }
        if (params.getStartDate() != null) {
            LocalDateTime endDate = params.getEndDate();
            if (endDate == null) {
                endDate = LocalDateTime.now();
            }
            stmt.setTimestamp(i++, Timestamp.from(params.getStartDate().toInstant(ZoneOffset.ofHours(2))));
            stmt.setTimestamp(i++, Timestamp.from(endDate.toInstant(ZoneOffset.ofHours(2))));
        }
        if (params.getStartCommit() != null && params.getEndCommit() != null) {
            stmt.setString(i++, params.getStartCommit());
            stmt.setString(i++, params.getEndCommit());
        }
        if (params.getFile() != null) {
            stmt.setString(i++, params.getFile());
            stmt.setString(i++, params.getFile());
        }
        if (params.getBranch() != null) {
            stmt.setString(i++, params.getBranch());
        }
        ResultSet resultSet = stmt.executeQuery();
        List<Commit> commits = getCommits(resultSet);
        PreparedStatement diffsGetStmt = connection.prepareStatement(
                "SELECT * FROM `vcs_" + project + "_diff` WHERE `new_commit` LIKE ?"
        );
        PreparedStatement branchesGetStmt = connection.prepareStatement(
                "SELECT * FROM `vcs_" + project + "_branches` WHERE `commit` LIKE ?"
        );

        for (Commit commit : commits) {
            diffsGetStmt.setString(1, commit.getId());
            branchesGetStmt.setString(1, commit.getId());
            ResultSet diffResult = diffsGetStmt.executeQuery();
            commit.setChangedFiles(getChangedFiles(diffResult));
            ResultSet branchResult = branchesGetStmt.executeQuery();
            commit.setBranches(getBranches(branchResult));
        }
        connection.close();
        return commits;
    }

    /**
     * creates the sql query for the passed search parameters that will contain placeholders for the actual search values
     *
     * @param project the project from which the commits shall be retrieved
     * @param params  the parameters of the search
     * @return an sql query representing the search parameters and containing placeholders for the actual search values
     */
    private String getQuery(String project, VCSSearchParameters params) {
        String query = "";
        boolean usesDiff = false;
        boolean usesBranch = false;
        if (params.getAuthor() != null) {
            query += " AND `author` LIKE ? ";
        }
        if (params.getMessage() != null) {
            query += " AND `message` LIKE ? ";
        }
        if (params.getTicket() != null) {
            query += " AND `message` REGEXP ? ";
        }
        if (params.getIds() != null) {
            query += " AND `id` IN ( ? )";
        }
        if (params.getStartDate() != null) {
            query += " AND `creation_date` BETWEEN ? AND ? ";
        }
        if (params.getStartCommit() != null && params.getEndCommit() != null) {
            query += " AND (`creation_date` BETWEEN "
                    + "(SELECT `creation_date` FROM `vcs_" + project + "` WHERE `id` LIKE ?)"
                    + " AND "
                    + "(SELECT `creation_date` FROM `vcs_" + project + "` WHERE `id` LIKE ?)"
                    + ")";
        }
        if (params.getFile() != null) {
            usesDiff = true;
            query += " AND (`vcs_" + project + "_diff`.`new_commit` = `vcs_" + project + "`.`id` AND "
                    + "(`vcs_" + project + "_diff`.`new_name` LIKE ? OR `vcs_" + project + "_diff`.`old_name` LIKE ? ))";
        }
        if (params.getBranch() != null) {
            usesBranch = true;
            query += " AND (`id` = `vcs_" + project + "_branches`.`commit` AND `vcs_" + project + "_branches`.`branch` LIKE ? )";
        }
        query = "SELECT DISTINCT(`id`), `message`, `author`, `creation_date` FROM "
                + (usesDiff ? "`vcs_" + project + "_diff`, " : "")
                + (usesBranch ? "`vcs_" + project + "_branches`, " : "")
                + "`vcs_" + project + "`  WHERE 1 " + query + " ORDER BY `creation_date` ";
        return query;
    }

    /**
     * get commits of the project that have the ids contained in the passed array
     *
     * @param project the project from which the commits shall be retrieved
     * @param ids     the ids of commits
     * @return a list of commits that have the passed ids or null if an error occured
     */
    public List<Commit> getCommitsForIds(String project, String[] ids) {
        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM `vcs_" + project + "` WHERE `id` IN ?");
            stmt.setArray(1, connection.createArrayOf("varchar", ids));
            return getCommits(stmt.executeQuery());
        } catch (SQLException e) {
            OutputUtil.printError("An error occured while printing the search results" + e.getMessage());
            return null;
        }
    }

    /**
     * gets the commits from the resultset and returns them as a list
     *
     * @param resultSet a sql resultset from the commits table that contains a list of commits
     * @return a list of Commit from the resultSet
     * @throws SQLException if the resultset does not contain commit data, the resultset is closed or the database connection failed
     */
    private List<Commit> getCommits(ResultSet resultSet) throws SQLException {
        List<Commit> commits = new ArrayList<>();
        while (resultSet.next()) {
            commits.add(new Commit(
                    resultSet.getString("id"),
                    resultSet.getString("message"),
                    resultSet.getString("author"),
                    resultSet.getTimestamp("creation_date").toLocalDateTime()
            ));
        }
        return commits;
    }

    /**
     * gets the changed files from the resultset and returns them as a list
     *
     * @param resultSet a sql resultset from the _diff table that contains a list of changed files for a commit
     * @return a list of FileChange that represents the changed files contained in the resultset or null if no filechanges exist for in the resultset
     * @throws SQLException if the resultset does not contain filechange data, the resultset is closed or the database connection failed
     */
    private AbstractList<FileChange> getChangedFiles(ResultSet resultSet) throws SQLException {
        AbstractList<FileChange> fileChanges = new ArrayList<>();
        while (resultSet.next()) {
            fileChanges.add(new FileChange(
                    resultSet.getString("old_commit"),
                    resultSet.getString("new_name"),
                    resultSet.getString("old_name")
            ));
        }
        if (fileChanges.size() == 0) {
            return null;
        } else {
            return fileChanges;
        }
    }

    /**
     * gets the branches from the resultset and returns them as a list
     *
     * @param resultSet a sql resultset from the _branches table that contains a list of branches for a commit
     * @return a list of branches contained in the resultset or null if no branches exist in the resultset
     * @throws SQLException if the resultset does not contain branch data, the resultset is closed or the database connection failed
     */
    private AbstractList<String> getBranches(ResultSet resultSet) throws SQLException {
        AbstractList<String> branches = new ArrayList<>();
        while (resultSet.next()) {
            branches.add(resultSet.getString("branch"));
        }
        if (branches.size() == 0) {
            return null;
        } else {
            return branches;
        }
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
}
