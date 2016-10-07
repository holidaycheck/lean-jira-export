package lean.jira.export.jiraImport;

import lean.jira.export.model.LeanIssue;
import com.atlassian.jira.rest.client.api.domain.Issue;
import org.junit.Before;
import org.junit.Test;

public class ITJiraClient
{
    JiraClient client;


    @Before
    public void logIn() {
        this.client = new JiraClient();

        this.client.setBlockLimit(2);
        this.client.setRequestBlockSize(3);
        this.client.logIn();
    }

    @Test
    public void testStories() {
            Iterable<Issue> stories = client.getStories();
            for (Issue story : stories) {
                LeanIssue leanIssue = new LeanIssueBuilder().build(story);
                System.out.println(leanIssue);
            }
    }

}
