import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FireIncidentSystemTest {
    @Test
    void run() {
        Scheduler scheduler = new Scheduler();
        Thread fireSys = new Thread(new FireIncidentSystem(scheduler, "/src/data/Sample_event_file.csv"), "FireSubThread");

        fireSys.start();

        assertEquals(scheduler.getTask().toString(), "[14:03:15] Zone: 3 | Type: FIRE_DETECTED | Severity: High | Status: PENDING");
    }
}