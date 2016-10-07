package lean.jira.export.modelExport;

import lean.jira.export.model.LeanIssue;
import lean.jira.export.model.LeanIssueState;

import java.text.*;
import java.util.*;

public class LeanIssueCSVFormatter {

    private static final long MS_IN_DAY = 24L * 60L * 60L * 1000L; // we output ms-values as 2-digit float days

    private List<LeanIssue> issueData = new ArrayList<LeanIssue>();
    private List<String> stateHeaders;
    private CSVFormatter formatter = new CSVFormatter();
    private List<String> csvHeaders;
    public NumberFormat outputNumberFormat;
    public DateFormat   outputDateFormat;


    public LeanIssueCSVFormatter(Iterable<LeanIssue> leanIssues) {
        // copy issue data (good practice, since an external iterator might only be iterable once)
        for (LeanIssue issue : leanIssues) issueData.add(issue);
        buildCSVHeaders(); // for this we need the data in place (since we're detecting which states are found)
        buildDataFormats();

    }

    private void buildDataFormats() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator(',');
        DecimalFormat decimalFormat = new DecimalFormat();
        decimalFormat.setDecimalFormatSymbols(symbols);
        outputNumberFormat = decimalFormat;


        outputNumberFormat.setMaximumFractionDigits(2);
        String outputDateFormatString = ResourceBundle.getBundle("CSVFileWriter").getString("outputDateFormat");
        outputDateFormat = new SimpleDateFormat(outputDateFormatString);
    }

    private void buildCSVHeaders() {
        stateHeaders = scanForStateHeaders();

        csvHeaders = new ArrayList<String>();
        csvHeaders.addAll(Arrays.asList("key", "summary" // , "description"
                , "currentStateName", "creationDate", "cycleEntryDate"));
        csvHeaders.addAll(stateHeaders);
        csvHeaders.addAll(Arrays.asList("leadTime", "cycleTime", "isResolved", "isWIP", "isInQueue"));
    }


    /**
     *
     * @return the list of headers for the CSV export (basic fields for every issue like creationDate and summary
     * PLUS the list of states that the issues might have spent time in (note: if there was a workflow-change we get all
     * states that ever were relevant for any ticket)
     */
    private List<String> scanForStateHeaders() {
        List<String> stateHeaders = new ArrayList<String>();

        // The list of states that are used as column headers for the CSV output
        // requires a scan of all issues. Older issues will have spent time in states
        // that are not part of the current
        // workflow anymore. Thus the list of states is dynamic and depends on the data.
        // We keep track of the states by eliminating duplicates
        // and keeping the order of appearance in the list of data (=> linked hash set)
        Set<String> statesInOrderOfAppearance = new LinkedHashSet<String>();
        for (LeanIssue issue : this.issueData) {
            for (String stateName : issue.getStateNames()) {
                statesInOrderOfAppearance.add(stateName);
            }
        }

        stateHeaders.addAll(statesInOrderOfAppearance);
        return stateHeaders;
    }


    public List<List<String>> getStructuredCSVData() {
        List<List<String>> csvData = new ArrayList<List<String>>();
        for (LeanIssue leanIssue : issueData) {
            csvData.add(getLineData(leanIssue));
        }
        return csvData;
    }

    private List<String> getLineData(LeanIssue leanIssue) {
        List<String> lineData = new ArrayList<String>();
        lineData.add(leanIssue.getKey());
        lineData.add(leanIssue.getSummary());
        lineData.add(leanIssue.getCurrentStateName());
        lineData.add(formatDate(leanIssue.getCreationDate()));
        lineData.add(formatDate(leanIssue.getCycleEntryDate()));

        // state data
        for (String stateName : stateHeaders) {
            LeanIssueState state = leanIssue.getState(stateName);
            if (state != null) {
                lineData.add(formatMs(state.getTotalTimeInStateMs()));
            } else {
                lineData.add("");
            }
        }

        // lean metrics
        lineData.add(formatMs(leanIssue.getLeadTimeMs()));
        lineData.add(formatMs(leanIssue.getCycleTimeMs()));

        lineData.add(Boolean.toString(leanIssue.isResolved()));
        lineData.add(Boolean.toString(leanIssue.isWIP()));
        lineData.add(Boolean.toString(leanIssue.isInQueue()));
        return lineData;
    }

    public String getFormattedCSVHeader() {
        return formatter.formatLine(csvHeaders);
    }

    public List<String> getFormattedCSVData() {
        List<String> dataLines = new ArrayList<String>();
        for (List<String> oneLineData : getStructuredCSVData()) {
            String dataLineAsString = formatter.formatLine(oneLineData);
            dataLines.add(dataLineAsString);
        }
        return dataLines;
    }


    public String formatDate(Date d) {
        if (d == null) {
            return "";
        }
        return outputDateFormat.format(d);
    }

    public String formatMs(long msValue) {
        double val = (double) msValue / (double) MS_IN_DAY;
        return outputNumberFormat.format(val);
    }


}
