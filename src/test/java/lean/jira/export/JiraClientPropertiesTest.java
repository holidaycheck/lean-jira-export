package lean.jira.export;

import junit.framework.Assert;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class JiraClientPropertiesTest {

    private ResourceBundle bundle;

    /** Copy this code into your client code and access only tested properties **/
    @Before
    public void setup() {
        this.bundle = PropertyResourceBundle.getBundle("JiraClient");
    }

    @Test
    public void testUserName() {
        Assert.assertTrue(StringUtils.isNotBlank(this.bundle.getString("userName")));
    }

    @Test
    public void testPassword() {
        Assert.assertTrue(StringUtils.isNotBlank(this.bundle.getString("password")));
    }

    @Test
    public void testJiraURL() {
        Assert.assertTrue(StringUtils.isNotBlank(this.bundle.getString("jiraURL")));
    }

    @Test
    public void testStoryJQL() {
        Assert.assertTrue(StringUtils.isNotBlank(this.bundle.getString("storyJQL")));
    }

    @Test
    public void testRequestBlockSize() {
        String requestBlockSize = this.bundle.getString("requestBlockSize");
        Assert.assertTrue(StringUtils.isNotBlank(requestBlockSize));
        try {
            Assert.assertNotNull(Integer.parseInt(requestBlockSize));
        } catch (NumberFormatException nfe) {
            Assert.fail("Cannot parse bundle.requestBlockSize=" + requestBlockSize + " as Integer!");
        }
    }

}
