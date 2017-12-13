# VCTSI (version control and ticketing system interface)
This project provides a console interface for access to meta data of multiple version control and ticketing systems.
It was developed as part of a bachelor thesis at the [Universität Bremen](http://www.uni-bremen.de).

Ticketing systems:
 * Bugzilla
 * GitLab
 * GitHub
 * Jira

Version control systems:
 * Git
 * SVN

## Features
General:
 * Console program
 * Result as JSON objects
 * Modular integration of the different ticketing and version control systems

Ticketing systems:
 * Search and filter your different ticketing systems for meta data eg. creation date, ticket title and message, assignee, comment messages, .... over the SAME interface
 * Sychronize the data of your ticketing system to a local sql db for faster access and more filter possibilities then by the default apis
 * Circumvent API access limitation when accessing lots of data from your ticketing system
 
Version control systems:
 * Import full commit history form your version control system
 * Fast search for meta data eg. all commits from a specific person
 * SAME interface for git and svn access

## Install
### Create SQL-Database
Run the SQL queries:
`CREATE DATABASE vctsi`
`GRANT ALL PRIVILEGES ON vctsi.* to 'vctsi-user'@'localhost' IDENTIFIED BY 'your-pwd'`

All necessary tables will be created on the fly when executing the program

You should also enable utf8mb4 encoding in mysql if not done by default:
`ALTER DATABASE vctsi CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci`


### Run the program
Under vctsi-parent/target you can find a precompiled jar that includes all necessary dependencies (vctsi-core-1.1-jar-with-dependencies.jar) or build it yourself (instructions further below). To execute it run 

`java -jar ./target/vctsi-core-1.1-jar-with-dependencies.jar -whateverparameters ...` 

Show the help:
`java -jar ./target/vctsi-core-1.1-jar-with-dependencies.jar -h`

#### Examples for importing issues and commits

Import issues from a ticketing system (here GitHub) to local sql db:
`java -jar ./target/vctsi-core-1.1-jar-with-dependencies.jar -itsModule=github -itsProject="skywalker-11/vctsi" -dbUsername="vctsi-user" -dbPassword="secret"  -importIssues -itsUsername="your_github_username" -itsPassword="your_github_pwd"`

Import commits from git repository (repository has already be cloned for git):
`java -jar vctsi-parent/target/vctsi-core-1.1-jar-with-dependencies.jar -vcsModule=git -vcsProject=vctsi -dbUsername="vctsi-user" -dbPassword="secret" -vcsLocalPath="/path/to/the/cloned/git/repo" -importCommits`

Some systems require authentication, some do not and some also allow authentication by api token. Just try the itsModules and they will tell you what it supports if the parameter is missing.

#### Examples for searching for issues or commits
By default the searches require that you have imported tickets or commits for that project which you want to search in already. The ticketing systems also allows online search when providing `-itsOnline` as parameter (not recommended: online search has constrained apis).

Search commits with a given commit messages:
`java -jar ./target/vctsi-core-1.1-jar-with-dependencies.jar -vcsModule=git -vcsProject=vctsi -dbUsername="vctsi-user" -dbPassword="secret" -searchCommitMessage="First%"`

Most search parameters accept sql wildcard match `%`. Exception is the search for ticket/commit reference in commit/ticket messages where REGEXP syntax is required.
Search for all commits that references the ticket with id 22 in its commit message. VCTSI will automatically expand the id to match the format used for reference in the ticketing system (here GitHub->`#22`)
`java -jar ./target/vctsi-core-1.1-jar-with-dependencies.jar -vcsModule=git -itsModule=github -vcsProject=vctsi -dbUsername="vctsi-user" -dbPassword="secret" -searchCommitTicket="22"`

### Use the program in your own JAVA application
If you want to use VCTSI as a library in your own application you may want to take a look at the test classes which circumvent the console parser and configure all the required settings manually.

## Build from sources
Install and configure Maven and a JDK.
This will download this code repository and the one for support of Bugzilla (https://github.com/skywalker-11/j2bugzilla.git)
`git clone --recursive https://github.com/skywalker/vctsi.git`
`cd vctsi/vctsi-parent`
`mvn clean install`
Ready to use JAR will be in vctsi-parent/target/vctsi-core-<version>-jar-with-dependencies.jar

### Why are the tests in the source code not working?
The test classes provided are included only as examples for specific api functions or performance measurements. The parameters provided there would run the tests on PUBLIC servers of the services. So if you REALLY want to run them go to the class TestParameters under vctsi-core and follow the instructions.
