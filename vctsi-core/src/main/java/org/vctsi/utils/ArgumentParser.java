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

import org.vctsi.internal.DBSettings;
import org.vctsi.internal.its.ITSModule;
import org.vctsi.internal.its.ITSSearchParameters;
import org.vctsi.internal.its.ITSSettings;
import org.vctsi.internal.tasks.*;
import org.vctsi.internal.vcs.VCSDiffSettings;
import org.vctsi.internal.vcs.VCSModule;
import org.vctsi.internal.vcs.VCSSearchParameters;
import org.vctsi.internal.vcs.VCSSettings;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

public class ArgumentParser {

    static final String ERROR_UNKNOWN_IDENTIFIER = "ERROR: unknown argument ",
            ERROR_PARSE_VCS_MODULE = "ERROR: parsing parameter -vcsModule: invalid module",
            ERROR_PARSE_ITS_MODULE = "ERROR: parsing parameter -itsModule: invalid module",
            ERROR_NO_TASKS_GIVEN = "ERROR: no tasks given. Use `-h` for help",
            ERROR_ISSUEID_NOT_A_NUMBER = "ERROR: issueid is not a number",
            ERROR_PORT_NOT_A_NUMBER = "ERROR: dbPort is not a number",
            ERROR_ONLY_ONE_TASK = "ERROR: only one task per execution possible",
            ERROR_PARSE_DATETIME = "ERROR: parsing datetime object of ",
            ERROR_PARSE_INTEGERS = "ERROR: parsing integers of ";

    private Task task;
    private DBSettings dbSettings = new DBSettings();

    private VCSSettings vcsSettings = new VCSSettings();
    private VCSSearchParameters vcsSearchParameters = new VCSSearchParameters();
    private VCSDiffSettings diffSettings = new VCSDiffSettings();
    private Class<? extends VCSModule> vcsModule;

    private ITSSettings itsSettings = new ITSSettings();
    private ITSSearchParameters itsSearchParameters = new ITSSearchParameters();
    private Class<? extends ITSModule> itsModule;

    /**
     * This will parse the given arguments
     *
     * @param args the command line arguments which are passed while starting
     *             the program
     * @return true if parsed successful, false if errors occures or help is requested and the program should be stopped
     */
    public boolean parse(String[] args) {
        for (String arg : args) {
            String[] argi = arg.split("=", 2);
            if (argi[0].equals("-h")) {
                System.out.print(getHelpText());
                return false;
            } else {
                String error = parseArg(argi);
                if (error != null) {
                    OutputUtil.printError(error);
                    return false;
                }
            }
        }

        if (task == null) {
            OutputUtil.printError(ERROR_NO_TASKS_GIVEN);
            return false;
        }
        if (vcsModule != null) {
            task.setVcsModule(vcsModule);
        }
        if (itsModule != null) {
            task.setItsModule(itsModule);
        }

        if (task instanceof GetDiffsTask) {
            ((GetDiffsTask) task).setDiffSettings(diffSettings);
        } else if (task instanceof SearchCommitTask) {
            ((SearchCommitTask) task).setSearchParameters(vcsSearchParameters);
        } else if (task instanceof SearchIssueTask) {
            ((SearchIssueTask) task).setSearchParameters(itsSearchParameters);
        }
        return true;
    }

    public Task getTask() {
        return task;
    }

    public VCSSettings getVcsSettings() {
        return vcsSettings;
    }

    public ITSSettings getItsSettings() {
        return itsSettings;
    }

    public DBSettings getDbSettings() {
        return dbSettings;
    }

    /**
     * parses an argument and prints an error message if parsing fails
     *
     * @param argi array containing the identifier and value of one command line
     *             parameter
     * @return null if argument was parsed successfully, else an error message
     */
    private String parseArg(String[] argi) {
        String identifier = argi[0];
        String value = "";
        if (argi.length > 1) {
            value = argi[1];
        }
        if (identifier.startsWith("-vcs")) {
            return parseVcs(identifier, value);
        }
        if (identifier.startsWith("-its")) {
            return parseIts(identifier, value);
        }
        if (identifier.startsWith("-db")) {
            return parseDB(identifier, value);
        }
        if (identifier.startsWith("-searchIssue")) {
            return parseSearchIssue(identifier, value);
        }
        if (identifier.startsWith("-searchCommit")) {
            return parseSearchCommit(identifier, value);
        }
        if (identifier.startsWith("-getDiffs")) {
            return parseGetDiffs(identifier, value);
        }
        switch (identifier) {
            case "-getIssue":
                try {
                    int issueId = Integer.parseInt(value.trim());
                    task = new GetIssueTask(issueId);
                } catch (NumberFormatException e) {
                    return ERROR_ISSUEID_NOT_A_NUMBER + "(" + value + ")" + e.getMessage();
                }
                break;
            case "-importIssues":
                task = new ImportIssuesTask();
                break;
            case "-getCommit":
                task = new GetCommitTask(value);
                break;
            case "-importCommits":
                task = new ImportCommitsTask();
                break;
            default:
                return ERROR_UNKNOWN_IDENTIFIER + identifier;
        }
        return null;
    }

    /**
     * parses the vcs settings
     *
     * @param key   the key of the  setting
     * @param value the value of the setting
     * @return null if successful; else a error message
     */
    private String parseVcs(String key, String value) {
        switch (key) {
            case "-vcsModule":
                vcsModule = VCSModule.availableVCSModules.get(value);
                if (vcsModule == null) {
                    return ERROR_PARSE_VCS_MODULE;
                }
                break;
            case "-vcsRemotePath":
                vcsSettings.setRemotePath(value);
                break;
            case "-vcsLocalPath":
                vcsSettings.setLocalPath(value);
                break;
            case "-vcsProject":
                vcsSettings.setProject(value);
                break;
            case "-vcsUsername":
                vcsSettings.setUsername(value);
                break;
            case "-vcsPassword":
                vcsSettings.setPassword(value);
                break;
            case "-vcsOnlyNew":
                vcsSettings.setOnlyNew(true);
                break;
            case "-vcsNoUpdate":
                vcsSettings.setNoUpdate(true);
                break;
            case "-vcsSshKey":
                vcsSettings.setSshKey(value);
                break;
            case "-vcsBranchRootFolder":
                vcsSettings.setBranchRootFolder(value);
                break;
            default:
                return ERROR_UNKNOWN_IDENTIFIER + key;
        }
        return null;
    }

    /**
     * parses the its settings
     *
     * @param key   the key of the  setting
     * @param value the value of the setting
     * @return null if successful; else a error message
     */
    private String parseIts(String key, String value) {
        switch (key) {
            case "-itsPath":
                itsSettings.setPath(value);
                break;
            case "-itsProject":
                itsSettings.setProject(value);
                break;
            case "-itsUsername":
                itsSettings.setUsername(value);
                break;
            case "-itsPassword":
                itsSettings.setPassword(value);
                break;
            case "-itsOnline":
                itsSettings.setOnline(true);
                break;
            case "-itsModule":
                itsModule = ITSModule.availableITSModules.get(value);
                if (itsModule == null) {
                    return ERROR_PARSE_ITS_MODULE;
                }
                break;
            default:
                return ERROR_UNKNOWN_IDENTIFIER + key;
        }
        return null;
    }

    /**
     * parses the db settings
     *
     * @param key   the key of the  setting
     * @param value the value of the setting
     * @return null if successful; else a error message
     */
    private String parseDB(String key, String value) {
        switch (key) {
            case "-dbServer":
                dbSettings.setServer(value);
                break;
            case "-dbPort":
                try {
                    dbSettings.setPort(Integer.parseInt(value.trim()));
                } catch (NumberFormatException e) {
                    return ERROR_PORT_NOT_A_NUMBER;
                }
                break;
            case "-dbDatabase":
                dbSettings.setDb(value);
                break;
            case "-dbUsername":
                dbSettings.setUsername(value);
                break;
            case "-dbPassword":
                dbSettings.setPassword(value);
                break;
            default:
                return ERROR_UNKNOWN_IDENTIFIER + key;
        }
        return null;
    }

    /**
     * parses the parameters for an issue search
     *
     * @param key   the name of the parameter
     * @param value the value of the parameter
     * @return null if successful; else a error message
     */
    private String parseSearchIssue(String key, String value) {
        if (task != null && !(task instanceof SearchIssueTask)) {
            return ERROR_ONLY_ONE_TASK;
        }
        task = new SearchIssueTask();
        switch (key) {
            case "-searchIssueAssignee":
                itsSearchParameters.setAssignee(value);
                break;
            case "-searchIssueAuthor":
                itsSearchParameters.setAuthor(value);
                break;
            case "-searchIssueCommit":
                itsSearchParameters.setCommit(value);
                break;
            case "-searchIssueDescription":
                itsSearchParameters.setDescription(value);
                break;
            case "-searchIssueEndDate":
                try {
                    itsSearchParameters.setEndDate(getLocalDateTime(value));
                } catch (DateTimeParseException e) {
                    return ERROR_PARSE_DATETIME + key;
                }
                break;
            case "-searchIssueStartDate":
                try {
                    itsSearchParameters.setStartDate(getLocalDateTime(value));
                } catch (DateTimeParseException e) {
                    return ERROR_PARSE_DATETIME + key;
                }
                break;
            case "-searchIssueIds":
                try {
                    String[] vals = value.split(",");
                    Integer[] ids = new Integer[vals.length];
                    for (int i = 0; i < vals.length; i++) {
                        ids[i] = Integer.parseInt(vals[i].trim());
                    }
                    itsSearchParameters.setIds(ids);
                } catch (NumberFormatException e) {
                    return ERROR_PARSE_INTEGERS + key;
                }
                break;
            case "-searchIssueNames":
                itsSearchParameters.setNames(value.split(","));
                break;
            case "-searchIssueState":
                itsSearchParameters.setState(value);
                break;
            case "-searchIssueTargetVersion":
                itsSearchParameters.setTargetVersion(value);
                break;
            case "-searchIssueTitle":
                itsSearchParameters.setTitle(value);
                break;
            default:
                return ERROR_UNKNOWN_IDENTIFIER + key;

        }
        return null;
    }

    /**
     * parses the parameters for an commit search
     *
     * @param key   the name of the parameter
     * @param value the value of the parameter
     * @return null if successful; else a error message
     */
    private String parseSearchCommit(String key, String value) {
        if (task != null && !(task instanceof SearchCommitTask)) {
            return ERROR_ONLY_ONE_TASK;
        }
        task = new SearchCommitTask();
        switch (key) {
            case "-searchCommitBranch":
                vcsSearchParameters.setBranch(value);
                break;
            case "-searchCommitFile":
                vcsSearchParameters.setFile(value);
                break;
            case "-searchCommitEndCommit":
                vcsSearchParameters.setEndCommit(value);
                break;
            case "-searchCommitEndDate":
                try {
                    vcsSearchParameters.setEndDate(getLocalDateTime(value));
                } catch (DateTimeParseException e) {
                    return ERROR_PARSE_DATETIME + key;
                }
                break;
            case "-searchCommitStartCommit":
                vcsSearchParameters.setStartCommit(value);
                break;
            case "-searchCommitStartDate":
                try {
                    vcsSearchParameters.setStartDate(getLocalDateTime(value));
                } catch (DateTimeParseException e) {
                    return ERROR_PARSE_DATETIME + key;
                }
                break;
            case "-searchCommitAuthor":
                vcsSearchParameters.setAuthor(value);
                break;
            case "-searchCommitIds":
                vcsSearchParameters.setIds(value.split(","));
                break;
            case "-searchCommitMessage":
                vcsSearchParameters.setMessage(value);
                break;
            case "-searchCommitTicket":
                vcsSearchParameters.setTicket(value);
                break;
            default:
                return ERROR_UNKNOWN_IDENTIFIER + key;
        }
        return null;
    }

    /**
     * parses the parameters for an diff request
     *
     * @param key   the name of the parameter
     * @param value the value of the parameter
     * @return null if successful; else a error message
     */
    private String parseGetDiffs(String key, String value) {
        if (task != null && !(task instanceof GetDiffsTask)) {
            return ERROR_ONLY_ONE_TASK;
        }
        task = new GetDiffsTask();
        switch (key) {
            case "-getDiffsCommit1":
                diffSettings.setCommit1(value);
                break;
            case "-getDiffsCommit2":
                diffSettings.setCommit2(value);
                break;
            case "-getDiffsPath":
                diffSettings.setPath(value);
                break;
            default:
                return ERROR_UNKNOWN_IDENTIFIER + key;
        }
        return null;
    }

    static final String helpText1 = "The following arguments are available:" + System.lineSeparator()
            + "  * -h : print this help" + System.lineSeparator()
            + "For the parameter and values following syntax will be assumed:" + System.lineSeparator()
            + "  * the parameters then will be identified by the key string followed by a '=' and the value" + System.lineSeparator()
            + "  * date: passed as string in time in millis or ISO-8601 format" + System.lineSeparator()
            + "  * arrays: values of the array are seperated by ',' " + System.lineSeparator()
            + System.lineSeparator()
            + "----------------------------------------------------------------------------------------------------" + System.lineSeparator()
            + "  * settings for database connection: " + System.lineSeparator()
            + "   ** -dbServer (string): the sql server for offline search inclusive jdbc driver (eg: mysql://localhost) (default: mysql://localhost)" + System.lineSeparator()
            + "   ** -dbPort (int): the sql server port (default: 3306)" + System.lineSeparator()
            + "   ** -dbDatabase (string): sql database (default: vctsi" + System.lineSeparator()
            + "   ** -dbUsername (string): sql user" + System.lineSeparator()
            + "   ** -dbPassword (string): sql password" + System.lineSeparator()
            + System.lineSeparator()
            + "  * settings for vcs module: " + System.lineSeparator()
            + "   ** -vcsProject (string): the project/repo name (will also be used as part of sql table names)" + System.lineSeparator()
            + "   ** -vcsLocalPath (string): local file path at which the files of git, svn, ... are stored (eg. the repository folder)" + System.lineSeparator()
            + "   ** -vcsRemotePath (string): the uri to the remote repository (eg. http://.../project.git)" + System.lineSeparator()
            + "   ** -vcsOnlyNew: no value. if set only new revisions (difference from local and remote) will be imported" + System.lineSeparator()
            + "   ** -vcsNoUpdate: no value. if set the local repo will not be updated. Not combinable with -vcsOnlyNew" + System.lineSeparator()
            + "   ** -vcsBranchRootFolder (string): a folder that contains the roots of the branches" + System.lineSeparator()
            + "   ** -vcsUsername (string): the username to authenticate at the remote repository;" + System.lineSeparator()
            + "                   when using token based authentication (eg. for github) use \"token\" as username" + System.lineSeparator()
            + "   ** -vcsPassword (string): the password or api token to authenticate at the remote repository" + System.lineSeparator()
            + "   ** -vcsModule (string): the module that should be used for version control systems" + System.lineSeparator()
            + "                   The following vsc modules are available:" + System.lineSeparator();
    static final String helpTextPart2 = System.lineSeparator()
            + "  * settings for its module: " + System.lineSeparator()
            + "   ** -itsProject (string): the project name (will also be used as part of sql table names)" + System.lineSeparator()
            + "   ** -itsPath (string): the uri to the ticketing system (eg. https://bugzilla.mozilla.org)" + System.lineSeparator()
            + "   ** -itsUsername (string): the username to authenticate at the remote repository;" + System.lineSeparator()
            + "                   when using token based authentication (eg. for github) use \"token\" as username" + System.lineSeparator()
            + "   ** -itsPassword (string): the password or api token to authenticate at the remote repository" + System.lineSeparator()
            + "   ** -itsOnline: no value. if set the searches will be executed online. if not on the local database" + System.lineSeparator()
            + "   ** -itsModule (string): the module that should be used for ticket system" + System.lineSeparator()
            + "                   The following its modules are available:" + System.lineSeparator();
    static final String helpTextPart3 = System.lineSeparator()
            + System.lineSeparator()
            + "The following defines the task to be executed. Only one task can be executed at a time"
            + "  * search a commits: " + System.lineSeparator()
            + "   ** -searchCommitBranch (string): branch from which commits shall be retrieved" + System.lineSeparator()
            + "   ** -searchCommitFile (string): commits that changes the file " + System.lineSeparator()
            + "   ** -searchCommitEndCommit (string): till which commit they should be retrieved (startCommit have to be set)" + System.lineSeparator()
            + "   ** -searchCommitEndDate (date): till which date the commits shall be retrieved (requires startDate);" + System.lineSeparator()
            + "                            if not set the current date will be used" + System.lineSeparator()
            + "   ** -searchCommitStartCommit (string): from which commit on they shall be retrieved" + System.lineSeparator()
            + "   ** -searchCommitStartDate (date): from which date on the commits shall be retrieved" + System.lineSeparator()
            + "   ** -searchCommitAuthor (string): author of the commit" + System.lineSeparator()
            + "   ** -searchCommitIds (int[]): ids of commits that should be retrieved" + System.lineSeparator()
            + "   ** -searchCommitMessage (string): commit message" + System.lineSeparator()
            + "   ** -searchCommitTicket (string): a ticket id mentioned in the commit message" + System.lineSeparator()
            + System.lineSeparator()
            + "  * search an issue: " + System.lineSeparator()
            + "   ** -searchIssueAssignee (string): the assignee of an issue" + System.lineSeparator()
            + "   ** -searchIssueAuthor (string): author of an issue" + System.lineSeparator()
            + "   ** -searchIssueCommit (string): id of a commit (hash or revision id) mentioned in the title or description" + System.lineSeparator()
            + "   ** -searchIssueDescription (string): description of issue" + System.lineSeparator()
            + "   ** -searchIssueEndDate (date): date till that the issues shall be retrieved (requires startDate)" + System.lineSeparator()
            + "   ** -searchIssueStartDate (date): date from which on the issues shall be retrieved" + System.lineSeparator()
            + "   ** -searchIssueIds (int[]): ids  of issues that should be received" + System.lineSeparator()
            + "   ** -searchIssueNames (string[]): names of issues (some its eg. Jira use names as identifier of the issue)" + System.lineSeparator()
            + "   ** -searchIssueState (string): the state of the issue (standardisiert: NEW,ASSIGNED,SOLVED,REOPENED,UNCONFIRMED,VERIFIED,RESOLVED)" + System.lineSeparator()
            + "   ** -searchIssueTargetVersion (string): assinged targetVersion alias milestone" + System.lineSeparator()
            + "   ** -searchIssueTitle (string): title of issue" + System.lineSeparator()
            + System.lineSeparator()
            + "  * get diff between two revisions" + System.lineSeparator()
            + "   ** -getDiffsCommit1 (string): first revision (id (svn) or hash (git))" + System.lineSeparator()
            + "   ** -getDiffsCommit2 (string): second revision (id (svn) or hash (git))" + System.lineSeparator()
            + "   ** -getDiffsPath (string): a path of a file/folder of that the diff should be made" + System.lineSeparator()
            + System.lineSeparator()
            + "  * get an issue for an id" + System.lineSeparator()
            + "   ** -getIssue (int): id of an issue" + System.lineSeparator()
            + System.lineSeparator()
            + "  * get a commit for an id (svn) or a hash (git)" + System.lineSeparator()
            + "   ** -getCommit (string): id or hash that identifies a commit" + System.lineSeparator()
            + System.lineSeparator()
            + "  * import issues to local database" + System.lineSeparator()
            + "    ** -importIssues: no value" + System.lineSeparator()
            + System.lineSeparator()
            + "  * import commmits to local database" + System.lineSeparator()
            + "    ** -importCommits: no value" + System.lineSeparator();


    /**
     * returns the help text
     */
    private String getHelpText() {
        String availableVCSModules = "";
        for (String module : VCSModule.availableVCSModules.keySet()) {
            availableVCSModules += "    *** " + module + "" + System.lineSeparator();
        }
        String availableITSModules = "";
        for (String module : ITSModule.availableITSModules.keySet()) {
            availableITSModules += "    *** " + module + "" + System.lineSeparator();
        }
        return helpText1 + availableVCSModules + helpTextPart2 + availableITSModules + helpTextPart3;
    }

    /**
     * returns a LocalDateTime representation of the value
     * @param value a string containing the time in milliseconds or in a Format known to the LocalDateTime parser (eg. ISO datetime)
     * @return the LocalDateTime representative of the value
     * @throws DateTimeParseException value does not contain the time in milliseconds nor a parsable format
     */
    private LocalDateTime getLocalDateTime(String value) throws DateTimeParseException {
        LocalDateTime time;
        try {
            time = LocalDateTime.ofEpochSecond(Long.parseLong(value), 0, ZoneOffset.ofHours(2));
        } catch (NumberFormatException e) {
            time = LocalDateTime.parse(value);
        }
        return time;
    }
}
