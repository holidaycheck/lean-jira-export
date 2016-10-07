# LeanJIRAExport

Allows to export jira issues based on configurable JQL including lean metrics data such as _lead-time_ &amp; _cycle-time_

## Configuration

Before you can build and run this tool you need to configure it (i.e. telling the tool which JIRA repo to use, which account and JQL etc.) 
To do this cd to src/main/resources and copy all *.properties.tpl files to corresponding *.properties files.
Note that while the *.tpl files _are_ checked in the *.properties files are NOT! They will contain your password, repo URL etc. and so they cannot go up to GitHub. 

```shell
cp JiraClient.properties.tpl JiraClient.properties
cp CSVFileWriter.properties.tpl CSVFileWriter.properties
cp log4j.properties.tpl log4j.properties
```

Now go edit the 3 *.properties files for your needs.

## Building

Just do a 
```shell
./build.sh
```

This should trigger maven (that is required on your local system and must be added to your PATH).
If you don't have maven go get it from https://maven.apache.org

## Running

Just do a 
```shell
./run.sh
```

This will run the tool and export data to the directory exported-data

## Using the data

### Encoding
Note that exported files are UTF-8 encoded. Thus rather than just double clicking the *.csv file you will want to
import the files into google sheets or excel where you can tell the tool, which encoding the data has.

### Assumptions made about JIRA
The tool asumes a linear workflow where the first state is the queue state (like TODO or Open etc.) and the last state is the END of the cycle (like CLOSED or LIVE etc.). 

Any (minimally 1) states inbetween are considered as contributing to the cycle-time. Stories may go back and forth in between (e.g. from PROGRAMMING to REVIEW and back then a leap forward to ACCEPTANCE and finally to CLOSED). 

The tool assumes that the last state is the one that sets resolution (If you use JIRA Agile and the Simplified JIRA Workflow) there is a checkbox you can apply to "Board/Configure/Columns" at the last column's state, to set to resolved when the state is entered. 

As long as resolved is not set the tool calculates the duration in the last state up to the moment of export (e.g. now). This allows you to look into the cycle-time as it grows while long-running tickets are open. 

Finally the tool does not handle Subtasks explicitly (this would be an interesting future development). 
Rather it just assumes that your stories transition through at least one of the intermediate states. 

**_Warning_**: When you sometimes use subtickets and sometimes don't you need to make sure that the story itself enters a PROGRESS - state as soon as the first sub-task does. To automate this check out https://confluence.atlassian.com/jirakb/moving-a-sub-task-to-in-progress-does-not-transition-its-parent-s-to-the-same-status-779158949.html

## Intention

This data should never be used to judge a team, but rather by the team itself and perhaps people intending to help the team
grow closer together and increase flow for more focused collaboration and earlier feedback.
Note that a low cycle-time only means that the story did not take so long (not how much was accomplished)

Have fun! 
