package lean.jira.export.jiraImport;

import com.atlassian.httpclient.api.Response;
import com.atlassian.httpclient.api.ResponsePromise;
import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.auth.BasicHttpAuthenticationHandler;
import com.atlassian.jira.rest.client.internal.async.AsynchronousHttpClientFactory;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClient;
import com.atlassian.jira.rest.client.internal.async.DisposableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class JiraClient {

    private Logger LOG = LoggerFactory.getLogger(JiraClient.class);
    private JiraRestClient restClient;

    private int requestBlockSize;
    private int blockLimit;
    private String username;
    private String password;
    private String storyJql;
    private String jiraUrl;



    /**
     * This value will determine the number of items fetched from JIRA per request.
     * Since a parallel follow-up request will be sent for each item in the block,
     * this also determines the maximum number of parallel requests to the JIRA server
     *
     * Normally it will be configured in JiraClient.properties. But it can be overridden from client code (needed for tests)
     */
    void setRequestBlockSize(int requestBlockSize) {
        this.requestBlockSize = requestBlockSize;
    }

    /**
     *
     * @param blockLimit Only fetch this many blocks
     */
    void setBlockLimit(int blockLimit) {
        this.blockLimit = blockLimit;
    }


    public JiraClient() throws IOException {
        this(PropertyResourceBundle.getBundle("JiraClient").getString("storyJQL"));

    }

    public JiraClient(String storyJql) throws IOException {
        ResourceBundle bundle = PropertyResourceBundle.getBundle("JiraClient");
        // configure request block size. It may be overridden by external code
        String requestBlockSizeString = bundle.getString("requestBlockSize");
        this.requestBlockSize = Integer.parseInt(requestBlockSizeString);
        this.blockLimit = -1; // no limit.
        this.username = bundle.getString("userName");
        this.password = bundle.getString("password");
        this.jiraUrl = bundle.getString("jiraURL");
        this.storyJql = storyJql;
        logIn();

    }

    private void logIn() throws IOException {
        URI jiraServerUri;
        try {
            jiraServerUri = new URI(this.jiraUrl);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        // Obtain session
        final String authString = "{\"username\": \""+username+"\", \"password\": \""+password+"\"}";

        final BasicHttpAuthenticationHandler basicHttpAuthenticationHandler = new BasicHttpAuthenticationHandler(username, password);

        final DisposableHttpClient httpClient = new AsynchronousHttpClientFactory()
                .createClient(jiraServerUri, basicHttpAuthenticationHandler);

        ResponsePromise responsePromise = httpClient
                .newRequest(jiraUrl + "/rest/auth/1/session")
                .setContentType("application/json")
                .setAccept("application/json")
                .setEntity(authString)
                .post();

        Response response = responsePromise.claim();
        System.out.println(response.getStatusCode());
        this.restClient = new AsynchronousJiraRestClient(jiraServerUri, httpClient);

        LOG.info("LoggedIn as : " + restClient.getUserClient().getUser(username).claim().getDisplayName());

    }

    public Iterable<Issue> getStories() {
        return getIssues(storyJql);
    }


    Iterable<Issue> getIssues(final String jql) {

        LOG.debug("Querying for issues JQL: " +jql);

        // now let's put this into an overall iterator that we can return (hiding the complexity of remote iteration from clients)
        class RemoteIterator implements Iterator<Issue> {

            private int blockIndex = 0;
            private String storyJQL;
            private int itemIndex = 0;
            boolean abort = false;

            private Iterator<Issue> currentBlock;
            private int searchResultTotal = 0;

            RemoteIterator(String storyJQL) {
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

            @Override
            public void remove() {
                throw new UnsupportedOperationException("remove");
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
