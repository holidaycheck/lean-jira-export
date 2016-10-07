package lean.jira.export.jiraImport;

import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class JiraClient {

    private Logger LOG = LoggerFactory.getLogger(JiraClient.class);
    private JiraRestClient restClient;
    private ResourceBundle bundle;

    private int requestBlockSize;
    private int blockLimit;



    /**
     * This value will determine the number of items fetched from JIRA per request.
     * Since a parallel follow-up request will be sent for each item in the block,
     * this also determines the maximum number of parallel requests to the JIRA server
     *
     * Normally it will be configured in JiraClient.properties. But it can be overridden from client code (needed for tests)
     */
    public void setRequestBlockSize(int requestBlockSize) {
        this.requestBlockSize = requestBlockSize;
    }

    /**
     *
     * @param blockLimit Only fetch this many blocks
     */
    public void setBlockLimit(int blockLimit) {
        this.blockLimit = blockLimit;
    }


    public JiraClient() {
        this.bundle = PropertyResourceBundle.getBundle("JiraClient");
        // configure request block size. It may be overridden by external code
        String requestBlockSizeString = bundle.getString("requestBlockSize");
        this.requestBlockSize = Integer.parseInt(requestBlockSizeString);
        this.blockLimit = -1; // no limit.

    }

    public void logIn()  {
        URI jiraServerUri;
        try {
            jiraServerUri = new URI(this.bundle.getString("jiraURL"));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        final AsynchronousJiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
        this.restClient = factory.createWithBasicHttpAuthentication(jiraServerUri, this.bundle.getString("userName"), this.bundle.getString("password"));
    }

    public Iterable<Issue> getStories() {
        String storyJQL = this.bundle.getString("storyJQL");
        return getIssues(storyJQL);
    }


    public Iterable<Issue> getIssues(String jql) {

        LOG.debug("Querying for issues JQL: " +jql);

        // now let's put this into an overall iterator that we can return (hiding the complexity of remote iteration from clients)
        class RemoteIterator implements Iterator<Issue> {

            private int blockIndex = 0;
            private String storyJQL;
            private int itemIndex = 0;
            boolean abort = false;

            private Iterator<Issue> currentBlock;
            private int searchResultTotal = 0;

            public RemoteIterator(String storyJQL) {
                this.storyJQL = storyJQL;
                fetchBlock();
            }

            private void fetchBlock() {
                Set<String> fieldSet = new HashSet<String>();
                fieldSet.add("*navigable");
                final SearchResult searchResult = restClient.getSearchClient().searchJql(storyJQL,requestBlockSize,itemIndex,fieldSet).claim();
                LOG.trace(searchResult.toString());
                searchResultTotal = searchResult.getTotal();
                currentBlock = searchResult.getIssues().iterator();
                blockIndex++;
                if (blockLimit >= 0 && blockIndex > blockLimit) {
                    abort = true;
                }
            }

            public boolean hasNext() {
                if (abort) return false;
                return itemIndex < searchResultTotal;
            }

            public Issue next() {
                if (!hasNext()) {
                    throw new RuntimeException("iteration out of bounds");
                }

                LOG.trace("======= issue " + itemIndex + " =======");
                Issue nextIssue = currentBlock.next();
                nextIssue = restClient.getIssueClient().getIssue(nextIssue.getKey(), Arrays.asList(IssueRestClient.Expandos.CHANGELOG)).claim();

                itemIndex++;
                if (itemIndex % requestBlockSize == 0) {
                    // every requestBlockSize objects we need a new block
                    fetchBlock();
                }

                return nextIssue;
            }
        }

        final RemoteIterator iterator = new RemoteIterator(jql);

        return new Iterable<Issue>() {

            public Iterator<Issue> iterator() {
                return iterator;
            }
        };

    }




}
