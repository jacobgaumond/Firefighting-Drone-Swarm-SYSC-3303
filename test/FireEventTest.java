import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FireEventTest {
    private FireEvent testFire = new FireEvent("12:00", 1, "Fire", "High");

    @Test
    void fireHandled() {
        assertTrue(testFire.toString().contains("PENDING"));
        testFire.fireHandled(true);

        assertTrue(testFire.toString().contains("EXTINGUISHED"));
    }

    @Test
    void getZoneId() {
        assertEquals(1, testFire.getZoneId());
    }

    @Test
    void getTime() {
        assertEquals("12:00", testFire.getTime());
    }

    @Test
    void testToString() {
        assertEquals(testFire.toString(), "[12:00] Zone: 1 | Type: Fire | Severity: High | Status: PENDING");
    }
}