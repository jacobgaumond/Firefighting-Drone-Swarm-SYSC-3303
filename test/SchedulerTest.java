import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class SchedulerTest {

    private Scheduler scheduler;

    @BeforeEach
    void setup() {
        scheduler = new Scheduler();
    }

    @AfterEach
    void breakdown() {
        scheduler.closeBox();
    }
    // ==================== registerDrone ====================

    @Test
    void testRegisterDroneAddsToRegistry() {
        scheduler.registerDrone(1);
        assertTrue(scheduler.getDroneRegistry().containsKey(1));
    }

    @Test
    void testRegisterMultipleDrones() {
        scheduler.registerDrone(1);
        scheduler.registerDrone(2);
        assertEquals(2, scheduler.getDroneRegistry().size());
    }

    // ==================== processFireEvent ====================

    @Test
    void testFireEventAddedToTaskQueue() {
        FireEvent fire = new FireEvent("14:03:15", 3, "FIRE_DETECTED", "High", 250, 1050);
        Message msg = new Message("FireIncidentSubsystem", "Scheduler", fire.serialize(), Message.MessageType.FireEvent);

        scheduler.processFireEvent(msg);

        assertEquals(1, scheduler.getTaskQueue().size());
    }

    @Test
    void testMultipleFireEventsQueueInOrder() {
        FireEvent fire1 = new FireEvent("14:03:15", 3, "FIRE_DETECTED", "High",     250,  1050);
        FireEvent fire2 = new FireEvent("14:10:00", 5, "FIRE_DETECTED", "Moderate", 1650, 700);

        scheduler.processFireEvent(new Message("FireIncidentSubsystem", "Scheduler", fire1.serialize(), Message.MessageType.FireEvent));
        scheduler.processFireEvent(new Message("FireIncidentSubsystem", "Scheduler", fire2.serialize(), Message.MessageType.FireEvent));

        assertEquals(2, scheduler.getTaskQueue().size());
        assertEquals(3, scheduler.getTaskQueue().peek().getZoneId()); // fire1 is first
    }

    @Test
    void testNonFireEventIgnoredByProcessFireEvent() {
        FireEvent fire = new FireEvent("14:03:15", 3, "FIRE_DETECTED", "High", 250, 1050);
        Message msg = new Message("DroneSubsystem", "Scheduler", fire.serialize(), Message.MessageType.DroneResponse);

        scheduler.processFireEvent(msg); // wrong type — should be ignored

        assertTrue(scheduler.getTaskQueue().isEmpty());
    }

    // ==================== tryAssignTask ====================

    @Test
    void testTryAssignTaskDoesNothingWithNoDrones() {
        FireEvent fire = new FireEvent("14:03:15", 3, "FIRE_DETECTED", "High", 250, 1050);
        scheduler.processFireEvent(new Message("FireIncidentSubsystem", "Scheduler", fire.serialize(), Message.MessageType.FireEvent));

        scheduler.tryAssignTask(); // no drones registered — task should stay queued

        assertEquals(1, scheduler.getTaskQueue().size());
    }

    @Test
    void testFireStaysQueuedWhenInsufficientFluid() {
        scheduler.registerDrone(1); // drone has 15 fluid, High severity needs 30

        FireEvent fire1 = new FireEvent("14:03:15", 3, "FIRE_DETECTED", "High", 250, 1050);
        scheduler.processFireEvent(new Message("FireIncidentSubsystem", "Scheduler", fire1.serialize(), Message.MessageType.FireEvent));

        // fire1 is assigned to drone but NOT dequeued because
        // drone's 15 fluid < 30 required for High severity
        assertEquals(1, scheduler.getTaskQueue().size());
        assertEquals(3, scheduler.getTaskQueue().peek().getZoneId());
    }

    @Test
    void testLowSeverityFireDequeued() {
        scheduler.registerDrone(1); // drone has 15 fluid, Low severity needs 10

        FireEvent fire1 = new FireEvent("14:03:15", 3, "FIRE_DETECTED", "Low", 0, 0);
        scheduler.processFireEvent(new Message("FireIncidentSubsystem", "Scheduler", fire1.serialize(), Message.MessageType.FireEvent));

        // drone's 15 fluid >= 10 required for Low severity → fire is dequeued
        assertEquals(0, scheduler.getTaskQueue().size());
    }

    // ==================== getTaskQueue / getDroneRegistry / getActiveFires ====================

    @Test
    void testTaskQueueStartsEmpty() {
        assertTrue(scheduler.getTaskQueue().isEmpty());
    }

    @Test
    void testDroneRegistryStartsEmpty() {
        assertTrue(scheduler.getDroneRegistry().isEmpty());
    }

    @Test
    void testActiveFiresStartsEmpty() {
        assertTrue(scheduler.getActiveFires().isEmpty());
    }

    // ==================== calculateGuiDroneTravelTime ====================

    @Test
    void testTravelTimeIsZeroForSamePosition() {
        assertEquals(0, scheduler.calculateGuiDroneTravelTime(0, 0, 0, 0));
    }

    @Test
    void testTravelTimeIsPositiveForDifferentPositions() {
        long time = scheduler.calculateGuiDroneTravelTime(0, 0, 100, 100);
        assertTrue(time > 0);
    }

    @Test
    void testTravelTimeIsSymmetric() {
        long there = scheduler.calculateGuiDroneTravelTime(0,   0,   100, 100);
        long back  = scheduler.calculateGuiDroneTravelTime(100, 100, 0,   0);
        assertEquals(there, back);
    }
}