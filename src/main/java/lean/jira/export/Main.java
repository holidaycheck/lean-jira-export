package lean.jira.export;

import lean.jira.export.jiraImport.JiraClient;
import lean.jira.export.jiraImport.LeanIssueBuilder;
import lean.jira.export.model.LeanIssue;
import lean.jira.export.modelExport.CSVFileWriter;
import com.atlassian.jira.rest.client.api.domain.Issue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Main {


    private static Logger LOG = LoggerFactory.getLogger(Main.class);

    /**
        Exports stories matching the configured jql query for stories to a CSV-file in the current dir.
     */
    public static void main(String[] args) throws Exception {

        // create services:

        // source
        LOG.info("logIn to jira");
        JiraClient client = new JiraClient();
        client.logIn();


        // exporter
        CSVFileWriter csvFileWriter = new CSVFileWriter();


        // get data from source
        LOG.info("Querying jira (this may take a while depending on your JQL)");
        Iterable<Issue> jiraData = client.getStories();


        // transform data
        List<LeanIssue> data = new ArrayList<LeanIssue>();
        for(Issue jiraIssue : jiraData) {
            LOG.info("Processing issue " + jiraIssue.getKey());
            LeanIssueBuilder builder = new LeanIssueBuilder();
            data.add(builder.build(jiraIssue));
        }

        // export data
        csvFileWriter.export(data);

        // this seems to be necessary after having requested data might have some threads open
        System.exit(0);

    }
}






