package lean.jira.export.modelExport;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * Created by pmarshall on 04/10/16.
 */
public class CSVFileWriterTest {

    private TestData td = new TestData();

    @Test
    public void testExport() throws IOException {

        File f = File.createTempFile("CSVFileWriterTest", "csv");
        CSVFileWriter csvFileWriter = new CSVFileWriter();
        csvFileWriter.setFile(f);
        csvFileWriter.export(td.issueList);

    }



}
