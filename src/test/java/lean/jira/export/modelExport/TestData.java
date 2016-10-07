package lean.jira.export.modelExport;

import lean.jira.export.model.LeanIssue;
import lean.jira.export.model.LeanIssueState;
import org.joda.time.DateTime;

import java.util.Arrays;
import java.util.List;

/**
 * Created by pmarshall on 04/10/16.
 */
public class TestData {
    public DateTime day0;
    public DateTime day1;
    public DateTime day2;
    public DateTime day3;
    public DateTime day4;
    public LeanIssueState state1;
    public LeanIssueState state2;
    public LeanIssueState state3;
    public LeanIssue issue1;
    public LeanIssue issue2;
    public List<LeanIssue> issueList;
    public long day21Milis;
    public long day31Milis;

    public TestData() {
        day0 = new DateTime();
        day1 = day0.plusDays(1);
        day2 = day1.plusDays(1);
        day3 = day2.plusDays(1);
        day4 = day3.plusDays(1);


        day21Milis = day2.getMillis() - day1.getMillis();
        day31Milis = day3.getMillis() - day1.getMillis();


        state1 = new LeanIssueState();
        state1.setFirstEntryDate(day1.toDate());
        state1.setLastEntryDate(day1.toDate());
        state1.setLastExitDate(day2.toDate());
        state1.setName("STATE1");
        state1.setNumTimesInState(1);
        state1.setTotalTimeInStateMs(day21Milis);


        state2 = new LeanIssueState();
        state2.setFirstEntryDate(day1.toDate());
        state2.setLastEntryDate(day1.toDate());
        state2.setLastExitDate(day2.toDate());
        state2.setName("STATE2");
        state2.setNumTimesInState(1);
        state2.setTotalTimeInStateMs(day21Milis);

        state3 = new LeanIssueState();
        state3.setFirstEntryDate(day1.toDate());
        state3.setLastEntryDate(day1.toDate());
        state3.setLastExitDate(day3.toDate());
        state3.setName("STATE3");
        state3.setNumTimesInState(1);
        state3.setTotalTimeInStateMs(day31Milis);


        issue1 = new LeanIssue();
        issue1.setCreationDate(day0.toDate());
        issue1.setCurrentStateName("STATE2");
        issue1.setDescription("DESC1");
        issue1.setKey("EXP-4711");
        issue1.setSummary("SUMMARY1");
        issue1.addState(state1);
        issue1.addState(state2);
        issue1.setLeadTimeMs(day21Milis);
        issue1.setCycleTimeMs(day31Milis);
        issue1.setResolved(false);

        issue2 = new LeanIssue();
        issue2.setCreationDate(day0.toDate());
        issue2.setCurrentStateName("STATE3");
        issue2.setDescription("DESC2");
        issue2.setKey("EXP-4712");
        issue2.setSummary("SUMMARY2");
        issue2.addState(state2);
        issue2.addState(state3);
        issue2.setLeadTimeMs(day21Milis);
        issue2.setCycleTimeMs(day31Milis);
        issue2.setResolved(true);

        issueList = Arrays.asList(issue1, issue2);

    }

}
