package lean.jira.export.modelExport;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Created by pmarshall on 04/10/16.
 */
public class CSVFormatterTest {

    private CSVFormatter formatter;

    @Before
    public void setup() {
        formatter = new CSVFormatter();
    }

    @Test
    public void testSimpleLine() {
        List<String> testData = Arrays.asList("No1", "No2", "No3");
        String expectedCSV = "\"No1\";\"No2\";\"No3\"";
        Assert.assertEquals("SimpleLine", expectedCSV, formatter.formatLine(testData));
    }

    @Test
    public void testQuotesAndSeparatorsInData() {
        List<String> testData = Arrays.asList("This is \"special\"", "This is OK", "This; is a semicolon");
        String expectedCSV = "\"This is \"\"special\"\"\";\"This is OK\";\"This; is a semicolon\"";
        Assert.assertEquals("LineWithQuotesAndSeparators", expectedCSV, formatter.formatLine(testData));
    }

    @Test
    public void testOtherSeparator() {
        List<String> testData = Arrays.asList("This is \"special\"", "This is OK", "This; is a semicolon");
        String expectedCSV = "\"This is \"\"special\"\"\",\"This is OK\",\"This; is a semicolon\"";
        formatter.setSeparator(',');
        Assert.assertEquals("LineWith other Separator", expectedCSV, formatter.formatLine(testData));
    }
}
