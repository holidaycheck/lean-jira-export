package lean.jira.export.modelExport;

import java.util.List;


public class CSVFormatter {

    private char separator = ';';

    public void setSeparator(char separator) {
        this.separator = separator;
    }

    String formatLine(List<String> oneLineData) {
        StringBuffer buff = new StringBuffer();

        for (String value : oneLineData) {
            buff.append(separator);
            buff.append("\"");
            value = value.replaceAll("\"", "\"\""); // Escape quotes as in : This is "quoted text" => This is ""quoted text""
            buff.append(value);
            buff.append("\"");
        }

        // strip leading separator if there are any entries
        if (buff.length() > 0) {
            buff.deleteCharAt(0);
        }
        return buff.toString();
    }
}
