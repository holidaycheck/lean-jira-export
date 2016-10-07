package lean.jira.export.jiraImport;

import lean.jira.export.model.LeanIssue;
import lean.jira.export.model.LeanIssueState;
import com.atlassian.jira.rest.client.api.domain.ChangelogGroup;
import com.atlassian.jira.rest.client.api.domain.ChangelogItem;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.Status;
import junit.framework.Assert;
import org.joda.time.DateTime;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;

/**
 * Tests the main conversion logic of JIRA to this tool's domain model found in the LeanIssueBuilder
 */
public class LeanIssueBuilderTest {

    // creation date is far in the past => this moves all transitions to the past allowing realtime calculations (now - transitionDate) to be positive!
    private final DateTime creationDate = new DateTime().minusDays(10);
    private long oneDayMs = creationDate.plusDays(1).getMillis() - creationDate.getMillis();


    @Test
    public void testBuildWithKey() {
        Issue mockIssue = mockIssue();
        LeanIssue resultingIssue = new LeanIssueBuilder().addBasicJiraIssue(mockIssue).getLeanIssue();
        Assert.assertEquals("key", "EXP-4711", resultingIssue.getKey());
    }

    public Issue mockIssue() {
        Issue mockIssue = Mockito.mock(Issue.class);
        Mockito.when(mockIssue.getKey()).thenReturn("EXP-4711");
        Mockito.when(mockIssue.getSummary()).thenReturn("MySummary");
        Mockito.when(mockIssue.getDescription()).thenReturn("MyDescription");

        Status mockStatus = Mockito.mock(Status.class);
        Mockito.when(mockStatus.getName()).thenReturn("Open");
        Mockito.when(mockIssue.getStatus()).thenReturn(mockStatus);

        Mockito.when(mockIssue.getCreationDate()).thenReturn(creationDate);


        ChangelogGroup mockChangelogGroup = Mockito.mock(ChangelogGroup.class);
        Mockito.when(mockChangelogGroup.getCreated()).thenReturn(creationDate.plusDays(1));

        ChangelogItem mockChangelogItem = Mockito.mock(ChangelogItem.class);
        Mockito.when(mockChangelogGroup.getItems()).thenReturn(Arrays.asList(new ChangelogItem[] {mockChangelogItem}));

        Mockito.when(mockChangelogItem.getField()).thenReturn("status");
        Mockito.when(mockChangelogItem.getFromString()).thenReturn("Open");
        Mockito.when(mockChangelogItem.getToString()).thenReturn("STATUS2");
        Mockito.when(mockIssue.getChangelog()).thenReturn(Arrays.asList(new ChangelogGroup[] {mockChangelogGroup}));

        return mockIssue;
    }

    @Test
    public void testBuildWithSummary() {
        Issue mockIssue = mockIssue();

        LeanIssue resultingIssue = new LeanIssueBuilder().addBasicJiraIssue(mockIssue).getLeanIssue();
        Assert.assertEquals("summary", "MySummary", resultingIssue.getSummary());
    }

    @Test
    public void testBuildWithDescription() {
        Issue mockIssue = mockIssue();

        LeanIssue resultingIssue = new LeanIssueBuilder().addBasicJiraIssue(mockIssue).getLeanIssue();
        Assert.assertEquals("description", "MyDescription", resultingIssue.getDescription());
    }

    @Test
    public void testBuildWithInitialStatus() {
        Issue mockIssue = mockIssue();


        LeanIssue resultingIssue = new LeanIssueBuilder().addBasicJiraIssue(mockIssue).getLeanIssue();
        Assert.assertEquals("currentStateName", "Open", resultingIssue.getCurrentStateName());
    }


    @Test
    public void testBuildWithCreationDate() {
        Issue mockIssue = mockIssue();
        LeanIssue resultingIssue = new LeanIssueBuilder().addBasicJiraIssue(mockIssue).getLeanIssue();
        Assert.assertEquals("creationDate", creationDate.toDate(), resultingIssue.getCreationDate());
    }

    @Test
    public void testAddOneTransition() {
        Issue mockIssue = mockIssue();
        LeanIssueBuilder.WorkflowTransition workflowTransition = new LeanIssueBuilder.WorkflowTransition(creationDate.plusDays(1).toDate(), "Open", "STATUS2");
        LeanIssue resultingIssue = new LeanIssueBuilder().addBasicJiraIssue(mockIssue).addTransition(workflowTransition).doCalculations().getLeanIssue();
        Assert.assertEquals("stateList", Arrays.asList(new String[] {"Open", "STATUS2"}) , resultingIssue.getStateNames());
        LeanIssueState state1 = resultingIssue.getState("Open");
        LeanIssueState state2 = resultingIssue.getState("STATUS2");
        Assert.assertEquals("name", "Open", state1.getName());
        Assert.assertEquals("firstEntryDate", creationDate.toDate(), state1.getFirstEntryDate());
        Assert.assertEquals("lastEntryDate", creationDate.toDate(), state1.getLastEntryDate());
        Assert.assertEquals("lastExitDate", creationDate.plusDays(1).toDate(), state1.getLastExitDate());
        Assert.assertEquals("numTimesInState", 1, state1.getNumTimesInState());
        Assert.assertEquals("totalTimeInStateMs", oneDayMs, state1.getTotalTimeInStateMs());

        Assert.assertEquals("name", "STATUS2", state2.getName());
        Assert.assertEquals("firstEntryDate", creationDate.plusDays(1).toDate(), state2.getFirstEntryDate());
        Assert.assertEquals("lastEntryDate", creationDate.plusDays(1).toDate(), state2.getLastEntryDate());
        Assert.assertEquals("lastExitDate", null, state2.getLastExitDate());
        Assert.assertEquals("numTimesInState", 0, state2.getNumTimesInState());
        Assert.assertTrue("totalTimeInStateMs > 0", state2.getTotalTimeInStateMs()  > 0); // date since last transition is earlier than now..

        Assert.assertEquals("leadTime", oneDayMs, resultingIssue.getLeadTimeMs());
        Assert.assertTrue("cycleTime >0 ! " + resultingIssue.getCycleTimeMs(), resultingIssue.getCycleTimeMs() > 0);
    }

    @Test
    public void testAddTwoTransitions() {
        Issue mockIssue = mockIssue();
        LeanIssueBuilder.WorkflowTransition workflowTransition1 = new LeanIssueBuilder.WorkflowTransition(creationDate.plusDays(1).toDate(), "Open", "STATUS2");
        LeanIssueBuilder.WorkflowTransition workflowTransition2 = new LeanIssueBuilder.WorkflowTransition(creationDate.plusDays(2).toDate(), "STATUS2", "STATUS3");


        LeanIssueBuilder builder= new LeanIssueBuilder().addBasicJiraIssue(mockIssue);
        builder.addTransition(workflowTransition1);
        builder.addTransition(workflowTransition2);
        builder.doCalculations();
        LeanIssue resultingIssue = builder.getLeanIssue();

        Assert.assertEquals("stateList", Arrays.asList(new String[] {"Open", "STATUS2", "STATUS3"}) , resultingIssue.getStateNames());
        LeanIssueState state1 = resultingIssue.getState("Open");
        LeanIssueState state2 = resultingIssue.getState("STATUS2");
        LeanIssueState state3 = resultingIssue.getState("STATUS3");

        Assert.assertEquals("name", "Open", state1.getName());
        Assert.assertEquals("firstEntryDate", creationDate.toDate(), state1.getFirstEntryDate());
        Assert.assertEquals("lastEntryDate", creationDate.toDate(), state1.getLastEntryDate());
        Assert.assertEquals("lastExitDate", creationDate.plusDays(1).toDate(), state1.getLastExitDate());
        Assert.assertEquals("numTimesInState", 1, state1.getNumTimesInState());
        Assert.assertEquals("totalTimeInStateMs", creationDate.plusDays(1).getMillis() - creationDate.getMillis(), state1.getTotalTimeInStateMs());

        Assert.assertEquals("name", "STATUS2", state2.getName());
        Assert.assertEquals("firstEntryDate", creationDate.plusDays(1).toDate(), state2.getFirstEntryDate());
        Assert.assertEquals("lastEntryDate", creationDate.plusDays(1).toDate(), state2.getLastEntryDate());
        Assert.assertEquals("lastExitDate", creationDate.plusDays(2).toDate(), state2.getLastExitDate());
        Assert.assertEquals("numTimesInState", 1, state2.getNumTimesInState());
        Assert.assertEquals("totalTimeInStateMs", creationDate.plusDays(2).getMillis() - creationDate.plusDays(1).getMillis(), state2.getTotalTimeInStateMs());

        Assert.assertEquals("name", "STATUS3", state3.getName());
        Assert.assertEquals("firstEntryDate", creationDate.plusDays(2).toDate(), state3.getFirstEntryDate());
        Assert.assertEquals("lastEntryDate", creationDate.plusDays(2).toDate(), state3.getLastEntryDate());
        Assert.assertEquals("lastExitDate", null, state3.getLastExitDate());
        Assert.assertEquals("numTimesInState", 0, state3.getNumTimesInState());
        Assert.assertTrue("totalTimeInStateMs>0", state3.getTotalTimeInStateMs()>0);

        Assert.assertEquals("leadTime", oneDayMs, resultingIssue.getLeadTimeMs());
        Assert.assertTrue("cycleTime >0 ! " + resultingIssue.getCycleTimeMs(), resultingIssue.getCycleTimeMs() > 0);
    }


    @Test
    public void testAddBackAndForthTransitions() {
        Issue mockIssue = mockIssue();
        LeanIssueBuilder.WorkflowTransition workflowTransition1 = new LeanIssueBuilder.WorkflowTransition(creationDate.plusDays(1).toDate(), "Open", "STATUS2");
        LeanIssueBuilder.WorkflowTransition workflowTransition2 = new LeanIssueBuilder.WorkflowTransition(creationDate.plusDays(2).toDate(), "STATUS2", "Open");
        LeanIssueBuilder.WorkflowTransition workflowTransition3 = new LeanIssueBuilder.WorkflowTransition(creationDate.plusDays(3).toDate(), "Open", "STATUS2");


        LeanIssueBuilder builder= new LeanIssueBuilder().addBasicJiraIssue(mockIssue);
        builder.addTransition(workflowTransition1);
        builder.addTransition(workflowTransition2);
        builder.addTransition(workflowTransition3);
        builder.doCalculations();
        LeanIssue resultingIssue = builder.getLeanIssue();

        Assert.assertEquals("stateList", Arrays.asList(new String[] {"Open", "STATUS2"}) , resultingIssue.getStateNames());
        LeanIssueState state1 = resultingIssue.getState("Open");
        LeanIssueState state2 = resultingIssue.getState("STATUS2");


        Assert.assertEquals("name", "Open", state1.getName());
        Assert.assertEquals("firstEntryDate", creationDate.toDate(), state1.getFirstEntryDate());
        Assert.assertEquals("lastEntryDate", creationDate.plusDays(2).toDate(), state1.getLastEntryDate());
        Assert.assertEquals("lastExitDate", creationDate.plusDays(3).toDate(), state1.getLastExitDate());
        Assert.assertEquals("numTimesInState", 2, state1.getNumTimesInState());
        Assert.assertEquals("totalTimeInStateMs", creationDate.plusDays(2).getMillis() - creationDate.getMillis(), state1.getTotalTimeInStateMs());

        Assert.assertEquals("name", "STATUS2", state2.getName());
        Assert.assertEquals("firstEntryDate", creationDate.plusDays(1).toDate(), state2.getFirstEntryDate());
        Assert.assertEquals("lastEntryDate", creationDate.plusDays(3).toDate(), state2.getLastEntryDate());
        Assert.assertEquals("lastExitDate", creationDate.plusDays(2).toDate(), state2.getLastExitDate());
        Assert.assertEquals("numTimesInState", 1, state2.getNumTimesInState());
        Assert.assertTrue("totalTimeInStateMs > 1d  (still running)",  state2.getTotalTimeInStateMs() > creationDate.plusDays(1).getMillis() - creationDate.getMillis());

        Assert.assertEquals("leadTime", 2L * oneDayMs, resultingIssue.getLeadTimeMs());
        Assert.assertTrue("cycleTime >0 ! " + resultingIssue.getCycleTimeMs(), resultingIssue.getCycleTimeMs() > 0);

    }

    @Test
    public void testSelfTransitions() {
        Issue mockIssue = mockIssue();
        LeanIssueBuilder.WorkflowTransition workflowTransition1 = new LeanIssueBuilder.WorkflowTransition(creationDate.plusDays(1).toDate(), "Open", "STATUS2");
        LeanIssueBuilder.WorkflowTransition workflowTransition2 = new LeanIssueBuilder.WorkflowTransition(creationDate.plusDays(2).toDate(), "STATUS2", "STATUS2");
        LeanIssueBuilder.WorkflowTransition workflowTransition3 = new LeanIssueBuilder.WorkflowTransition(creationDate.plusDays(3).toDate(), "STATUS2", "STATUS3");


        LeanIssueBuilder builder= new LeanIssueBuilder().addBasicJiraIssue(mockIssue);
        builder.addTransition(workflowTransition1);
        builder.addTransition(workflowTransition2);
        builder.addTransition(workflowTransition3);
        builder.doCalculations();
        LeanIssue resultingIssue = builder.getLeanIssue();

        Assert.assertEquals("stateList", Arrays.asList(new String[] {"Open", "STATUS2", "STATUS3"}) , resultingIssue.getStateNames());
        LeanIssueState state1 = resultingIssue.getState("Open");
        LeanIssueState state2 = resultingIssue.getState("STATUS2");
        LeanIssueState state3 = resultingIssue.getState("STATUS3");


        Assert.assertEquals("name", "Open", state1.getName());
        Assert.assertEquals("firstEntryDate", creationDate.toDate(), state1.getFirstEntryDate());
        Assert.assertEquals("lastEntryDate", creationDate.toDate(), state1.getLastEntryDate());
        Assert.assertEquals("lastExitDate", creationDate.plusDays(1).toDate(), state1.getLastExitDate());
        Assert.assertEquals("numTimesInState", 1, state1.getNumTimesInState());
        Assert.assertEquals("totalTimeInStateMs", creationDate.plusDays(1).getMillis() - creationDate.getMillis(), state1.getTotalTimeInStateMs());

        Assert.assertEquals("name", "STATUS2", state2.getName());
        Assert.assertEquals("firstEntryDate", creationDate.plusDays(1).toDate(), state2.getFirstEntryDate());
        Assert.assertEquals("lastEntryDate", creationDate.plusDays(2).toDate(), state2.getLastEntryDate());
        Assert.assertEquals("lastExitDate", creationDate.plusDays(3).toDate(), state2.getLastExitDate());
        Assert.assertEquals("numTimesInState", 2, state2.getNumTimesInState());
        Assert.assertEquals("totalTimeInStateMs", creationDate.plusDays(2).getMillis() - creationDate.getMillis(), state2.getTotalTimeInStateMs());

        Assert.assertEquals("name", "STATUS3", state3.getName());
        Assert.assertEquals("firstEntryDate", creationDate.plusDays(3).toDate(), state3.getFirstEntryDate());
        Assert.assertEquals("lastEntryDate", creationDate.plusDays(3).toDate(), state3.getLastEntryDate());
        Assert.assertEquals("lastExitDate", null, state3.getLastExitDate());
        Assert.assertEquals("numTimesInState", 0, state3.getNumTimesInState());
        Assert.assertTrue("totalTimeInStateMs > 0", state3.getTotalTimeInStateMs() > 0);

    }


    @Test
    public void testBuild() {
        Issue mockIssue = mockIssue();
        LeanIssue resultingIssue = new LeanIssueBuilder().build(mockIssue);
        Assert.assertEquals("stateList", Arrays.asList(new String[] {"Open", "STATUS2"}) , resultingIssue.getStateNames());
        LeanIssueState state1 = resultingIssue.getState("Open");
        LeanIssueState state2 = resultingIssue.getState("STATUS2");
        Assert.assertEquals("name", "Open", state1.getName());
        Assert.assertEquals("firstEntryDate", creationDate.toDate(), state1.getFirstEntryDate());
        Assert.assertEquals("lastEntryDate", creationDate.toDate(), state1.getLastEntryDate());
        Assert.assertEquals("lastExitDate", creationDate.plusDays(1).toDate(), state1.getLastExitDate());
        Assert.assertEquals("numTimesInState", 1, state1.getNumTimesInState());
        Assert.assertEquals("totalTimeInStateMs", creationDate.plusDays(1).getMillis() - creationDate.getMillis(), state1.getTotalTimeInStateMs());

        Assert.assertEquals("name", "STATUS2", state2.getName());
        Assert.assertEquals("firstEntryDate", creationDate.plusDays(1).toDate(), state2.getFirstEntryDate());
        Assert.assertEquals("lastEntryDate", creationDate.plusDays(1).toDate(), state2.getLastEntryDate());
        Assert.assertEquals("lastExitDate", null, state2.getLastExitDate());
        Assert.assertEquals("numTimesInState", 0, state2.getNumTimesInState());
        Assert.assertTrue("totalTimeInStateMs", state2.getTotalTimeInStateMs() > 0);
    }

    @Test
    public void testIllegalTransitions() {
        Issue mockIssue = mockIssue();
        LeanIssueBuilder.WorkflowTransition workflowTransition1 = new LeanIssueBuilder.WorkflowTransition(creationDate.toDate(), "Open", "STATUS2");

        LeanIssueBuilder builder= new LeanIssueBuilder().addBasicJiraIssue(mockIssue);
        try {
            builder.addTransition(workflowTransition1);
            Assert.fail("Expected a RuntimeException since the transition was at the same date as the creationDate => 0 time in Open");
        } catch (RuntimeException e) {
            // this is expected. do nothing
        }

    }


}
