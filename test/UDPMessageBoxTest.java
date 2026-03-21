import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

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
    void getAndPutMessage() {
        Message testMessage = new Message("fire_incident", "scheduler", "fire_detected", Message.MessageType.FireEvent);
        
        // schedulerBox is empty
        assertFalse(schedulerBox.isFull());
        
        // send message to SCHEDULER
        Thread sendThread = new Thread(() -> {
            fireIncidentBox.putMessage(testMessage, UDPMessageBox.SCHEDULER_PORT);
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
    void isFull() {
        // Initially empty
        assertFalse(schedulerBox.isFull());
        
        // Send a message to box
        Message testMessage = new Message("fire_incident", "scheduler", "fire_detected", Message.MessageType.FireEvent);
        Thread sendThread = new Thread(() -> {
            fireIncidentBox.putMessage(testMessage, UDPMessageBox.SCHEDULER_PORT);
        });
        sendThread.start();
        
        // assertFalse(schedulerBox.isFull()); 
        // isFull used for blocking internally
        // Blocked by schedulerBox's receive

        // message is received and box becomes full
        Message retrievedMessage = schedulerBox.getMessage(); // GET
        assertNotNull(retrievedMessage);
        
        // After retrieving, box empty again
        assertFalse(schedulerBox.isFull());
    }
}