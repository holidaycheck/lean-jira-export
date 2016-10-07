package lean.jira.export.model;

import java.util.*;


public class LeanIssue {

    private String key;
    private String description;
    private String summary;
    private String currentStateName;
    private Date creationDate;
    private boolean resolved;
    private boolean isWIP;
    private boolean isInQueue;
    private long leadTimeMs;
    private long cycleTimeMs;
    private Date cycleEntryDate;



    // workflow
    private Map<String, LeanIssueState> leanIssueStates = new LinkedHashMap<String, LeanIssueState>();

    public List<String> getStateNames() {
        List<String> stateNames = new ArrayList<String>();
        stateNames.addAll(leanIssueStates.keySet());
        return stateNames;
    }

    public void addState(LeanIssueState state) {
        this.leanIssueStates.put(state.getName(), state);
    }

    public LeanIssueState getState(String name) {
        return leanIssueStates.get(name);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getCurrentStateName() {
        return currentStateName;
    }

    public void setCurrentStateName(String currentStateName) {
        this.currentStateName = currentStateName;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    public long getCycleTimeMs() {
        return cycleTimeMs;
    }

    public void setCycleTimeMs(long cycleTimeMs) {
        this.cycleTimeMs = cycleTimeMs;
    }

    public long getLeadTimeMs() {
        return leadTimeMs;
    }

    public void setLeadTimeMs(long leadTimeMs) {
        this.leadTimeMs = leadTimeMs;
    }

    public Date getCycleEntryDate() {
        return cycleEntryDate;
    }

    public void setCycleEntryDate(Date cycleEntryDate) {
        this.cycleEntryDate = cycleEntryDate;
    }

    public boolean isWIP() {
        return isWIP;
    }

    public void setWIP(boolean WIP) {
        isWIP = WIP;
    }

    public boolean isInQueue() {
        return isInQueue;
    }

    public void setInQueue(boolean inQueue) {
        isInQueue = inQueue;
    }


}
