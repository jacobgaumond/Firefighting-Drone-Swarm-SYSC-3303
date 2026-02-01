import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageBoxTest {

    @Test
    void putMessage() {
        MessageBox msgBox = new MessageBox();

        assertFalse(msgBox.isFull());
        msgBox.putMessage(new Message("FireIncidentSubsystem", "Scheduler", "TEST MESSAGE", Message.MessageType.FireEvent));
        assertTrue(msgBox.isFull());
    }

    @Test
    void isFull() {
        MessageBox msgBox = new MessageBox();

        msgBox.putMessage(new Message("FireIncidentSubsystem", "Scheduler", "TEST MESSAGE", Message.MessageType.FireEvent));
        assertTrue(msgBox.isFull());
    }

    @Test
    void getMessage() {
        MessageBox msgBox = new MessageBox();

        msgBox.putMessage(new Message("FireIncidentSubsystem", "Scheduler", "TEST MESSAGE", Message.MessageType.FireEvent));
        assertTrue(msgBox.isFull());

        Message msg = msgBox.getMessage();
        assertNotNull(msg);
    }
}