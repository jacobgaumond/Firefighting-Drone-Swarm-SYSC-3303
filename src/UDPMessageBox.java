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
        DRONE,
    }

    private final Subsystem SUBSYSTEM;
    private final MessageBox INCOMING_BOX;
    private DatagramSocket socket;
    private final ListenerThread LISTENER_THREAD;

    // Port constants
    public final static int FIRE_INCIDENT_PORT  = 9500;
    public final static int SCHEDULER_PORT      = 9501;
    // scheduler tracks ephemeral drone ports 

    /**
     * UDPMessageBox constructor allowing a subsystem to communicate with another
     *
     * @param subsystem The Subsystem which owns this UDPMessageBox
     */
    public UDPMessageBox(Subsystem subsystem) {
        SUBSYSTEM = subsystem;
        INCOMING_BOX = new MessageBox();

        // Create socket
        try {
            if (SUBSYSTEM == Subsystem.SCHEDULER) {
                socket = new DatagramSocket(SCHEDULER_PORT); 
            } 
            else if (SUBSYSTEM == Subsystem.FIRE_INCIDENT) {
                socket = new DatagramSocket(FIRE_INCIDENT_PORT); 
            } 
            else if (SUBSYSTEM == Subsystem.DRONE) {
                socket = new DatagramSocket(); // ephemeral port
            } 
        } catch (SocketException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // Create listener
        LISTENER_THREAD = new ListenerThread(this);

        // Start the LISTENER_THREAD
        LISTENER_THREAD.start();
    }

    /**
     * Delegates to MessageBox.getMessage()
     *
     * @return  the Message object that was inside the box
     */
    public Message getMessage() {
        return INCOMING_BOX.getMessage();
    }
    
    /**
     * Sends messages to other subsystems.
     *
     * @param message   the Message object to send.
     * @param port      the target port to send the message to.
     */
    public void putMessage(Message message, int port) {
        sendMessage(message, port);
    }
    
    /**
     * Sends a Message over UDP to the target port
     *
     * @param message   the Message to send
     * @param port      the target port to send the message to
     */
    public void sendMessage(Message message, int port) {
        byte[] data = message.serialize().getBytes();
        try {
            InetAddress targetAddress = InetAddress.getLocalHost(); // TODO pretend not all localHost?
            DatagramPacket sendPacket = new DatagramPacket(data, data.length, targetAddress, port);
            System.out.println("UDP [" + SUBSYSTEM + "] -> Sending message on PORT: " + port);
            socket.send(sendPacket);
        } catch (UnknownHostException uhe) {
            uhe.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
        
    /**
     * Receives a UDP datagram from the socket, returning a boolean signifying method success.
     *
     * @param receivePacket         The DatagramPacket to receive on.
     * @return                      boolean signifying whether the method proceeded successfully/unsuccessfully
     *                              (i.e., false if the socket is closed while this method is receiving).
     */
    private boolean receiveUDPPacket(DatagramPacket receivePacket) {
        try {
            socket.receive(receivePacket); // wait
        } catch (SocketException se) {
            return false;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.exit(1);
        }
        return true;
    }

    public void closeBox() {
        LISTENER_THREAD.stopListening();
        closeSocket();
        INCOMING_BOX.closeBox();
    }

    // ========== Helpers ==========
    public int getPort() {
        return socket.getLocalPort();
    }
    public boolean isFull() {
        return INCOMING_BOX.isFull();
    }
    private void closeSocket() {
        socket.close();
    }

    private class ListenerThread extends Thread {
        private final UDPMessageBox UDP_MESSAGE_BOX;
        private boolean keepListening = true;

        public ListenerThread(UDPMessageBox udpMessageBox) {
            super(udpMessageBox.SUBSYSTEM.toString() + "_LISTENER");
            UDP_MESSAGE_BOX = udpMessageBox;
        }

        @Override
        public void run() {
            byte[] dataBuffer = new byte[200];
            while (keepListening) {
                DatagramPacket receivePacket = new DatagramPacket(dataBuffer, dataBuffer.length);

                if (UDP_MESSAGE_BOX.receiveUDPPacket(receivePacket)) {
                    Message message = new Message(new String(dataBuffer, 0, receivePacket.getLength()));
                    System.out.println("UDP [" + UDP_MESSAGE_BOX.SUBSYSTEM + "] <- Received message from PORT: " + receivePacket.getPort());
                    UDP_MESSAGE_BOX.INCOMING_BOX.putMessage(message);
                }
            }
        }

        public void stopListening() {
            keepListening = false;
        }
    }
}
