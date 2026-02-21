import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Queue;

public class SchedulerUnitTest {

    private MessageBox schedulerBox;
    private MessageBox fireIncidentBox;
    private MessageBox droneBox;
    private Scheduler scheduler;

    @BeforeEach
    void setup() throws InterruptedException {
        schedulerBox = new MessageBox();
        fireIncidentBox = new MessageBox();
        droneBox = new MessageBox();
        scheduler = new Scheduler(schedulerBox, fireIncidentBox, droneBox);
        new Thread(scheduler, "SchedulerThread").start();

        // Register a drone
        Message registerMessage = new Message(
                "Scheduler",
                "DroneSubsystem",
                "1",
                Message.MessageType.DroneRegistration
        );
        schedulerBox.putMessage(registerMessage);
        Thread.sleep(200); // wait for registration
    }

    @Test
    void testFireEventQueued() throws InterruptedException {
        // Send a fire event
        FireEvent fireEvent = new FireEvent("14:03:15", 3, "FIRE_DETECTED", "High", 250, 1050);
        Message fireMessage = new Message("Scheduler", "FireIncidentSubsystem", fireEvent.serialize(), Message.MessageType.FireEvent);
        schedulerBox.putMessage(fireMessage);
        Thread.sleep(300);

        // Drone should have received the task
        assertTrue(droneBox.isFull(), "Drone should have received task");
    }

    @Test
    void testSecondFireQueuedWhenDroneBusy() throws InterruptedException {
        // Send first fire event
        FireEvent fireEvent1 = new FireEvent("14:03:15", 3, "FIRE_DETECTED", "High", 250, 1050);
        Message fireMessage1 = new Message("Scheduler", "FireIncidentSubsystem", fireEvent1.serialize(), Message.MessageType.FireEvent);
        schedulerBox.putMessage(fireMessage1);
        Thread.sleep(300);

        // Drain the drone box so drone is marked busy
        droneBox.getMessage();

        // Send second fire event
        FireEvent fireEvent2 = new FireEvent("14:10:00", 5, "FIRE_DETECTED", "Moderate", 1650, 700);
        Message fireMessage2 = new Message("Scheduler", "FireIncidentSubsystem", fireEvent2.serialize(), Message.MessageType.FireEvent);
        schedulerBox.putMessage(fireMessage2);
        Thread.sleep(300);

        // Second task should be queued
        assertEquals(1, scheduler.getTaskQueue().size(), "Second task should be queued");
    }

    @Test
    void testDroneRegistered() throws InterruptedException {
        // Drone was registered in setup
        assertFalse(scheduler.getDroneRegistry().isEmpty(), "Drone registry should not be empty");
        assertTrue(scheduler.getDroneRegistry().containsKey(1), "Drone 1 should be registered");
    }

    @Test
    void testActiveFireTracked() throws InterruptedException {
        FireEvent fireEvent = new FireEvent("14:03:15", 3, "FIRE_DETECTED", "High", 250, 1050);
        Message fireMessage = new Message("Scheduler", "FireIncidentSubsystem", fireEvent.serialize(), Message.MessageType.FireEvent);
        schedulerBox.putMessage(fireMessage);
        Thread.sleep(300);

        assertFalse(scheduler.getActiveFires().isEmpty(), "Active fires should be tracked");
        assertTrue(scheduler.getActiveFires().containsKey(3), "Zone 3 should be in active fires");
    }
}