package lean.jira.export.jiraImport;

import lean.jira.export.model.LeanIssue;
import lean.jira.export.model.LeanIssueState;
import com.atlassian.jira.rest.client.api.domain.ChangelogGroup;
import com.atlassian.jira.rest.client.api.domain.ChangelogItem;
import com.atlassian.jira.rest.client.api.domain.Issue;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class LeanIssueBuilder {

    private static Logger LOG = LoggerFactory.getLogger(LeanIssueBuilder.class);


    private LeanIssue leanIssue = new LeanIssue();
    private DateTime now = new DateTime();


    /**
     * The central entry point to this builder
     * @param jiraIssue a jiraIssue that was enriched with Expandos.CHANGELOG (i.e. including all workflow transitions)
     * @return the completely built LeanIssue including all LeanIssueStates
     */
    public LeanIssue build(Issue jiraIssue){
        addBasicJiraIssue(jiraIssue);
        addAllTransitionsFor(jiraIssue);
        return this.leanIssue;
    }


    /**
     * @return The object under constuction in it's current state (initially empty)
     */
    LeanIssue getLeanIssue() {
        return this.leanIssue;
    }

    /**
     @param jiraIssue the issue object from the JIRA rest api
     @return this.leanIssue in a state without transitions but all basic fields covered
     */
    LeanIssueBuilder addBasicJiraIssue(Issue jiraIssue) {
        LOG.debug("Issue:" + jiraIssue.getKey());
        this.leanIssue.setKey(jiraIssue.getKey());
        this.leanIssue.setSummary(jiraIssue.getSummary());
        this.leanIssue.setDescription(jiraIssue.getDescription());
        this.leanIssue.setCurrentStateName(jiraIssue.getStatus().getName());
        this.leanIssue.setCreationDate(jiraIssue.getCreationDate().toDate());
        this.leanIssue.setResolved(jiraIssue.getResolution() != null);
        return this;
    }

    LeanIssueBuilder addAllTransitionsFor(Issue jiraIssue) {


        Iterable<ChangelogGroup> changelog = jiraIssue.getChangelog();
        assert changelog != null;
        for (ChangelogGroup changeLogGroup : changelog) {
            for(ChangelogItem item : changeLogGroup.getItems()) {
                if ("status".equals(item.getField())) {
                    addTransition(new WorkflowTransition(changeLogGroup.getCreated().toDate(), item.getFromString(), item.getToString()));
                }
            }
        }
        doCalculations();
        return this;
    }

    LeanIssueBuilder doCalculations() {
        handleNeverTransitioned();
        handleLastStateStillActive();
        calculateLeadTime();
        calculateCycleTime();
        calculateIsWIP();
        calculateIsInQueue();
        calculateCycleEntryDate();
        return this;
    }

    private LeanIssueBuilder calculateIsInQueue() {
        leanIssue.setInQueue(inQueue());
        return this;
    }

    private LeanIssueBuilder calculateCycleEntryDate() {
        if (inQueue()) {
            return null;
        } else {
            // minimally 2 states exist (i.e. the first transition has happened)
            String firstCycleStateName = leanIssue.getStateNames().get(1);
            LeanIssueState firstCycleState = leanIssue.getState(firstCycleStateName);
            leanIssue.setCycleEntryDate(firstCycleState.getFirstEntryDate());
        }
        return this;
    }

    private boolean inQueue() {
        return leanIssue.getStateNames().size() == 1;
    }

    private void calculateCycleTime() {
        // omit index 0 for cycle time (i.e. sum up all time spent in states except for the first one
        long cycleTime = 0;
        for (int i = 1; i<leanIssue.getStateNames().size(); i++) {
            String stateName = leanIssue.getStateNames().get(i);
            LeanIssueState state = leanIssue.getState(stateName);
            cycleTime += state.getTotalTimeInStateMs();
        }
        leanIssue.setCycleTimeMs(cycleTime);
    }

    private LeanIssueBuilder calculateLeadTime() {
        this.leanIssue.setLeadTimeMs(getFirstState().getTotalTimeInStateMs());
        return this;
    }

    private LeanIssueState getFirstState() {
        String firstStateName = this.leanIssue.getStateNames().get(0);
        return this.leanIssue.getState(firstStateName);
    }

    private LeanIssueBuilder handleLastStateStillActive() {
            int lastStateIndex = this.leanIssue.getStateNames().size() - 1;
            LeanIssueState lastState = this.leanIssue.getState(leanIssue.getStateNames().get(lastStateIndex));
            if (lastState == null) {
                throw new RuntimeException("no state object found for " + this.leanIssue.getCurrentStateName());
            }

            if (leanIssue.isResolved()) {
                lastState.setTotalTimeInStateMs(0); // HERE is an important assumption this tool makes: the last state is the one where the issue is resolved.
                // note: this does not handle the Won't Fix resolution etc..
            } else {
                // the benefit of doing it this way is that we get some kind of real-time cycleTime calculation (i.e. calculating cycle time even before the issue is closed!)
                lastState.setTotalTimeInStateMs(lastState.getTotalTimeInStateMs() + (now.toDate().getTime() - lastState.getLastEntryDate().getTime()));
            }
            return this;
    }

    private LeanIssueBuilder handleNeverTransitioned() {

        if (this.leanIssue.getStateNames().size() == 0) {
            LOG.debug("Adding initial State " + this.leanIssue.getCurrentStateName());
            LeanIssueState initialState = new LeanIssueState();
            initialState.setFirstEntryDate(this.leanIssue.getCreationDate());
            initialState.setLastEntryDate(this.leanIssue.getCreationDate());
            initialState.setName(leanIssue.getCurrentStateName());
            initialState.setNumTimesInState(1);
            this.leanIssue.addState(initialState);
        }

        return this;
    }

    private LeanIssueBuilder calculateIsWIP() {
        leanIssue.setWIP(leanIssue.getStateNames().size() > 1 && ! leanIssue.isResolved());
        return this;
    }


    // we are expecting that this is called in chronological and chained order!
    LeanIssueBuilder addTransition(WorkflowTransition transition) {
        LOG.debug("adding transition " + transition);

        LeanIssueState sourceState = this.leanIssue.getState(transition.source);
        LeanIssueState destinationState = this.leanIssue.getState(transition.destination);

        if (sourceState == null) {
            sourceState = new LeanIssueState();
            sourceState.setFirstEntryDate(this.leanIssue.getCreationDate());
            sourceState.setLastEntryDate(this.leanIssue.getCreationDate());
            sourceState.setName(transition.source);
            sourceState.setNumTimesInState(0);
        }

        // now leave the source state
        sourceState.setLastExitDate(transition.date);
        long timeSpentInSourceState =  sourceState.getLastExitDate().getTime() - sourceState.getLastEntryDate().getTime();
        if (timeSpentInSourceState <= 0) {
            throw new RuntimeException("Negative or 0 duration. Transition events must be in chronological and chained order!");
        }
        // keep track of intervals spent in this state (increment a counter and the total time in state)
        sourceState.setNumTimesInState(sourceState.getNumTimesInState() + 1);
        sourceState.setTotalTimeInStateMs(sourceState.getTotalTimeInStateMs() + timeSpentInSourceState);


        // enter new state
        if (destinationState == null) {
            destinationState = new LeanIssueState();
            destinationState.setName(transition.destination);
            destinationState.setFirstEntryDate(transition.date);
            destinationState.setLastEntryDate(transition.date);
        } else {
            // re-entering state (this actually happens a lot)
            destinationState.setLastEntryDate(transition.date);
        }

        this.leanIssue.addState(sourceState);
        this.leanIssue.addState(destinationState);

        return this;
    }


    public static class WorkflowTransition  {

        public WorkflowTransition(Date date, String source, String destination)  {
            this.date = date;
            this.source = source;
            this.destination = destination;
        }

        public Date date;
        public String source;
        public String destination;

        public String toString() {
            StringBuilder b = new StringBuilder();

            b.append("date=");
            b.append(date.toString());
            b.append(",");

            b.append("source=");
            b.append(source);
            b.append(",");

            b.append("destination=");
            b.append(destination);

            return b.toString();
      }

    }

}
