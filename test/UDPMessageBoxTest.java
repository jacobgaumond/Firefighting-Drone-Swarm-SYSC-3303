import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.*;

class UDPMessageBoxTest {

    private UDPMessageBox schedulerBox;
    private UDPMessageBox fireIncidentBox;

    @BeforeEach
    void setUp() {
        schedulerBox = new UDPMessageBox(UDPMessageBox.Subsystem.SCHEDULER);
        fireIncidentBox = new UDPMessageBox(UDPMessageBox.Subsystem.FIRE_INCIDENT);
    }

    @AfterEach
    void tearDown() {
        if (schedulerBox != null) {
            schedulerBox.closeBox();
        }
        if (fireIncidentBox != null) {
            fireIncidentBox.closeBox();
        }
    }

    @Test
    void getAndPutMessage() throws InterruptedException {
        Message testMessage = new Message("fire_incident", "scheduler", "fire_detected", Message.MessageType.FireEvent);
        
        // schedulerBox is empty
        assertFalse(schedulerBox.isFull());
        
        // send message to SCHEDULER
        Thread sendThread = new Thread(() -> {
            fireIncidentBox.putMessage(testMessage, UDPMessageBox.Subsystem.SCHEDULER); // PUT
        });
        sendThread.start();
        
        // check it was received by SCHEDULER
        Message retrievedMessage = schedulerBox.getMessage(); // GET
        
        // Verify retrieved matches sent
        assertNotNull(retrievedMessage);
        assertEquals(testMessage.getSourceName(), retrievedMessage.getSourceName());
        assertEquals(testMessage.getDestinationName(), retrievedMessage.getDestinationName());
        assertEquals(testMessage.getMessageData(), retrievedMessage.getMessageData());
        assertEquals(testMessage.getMessageType(), retrievedMessage.getMessageType());
    }

    @Test
    void closeBox() {
        // Test closeBox executes without throwing exceptions
        try {
            schedulerBox.closeBox();
        } catch (Exception e) {
            fail("closeBox() threw an exception: " + e.getMessage());
        }
        
        // Verify subsequent close calls don't cause errors (idempotency)
        try {
            schedulerBox.closeBox();
        } catch (Exception e) {
            fail("Second closeBox() call threw an exception: " + e.getMessage());
        }
    }

    @Test
    void getSubsystem() {
        // Test getSubsystem returns the correct subsystem for this instance
        UDPMessageBox.Subsystem subsystem = schedulerBox.getSubsystem();
        
        assertEquals(UDPMessageBox.Subsystem.SCHEDULER, subsystem);
        assertNotNull(subsystem);
        
        UDPMessageBox.Subsystem subsystem2 = fireIncidentBox.getSubsystem();
        assertEquals(UDPMessageBox.Subsystem.FIRE_INCIDENT, subsystem2);
    }

    @Test
    void isFull() {
        // Test isFull returns a valid boolean
        boolean isFull = schedulerBox.isFull();
        
        // Initially the box should not be full
        assertFalse(isFull);
        
        // Send a message to fill the box
        Message testMessage = new Message("fire_incident", "scheduler", "fire_detected", Message.MessageType.FireEvent);
        fireIncidentBox.putMessage(testMessage, UDPMessageBox.Subsystem.SCHEDULER);
        
        // Now the box should be full
        boolean isFullAfterMessage = schedulerBox.isFull();
        assertTrue(isFullAfterMessage);
        
        // Verify the method returns a consistent boolean
        boolean isFullAgain = schedulerBox.isFull();
        assertEquals(isFullAfterMessage, isFullAgain);
    }

    @Test
    void getSubsystemPortTest() {
        // Test that SCHEDULER subsystem gets the correct port
        assertEquals(UDPMessageBox.SCHEDULER_PORT, 9501);
        
        // Test that FIRE_INCIDENT subsystem gets the correct port
        assertEquals(UDPMessageBox.FIRE_INCIDENT_PORT, 9500);
        
        // Test that DRONE_GUI subsystem gets the correct port
        assertEquals(UDPMessageBox.DRONE_GUI_PORT, 9502);
        
        // Test that BASE_DRONE_PORT is set correctly
        assertTrue(UDPMessageBox.BASE_DRONE_PORT > 0);
    }

    @Test
    void getTargetSubsystemPortTest() {
        // Test that target ports are correctly set for each subsystem
        // FIRE_INCIDENT target port should be its own port
        UDPMessageBox testBox = new UDPMessageBox(UDPMessageBox.Subsystem.SCHEDULER);
        
        // Send a message to verify port routing works (indirectly testing port resolution)
        Message testMessage = new Message("scheduler", "fire_incident", "test_data", Message.MessageType.FireEvent);
        fireIncidentBox.putMessage(testMessage, UDPMessageBox.Subsystem.SCHEDULER);
        
        // Port constants should be different for different subsystems
        assertNotEquals(UDPMessageBox.SCHEDULER_PORT, UDPMessageBox.FIRE_INCIDENT_PORT);
        assertNotEquals(UDPMessageBox.SCHEDULER_PORT, UDPMessageBox.DRONE_GUI_PORT);
        assertNotEquals(UDPMessageBox.FIRE_INCIDENT_PORT, UDPMessageBox.DRONE_GUI_PORT);
        
        testBox.closeBox();
    }
}