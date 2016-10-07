package lean.jira.export.model;

import java.util.Date;

public class LeanIssueState {

    private String name;
    private Date firstEntryDate;
    private Date lastExitDate;
    private Date lastEntryDate;
    private int numTimesInState = 0;
    private long totalTimeInStateMs = 0l;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getFirstEntryDate() {
        return firstEntryDate;
    }

    public void setFirstEntryDate(Date firstEntryDate) {
        this.firstEntryDate = firstEntryDate;
    }

    public Date getLastExitDate() {
        return lastExitDate;
    }

    public void setLastExitDate(Date exitDate) {
        this.lastExitDate = exitDate;
    }

    public Date getLastEntryDate() {
        return lastEntryDate;
    }

    public void setLastEntryDate(Date lastEntryDate) {
        this.lastEntryDate = lastEntryDate;
    }

    public int getNumTimesInState() {
        return numTimesInState;
    }

    public void setNumTimesInState(int numTimesInState) {
        this.numTimesInState = numTimesInState;
    }

    public long getTotalTimeInStateMs() {
        return totalTimeInStateMs;
    }

    public void setTotalTimeInStateMs(long totalTimeInStateMs) {
        this.totalTimeInStateMs = totalTimeInStateMs;
    }





}
