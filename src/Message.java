
public class Message {

    public enum MessageType {
        FireEvent,
        DroneRequest,
        DroneResponse,
        DroneRegistration
    }

    private final MessageType type;
    private final String data;

    private final String destinationName;
    private final String sourceName;

    private final static String DELIMITER = "#";

    private int senderPort = -1;

    public Message(String destinationName, String sourceName, String messageData, MessageType messageType) {
        type = messageType;
        data = messageData;

        this.destinationName = destinationName;
        this.sourceName = sourceName;
    }

    /**
     * Constructor that creates a Message object from the output of the Message
     * class's serialize() method. In other words, it deserializes Strings to
     * create Message objects.
     *
     * @param serializedMessage A String containing a serialized Message object.
     */
    public Message(String serializedMessage) {
        String[] objectVariables = serializedMessage.split(DELIMITER);

        // Convert "type" (stored in objectVariables[0]) from String to MessageType
        MessageType type = null;
        try {
            type = MessageType.valueOf(objectVariables[0]);
        } catch (IllegalArgumentException iae) {
            iae.printStackTrace();
            System.exit(1);
        }
        // Initialize object variables
        this.type = type;
        data = objectVariables[1];
        destinationName = objectVariables[2];
        sourceName = objectVariables[3];
    }

    /**
     * Serializes the Message object into a String.
     *
     * @return String representation of the Message object.
     */
    public String serialize() {
        String type = this.type.toString();

        return type + DELIMITER + data + DELIMITER + destinationName + DELIMITER + sourceName;
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

    public int getSenderPort(){return senderPort;}
    public void setSenderPort(int senderPort){this.senderPort= senderPort;}
}
