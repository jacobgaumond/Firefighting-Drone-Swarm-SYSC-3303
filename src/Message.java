public class Message {
    public enum MessageType { // TODO: Add more MessageType values as needed (e.g., DroneResponseEvent, etc.)
        FireEvent
    }

    private final MessageType   type;
    private final String        data;

    private final String        destinationName;
    private final String        sourceName;

    public Message(String destinationName, String sourceName, String messageData, MessageType messageType) {
        type = messageType;
        data = messageData;

        this.destinationName = destinationName;
        this.sourceName      = sourceName;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getMessageData() {
        return data;
    }

    public MessageType getMessageType() {
        return type;
    }
}
