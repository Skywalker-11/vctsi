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

/** Please read ALL of this class description before you try to use this class.
 *
 * This class contains parameters for the tests. Be aware that most of the tests won't be executed by default.
 * Uncomment the `//@test` line in front of the test functions in the test classes and provide the required parameters
 * (eg. credentials) there or in this file.
 *
 * SOME WILL RUN ON PUBLIC SERVERS so take others into account that want to use that services and talk back to the
 * service provider
 */
public class TestParameters {
    public static String DB_SERVER = "mysql://localhost";
    public static int DB_PORT = 3306;
    public static String DB_DB = "vctsi";
    public static String DB_USER = "vctsi-user";
    public static String DB_PASSWORD = "";

    public static String SVN_REMOTE1 = "";
    public static String SVN_LOCAL1 = "/path/to/your/svn";
    public static String SVN_PROJECT1 = "";
    public static String SVN_USER1 = "";
    public static String SVN_PASS1 = "";
    public static String SVN_PROJECT2 = "";
    public static String SVN_REMOTE2 = "/path/to/other/repo";
    public static String SVN_REMOTE3 = "svn://svn.code.sf.net/p/codeblocks/code/";

    public static String BUGZILLA_PATH1 = "https://bugzilla.redhat.com";
    public static String BUGZILLA_PROJECT1 = "test";
    public static String BUGZILLA_PATH2 = "https://bugzilla.mozilla.org/";
    public static String BUGZILLA_PROJECT2 = "Firefox";
    public static String BUGZILLA_PATH3 = "https://landfill.bugzilla.org/bugzilla-5.0-branch";
    public static String BUGZILLA_PROJECT3 = "Ѕpїdєr Séçretíøns";
    public static String BUGZILLA_TESTTITLE = "apparent";
    public static String BUGZILLA_TESTTITLE_2 = "shut down";

    public static String GIT_BOOTSTRAP_PATH = "/test/git/bootstrap";
    public static String GIT_SM_PROJECT = "";
    public static String GIT_SM_USER = "";
    public static String GIT_SM_PASS = "";

    public static String GITLAB_PATH = "https://gitlab.com/";
    public static String GITLAB_PROJECT = "";
    public static String GITLAB_TOKEN = "";

    public static String JIRA_PROJECT = "JRJC";
    public static String JIRA_USER = "";
    public static String JIRA_PASS = "";
    public static String JIRA_PATH = "https://ecosystem.atlassian.net";

    public static String GITHUB_USER = "";
    public static String GITHUB_PASS = "";


}
