import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

// This class is a wrapper for the MessageBox class, which automates the sending/receiving Message
// objects as UDP datagrams over a network. This class is meant to facilitate inter-subsystem message passing.
//
// The class has a listener thread, which listens to/receives from a socket. 
// Incoming UDP datagrams are put into the MessageBox.
//
// Messages put into the message box by a subsystem are immediately sent through the socket.
//
// Other MessageBox methods are supported; many simply delegate to the MessageBox.

public class UDPMessageBox {
    public enum Subsystem {
        FIRE_INCIDENT,
        SCHEDULER,
        DRONE_GUI,
        DRONE,
        VOID // Used as the target subsystem for "incoming message boxes", which do not send outgoing messages over UDP.
    }

    private Subsystem SUBSYSTEM;
    private Subsystem TARGET_SUBSYSTEM;

    private final MessageBox INCOMING_BOX;

    private SocketWrapper socket;
    private final ListenerThread LISTENER_THREAD;

    private InetAddress targetSubsystemAddress;

    // Port constants
    public final static int FIRE_INCIDENT_PORT  = 9500;
    public final static int SCHEDULER_PORT      = 9501;
    public final static int DRONE_GUI_PORT      = 9502;
    public final static int BASE_DRONE_PORT     = 9503; // Increments per drone

    // Counter of created drones (avoid duplicate ports)
    private static int numDrones = 0;

    /**
     * Creates a UDPMessageBox to allow a subsystem to communicate with some other targetSubsystem.
     *
     * @param subsystem         The Subsystem which owns this UDPMessageBox.
     * @param targetSubsystem   The Subsystem with which this UDPMessageBox is facilitating communication with.
     */
    public UDPMessageBox(Subsystem subsystem) {
    // public UDPMessageBox(Subsystem subsystem, Subsystem targetSubsystem) {
        INCOMING_BOX = new MessageBox();

        SUBSYSTEM        = subsystem;
        // TARGET_SUBSYSTEM = targetSubsystem;

        // Determine port numbers
        int port = getSubsystemPort(SUBSYSTEM);
        // int targetPort = getTargetSubsystemPort(TARGET_SUBSYSTEM);

        // Create socket
        try {
            socket = new SocketWrapper(port);
        } catch (SocketException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // Create listener
        LISTENER_THREAD = new ListenerThread(this, socket);

        // Initialize target port/address
        try {
            targetSubsystemAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException uhe) {
            uhe.printStackTrace();
            System.exit(1);
        }
        // TARGET_SUBSYSTEM_PORT = targetPort;

        // Start the LISTENER_THREAD
        LISTENER_THREAD.start();
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
     * If sourceIsSubsystem is true, it immediately sends the Message over UDP to the targetSubsystem.
     * Otherwise, delegates to MessageBox.putMessage().
     *
     * @param message           the Message object to be placed in the box.
     * @param sourceIsSubsystem boolean signifying whether the caller of this method is the SUBSYSTEM subsystem.
     * @param targetSubsystem   the target Subsystem to send the message to.
     *
     * @return                  the Message object that has been placed in the box, or `null` if the box has been closed.
     */
    public Message putMessage(Message message, boolean sourceIsSubsystem, Subsystem targetSubsystem) {
        if (sourceIsSubsystem) {
            int targetPort = getTargetSubsystemPort(targetSubsystem);
            if (targetPort == -1) {
                System.out.println("Incoming message boxes cannot send outgoing messages (there is no destination).");
                System.exit(1);
            }
            else {
                // Skip message box, and send over UDP
                // (otherwise, it causes issues with subsystem/listener competing over use of the box)
                byte[] dataBuffer = message.serialize().getBytes();
                DatagramPacket sendPacket = new DatagramPacket(dataBuffer, dataBuffer.length,
                        targetSubsystemAddress, targetPort);

                socket.sendUDPPacket(sendPacket, SUBSYSTEM.toString(), targetSubsystem.toString());

                return message; // Return message to mimic Message.putMessage() behavior
            }
        }
        return INCOMING_BOX.putMessage(message);
    }

    /**
     * A polymorphic version of `Message putMessage(Message message, boolean sourceIsSubsystem, Subsystem targetSubsystem)` that hard-codes
     * sourceIsSubsystem to true. Meant to be used as the default.
     *
     * @param message           the Message object to be placed in the box.
     * @param targetSubsystem   the target Subsystem to send the message to.
     * @return                  the Message object that has been placed in the box, or `null` if the box has been closed.
     */
    public Message putMessage(Message message, Subsystem targetSubsystem) {
        return putMessage(message, true, targetSubsystem);
    }
    
    public void closeBox() {
        LISTENER_THREAD.stopListening();
        socket.close();

        INCOMING_BOX.closeBox();
    }

    // ========== Getters ==========
    public Subsystem getSubsystem() {
        return SUBSYSTEM;
    }
    public Subsystem getTargetSubsystem() {
        return TARGET_SUBSYSTEM;
    }

    // ========== Helpers ==========
    private int getSubsystemPort(Subsystem subsystem) {
        return switch (subsystem) {
            case FIRE_INCIDENT -> FIRE_INCIDENT_PORT;
            case SCHEDULER -> SCHEDULER_PORT;
            case DRONE_GUI -> DRONE_GUI_PORT;
            case DRONE -> {
                int port = BASE_DRONE_PORT + numDrones;
                numDrones += 1;
                yield port;
            }
            case VOID -> {
                System.out.println("UDPMessageBox class must be instantiated with an appropriate/supported subsystem");
                System.exit(1);
                yield -1;
            }
        };
    }
    private int getTargetSubsystemPort(Subsystem targetSubsystem) {
        return switch (targetSubsystem) {
            case FIRE_INCIDENT -> FIRE_INCIDENT_PORT;
            case SCHEDULER -> SCHEDULER_PORT;
            case DRONE_GUI -> DRONE_GUI_PORT;
            case DRONE -> {
                int port = BASE_DRONE_PORT + numDrones;
                numDrones += 1;
                yield port;
            }
            case VOID -> -1;
        };
    }
    public boolean isFull() {
        return INCOMING_BOX.isFull();
    }

    // ========== Inner classes ==========
    public class SocketWrapper {
        private final DatagramSocket socket;

        public SocketWrapper(int port) throws SocketException {
            this.socket = new DatagramSocket(port);
        }

        public void close() {
            socket.close();
        }

        /**
         * Receives a UDP datagram from the socket, returning a boolean signifying method success.
         *
         * @param receivePacket         The DatagramPacket to receive on.
         * @param subsystemName         The name of the subsystem who owns the socket.
         * @param sourceSubsystemName   The name of the subsystem that sent the DatagramPacket.
         * @return                      boolean signifying whether the method proceeded successfully/unsuccessfully
         *                              (i.e., false if the socket is closed while this method is receiving).
         */
        public boolean receiveUDPPacket(DatagramPacket receivePacket, String subsystemName, String sourceSubsystemName) {
            try {
                // System.out.println("UDP -- " + subsystemName + " WAITING ON EVENT");
                socket.receive(receivePacket); // wait
            } catch (SocketException se) {
                // System.out.println("UDP -- " + subsystemName + " CANCELED WAITING ON " + sourceSubsystemName + " (SOCKET HAS BEEN CLOSED)");
                return false;
            } catch (IOException ioe) {
                ioe.printStackTrace();
                System.exit(1);
            }

            // System.out.println("UDP -- " + subsystemName + " RECEIVED FROM " + sourceSubsystemName + ": " + new String(receivePacket.getData(), 0, receivePacket.getLength()));
            return true;
        }

        public void sendUDPPacket(DatagramPacket sendPacket, String subsystemName, String destinationSubsystemName) {
            try {
                socket.send(sendPacket);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }

            // System.out.println("UDP -- " + subsystemName + " SENT TO " + destinationSubsystemName + ": " + new String(sendPacket.getData(), 0, sendPacket.getLength()));
        }
    }

    private class ListenerThread extends Thread {
        private final UDPMessageBox UDP_MESSAGE_BOX;
        private final SocketWrapper SOCKET;

        private boolean keepListening = true;

        public ListenerThread(UDPMessageBox udpMessageBox, SocketWrapper socket) {
            super(udpMessageBox.getSubsystem().toString() + "_LISTENER");

            UDP_MESSAGE_BOX = udpMessageBox;
            SOCKET = socket;
        }

        public void stopListening() {
            keepListening = false;
        }

        @Override
        public void run() {
            byte[] dataBuffer = new byte[200];
            while (keepListening) {
                DatagramPacket receivePacket = new DatagramPacket(dataBuffer, dataBuffer.length);

                if (SOCKET.receiveUDPPacket(receivePacket, UDP_MESSAGE_BOX.getSubsystem().toString(), "UNKNOWN_SOURCE")) {
                    Message message = new Message(new String(dataBuffer, 0, receivePacket.getLength()));
                    UDP_MESSAGE_BOX.INCOMING_BOX.putMessage(message);
                }
            }
        }
    }
}
