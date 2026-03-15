import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class UDPMessageBox {
    // This class is a wrapper for the MessageBox class, which automates the process of sending/receiving Message
    // objects as UDP datagrams over a network. This class is meant to facilitate inter-subsystem message passing.
    //
    // The class has a listener thread, which listens to / receives from a socket. Incoming UDP datagrams are put
    // into the MessageBox.
    //
    // Messages put into the message box by a subsystem are immediately sent through the socket.
    //
    // Other MessageBox methods are supported; many simply delegate to the MessageBox.

    private class SocketListenerThread extends Thread {
        private final UDPMessageBox UDP_MESSAGE_BOX;
        private final SocketWrapper SOCKET;

        private boolean keepListening = true;

        public SocketListenerThread(UDPMessageBox udpMessageBox, SocketWrapper socket) {
            super(udpMessageBox.getSubsystem().toString() + "_LISTENER_FOR_" + udpMessageBox.getTargetSubsystem().toString());

            UDP_MESSAGE_BOX = udpMessageBox;
            SOCKET = socket;
        }

        public void stopListening() {
            keepListening = false;
        }

        public void run() {
            byte[] dataBuffer = new byte[200];
            while (keepListening) {
                DatagramPacket receivePacket = new DatagramPacket(dataBuffer, dataBuffer.length);

                if (SOCKET.receiveUDPPacket(receivePacket, UDP_MESSAGE_BOX.getSubsystem().toString(), UDP_MESSAGE_BOX.getTargetSubsystem().toString())) {
                    Message message = new Message(new String(dataBuffer, 0, receivePacket.getLength()));
                    UDP_MESSAGE_BOX.putMessage(message);
                }
            }
        }
    }

    public enum Subsystem {
        FIRE_INCIDENT,
        SCHEDULER,
        DRONE_GUI,
        DRONE
    }

    private Subsystem SUBSYSTEM;
    private Subsystem TARGET_SUBSYSTEM;

    private final MessageBox INCOMING_BOX;

    private SocketWrapper socket;
    private final SocketListenerThread LISTENER_THREAD;

    private InetAddress targetSubsystemAddress;
    private final int   TARGET_SUBSYSTEM_PORT;

    // Port constants
    public final static int FIRE_INCIDENT_PORT  = 9500;
    public final static int SCHEDULER_PORT      = 9501;
    public final static int DRONE_GUI_PORT      = 9502;

    public final static int BASE_DRONE_PORT     = 9503; // Must be larger (or significantly smaller than) the other ports

    // Static counter of already-created drone boxes (used to avoid drones with duplicate ports)
    private static int numDroneMessageBoxes = 0;

    /**
     * Creates a UDPMessageBox to allow a subsystem to communicate with some other targetSubsystem.
     *
     * @param subsystem         The Subsystem which owns this UDPMessageBox.
     * @param targetSubsystem   The Subsystem with which this UDPMessageBox is facilitating communication with (i.e., `subsystem`'s target).
     */
    public UDPMessageBox(Subsystem subsystem, Subsystem targetSubsystem) {
        INCOMING_BOX = new MessageBox();

        SUBSYSTEM           = subsystem;
        TARGET_SUBSYSTEM    = targetSubsystem;

        // Determine port numbers
        int subsystemPort = -1;
        int targetSubsystemPort = -1;
        switch(SUBSYSTEM) {
            case Subsystem.FIRE_INCIDENT:
                subsystemPort = FIRE_INCIDENT_PORT;
                break;
            case Subsystem.SCHEDULER:
                subsystemPort = SCHEDULER_PORT;
                break;
            case Subsystem.DRONE_GUI:
                subsystemPort = DRONE_GUI_PORT;
                break;
            case Subsystem.DRONE:
                subsystemPort = (BASE_DRONE_PORT + numDroneMessageBoxes);
                numDroneMessageBoxes += 1; // Increment, so that the next drone gets a unique port.
                break;
            default:
                System.out.println("UDPMessageBox class must be instantiated with an appropriate/supported subsystem");
                System.exit(1);
        }
        switch(TARGET_SUBSYSTEM) {
            case Subsystem.FIRE_INCIDENT:
                targetSubsystemPort = FIRE_INCIDENT_PORT;
                break;
            case Subsystem.SCHEDULER:
                targetSubsystemPort = SCHEDULER_PORT;
                break;
            case Subsystem.DRONE_GUI:
                targetSubsystemPort = DRONE_GUI_PORT;
                break;
            case Subsystem.DRONE:
                targetSubsystemPort = (BASE_DRONE_PORT + numDroneMessageBoxes);
                numDroneMessageBoxes += 1; // Increment, so that the next drone gets a unique port.
                break;
            default:
                System.out.println("UDPMessageBox class must be instantiated with an appropriate/supported targetSubsystem");
                System.exit(1);
        }

        // Setup UDP
            // Create source socket
        socket = null;
        try {
            socket = new SocketWrapper(subsystemPort);
        } catch (SocketException e) {
            e.printStackTrace();
            System.exit(1);
        }
            // Create listener for the socket
        LISTENER_THREAD = new SocketListenerThread(this, socket);
            // Initialize target port/address
        targetSubsystemAddress = null;
        try {
            targetSubsystemAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException uhe) {
            uhe.printStackTrace();
            System.exit(1);
        }
        TARGET_SUBSYSTEM_PORT = targetSubsystemPort;

        // Start the LISTENER_THREAD before finishing...
        LISTENER_THREAD.start();
    }

    /**
     * Getter method for SUBSYSTEM
     *
     * @return Subsystem    SUBSYSTEM object variable, representing the subsystem that owns the UDPMessageBox
     */
    public Subsystem getSubsystem() {
        return SUBSYSTEM;
    }
    /**
     * Getter method for TARGET_SUBSYSTEM
     *
     * @return Subsystem    TARGET_SUBSYSTEM object variable, representing the subsystem that the message box is targeting in its
     * communications
     */
    public Subsystem getTargetSubsystem() {
        return TARGET_SUBSYSTEM;
    }

    /**
     * Retrieves the Message object that is inside the INCOMING_BOX MessageBox, emptying the box.
     * If the box is empty, it waits until the box has a message.
     *
     * Delegates to MessageBox.getMessage().
     *
     * @return  the Message object that was inside the box, or `null` if the box has been closed.
     */
    public Message getMessage() {
        return INCOMING_BOX.getMessage();
    }

    /**
     * Places a Message object inside the INCOMING_BOX MessageBox, filling the box.
     * If the box is full, it waits until the box has been emptied.
     *
     * If sourceIsSubsystem is true, it immediately sends the Message over UDP to the TARGET_SUBSYSTEM.
     * Otherwise, delegates to MessageBox.putMessage().
     *
     * @param message           the Message object to be placed in the box.
     * @param sourceIsSubsystem boolean signifying whether the caller of this method is the SUBSYSTEM subsystem.
     *
     * @return                  the Message object that has been placed in the box, or `null` if the box has been closed.
     */
    public Message putMessage(Message message, boolean sourceIsSubsystem) {
        if (sourceIsSubsystem) {
            // Skip message box, and send over UDP
            // (otherwise, it causes issues with subsystem/listener competing over use of the box)
            byte[] dataBuffer = message.serialize().getBytes();
            DatagramPacket sendPacket = new DatagramPacket(dataBuffer, dataBuffer.length,
                    targetSubsystemAddress, TARGET_SUBSYSTEM_PORT);

            socket.sendUDPPacket(sendPacket, SUBSYSTEM.toString(), TARGET_SUBSYSTEM.toString());

            return message; // Return message to mimic Message.putMessage() behavior
        }
        else {
            return INCOMING_BOX.putMessage(message);
        }
    }

    /**
     * A polymorphic version of `Message putMessage(Message message, boolean sourceIsSubsystem)` that hard-codes
     * sourceIsSubsystem to true. Meant to be used as the default.
     *
     * @param message   the Message object to be placed in the box.
     * @return          the Message object that has been placed in the box, or `null` if the box has been closed.
     */
    public Message putMessage(Message message) {
        return putMessage(message, true);
    }

    /**
     * Checks if the INCOMING_BOX MessageBox is full.
     *
     * Delegates to MessageBox.isFull().
     *
     * @return
     */
    public boolean isFull() {
        return INCOMING_BOX.isFull();
    }

    /**
     * Closes the INCOMING_BOX MessageBox, closes the socket, and stops the socket's listener thread.
     */
    public void closeBox() {
        LISTENER_THREAD.stopListening();
        socket.close();

        INCOMING_BOX.closeBox();
    }
}
