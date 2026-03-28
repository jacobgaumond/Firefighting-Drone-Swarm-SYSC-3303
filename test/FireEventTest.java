import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FireEventTest {

    @Test
    void testParseFromCsv() {
        String line = "14:03:15,3,FIRE_DETECTED,High";
        FireEvent event = FireEvent.parseFromCsv(line);

        assertNotNull(event);
        assertEquals("14:03:15", event.getTime());
        assertEquals(3, event.getZoneId());
        assertEquals("FIRE_DETECTED", event.getEventType());
        assertEquals("High", event.getSeverity());
        assertEquals("", event.getFaultType());
    }
}