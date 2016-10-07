package lean.jira.export.modelExport;

import lean.jira.export.model.LeanIssue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Exports data to an UTF-8 encoded CSV file
 */
public class CSVFileWriter {

    private Logger LOG = LoggerFactory.getLogger(CSVFileWriter.class);


    private File file;


    public CSVFileWriter() {
        ResourceBundle bundle = ResourceBundle.getBundle("CSVFileWriter");
        configureFile(bundle);
    }



    private void configureFile(ResourceBundle bundle) {
        String outputDir = bundle.getString("outputDir");
        String outputFileDateFormat = bundle.getString("outputFileDateFormat");
        File dir = new File(outputDir);
        String dateString = (new SimpleDateFormat(outputFileDateFormat)).format(new Date());
        this.file = new File(dir, "JIRAExport_" + dateString + ".csv");
    }

    public void setFile(File file) {
        this.file = file;
    }




    private PrintWriter createPrintWriter() {
        LOG.info("Creating UTF-8 encoded output file " + file.getAbsolutePath());

        PrintWriter writer;
        try {
            writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8" ) );
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return writer;
    }


    public void export(Iterable<LeanIssue> data) {

        PrintWriter writer = createPrintWriter();

        LeanIssueCSVFormatter csvLineBuilder = new LeanIssueCSVFormatter(data);

        writer.println(csvLineBuilder.getFormattedCSVHeader());
        List<String> formattedCSVData = csvLineBuilder.getFormattedCSVData();

        int counter = 0;
        int size = formattedCSVData.size();
        LOG.info("Exporting " + size  + " lines of data to CSV output .. ");
        for (String dataLine : formattedCSVData) {
            if (counter % 10 == 0) {
                LOG.debug("lines " + counter + "/" + size);
            }
            writer.println(dataLine);
            counter++;
        }

        writer.flush();
        writer.close();
    }



}
