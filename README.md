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

Note that exported files are UTF-8 encoded. Thus rather than just double clicking the *.csv file you will want to
import the files into google sheets or excel where you can tell the tool, which encoding the data has.

## Intention

This data should never be used to judge a team, but rather by the team itself and perhaps people intending to help the team
grow closer together and increase flow for more focused collaboration and earlier feedback.
Note that a low cycle-time only means that the story did not take so long (not how much was accomplished)

Have fun! 
