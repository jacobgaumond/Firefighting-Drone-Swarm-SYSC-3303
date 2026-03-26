
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FireIncidentSubsystemTest {

    String inputFileName = "src/data/Sample_event_file.csv";
    private UDPMessageBox schedulerBox;
    private FireIncidentSubsystem fireSys;

    @BeforeEach
    void setup() {
        schedulerBox = new UDPMessageBox(UDPMessageBox.Subsystem.SCHEDULER);
        fireSys = new FireIncidentSubsystem();
    }

    @BeforeAll
    static void setupZones() {
        ZoneMap.loadZones("src/data/Sample_zone_file.csv");
    }

    @AfterEach
    void teardown() {
        if (schedulerBox != null) {
            schedulerBox.closeBox();
        }
        if (fireSys != null) {
            fireSys.closeBox();
        }
    }

    @Test
    void testParseFireEvent() {

        String line = "14:03:15,3,FIRE_DETECTED,High";
        FireEvent event = fireSys.parseFireEvent(line);

        assertNotNull(event);
        assertEquals("14:03:15", event.getTime());
        assertEquals(3, event.getZoneId());
        assertEquals("FIRE_DETECTED", event.getEventType());
        assertEquals("High", event.getSeverity());

        //check target coordinates
        assertEquals(ZoneMap.getX(3), event.getTargetX());
        assertEquals(ZoneMap.getY(3), event.getTargetY());
    }

    @Test
    void toSchedulerMessageBox() throws InterruptedException {
        FireEvent fireEvent = fireSys.parseFireEvent("14:03:15,3,FIRE_DETECTED,High");
        Message message = new Message(
                "Scheduler",
                "FireIncidentSubsystem",
                fireEvent.serialize(),
                Message.MessageType.FireEvent
        );
        fireSys.sendMessage(message);
        Thread.sleep(200);
        Message received = schedulerBox.getMessage();
        assertNotNull(received);
        assertEquals(Message.MessageType.FireEvent, received.getMessageType());
    }

}
