import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageTest {
    private final Message testMessage = new Message("Scheduler", "FireIncidentSystem", "FireEvent", Message.MessageType.FireEvent); // type

    @Test
    void getDestinationName() {
        assertEquals("Scheduler", testMessage.getDestinationName());
    }

    @Test
    void getSourceName() {
        assertEquals("FireIncidentSystem", testMessage.getSourceName());
    }

    @Test
    void getMessageData() {
        assertEquals("FireEvent", testMessage.getMessageData());
    }

    @Test
    void getMessageType() {
        assertEquals(Message.MessageType.FireEvent, testMessage.getMessageType());
    }
}
