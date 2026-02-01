import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class SchedulerTest {
    @Test
    void putGetFireEvent() {
        Scheduler scheduler = new Scheduler();
        FireEvent event = new FireEvent("12:00", 1, "Fire", "High");

        scheduler.putEvent(event);
        FireEvent retrieved = scheduler.getTask();

        assertEquals(1, retrieved.getZoneId());
    }
}