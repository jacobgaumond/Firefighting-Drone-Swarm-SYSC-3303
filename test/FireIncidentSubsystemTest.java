import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


class FireIncidentSubsystemTest {

    String inputFileName = "src/data/Sample_event_file.csv";



    MessageBox fireIncidentBox = new MessageBox();
    MessageBox schedulerBox = new MessageBox();



    private void startFireSubsystem() {
        FireIncidentSubsystem fireSys =
                new FireIncidentSubsystem(fireIncidentBox, schedulerBox, inputFileName);
        new Thread(fireSys, "FireIncidentSubsystemThread").start();
    }

    @Test
    void toSchedulerMessageBox() throws InterruptedException {
        startFireSubsystem();
        Thread.sleep(500);

        assertTrue(schedulerBox.isFull());
    }


    //setup zones is just for testing
    @BeforeAll
    static void setupZones(){
        ZoneMap.addZone(3, new ZoneMap.Zone(0,0,700,600));
        ZoneMap.addZone(7, new ZoneMap.Zone(0,0,700,600));
    }

    @Test
    void testParseFireEvent(){
        FireIncidentSubsystem subsystem =  new FireIncidentSubsystem(null, null);

        String line = "14:03:15,3,FIRE_DETECTED,High";
        FireTask task = subsystem.parseFireEvent(line);

        assertNotNull(task);
        assertEquals("14:03:15", task.getTime());
        assertEquals(3, task.getZoneId());
        assertEquals("FIRE_DETECTED", task.getEventType());
        assertEquals("High", task.getSeverity());

        //check target coordinates
        assertEquals(ZoneMap.getX(3), task.getTargetX());
        assertEquals(ZoneMap.getY(3), task.getTargetY());
    }
}
