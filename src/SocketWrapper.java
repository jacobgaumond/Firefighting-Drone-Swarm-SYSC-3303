import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class SocketWrapper {
    private final DatagramSocket socket;

    public SocketWrapper(int port) throws SocketException {
        this.socket = new DatagramSocket(port);
    }

//    public SocketWrapper() throws SocketException{
//        this.socket = new DatagramSocket();
//    }

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
            System.out.println("UDP -- " + subsystemName + " WAITING ON (RECEIVING FROM) " + sourceSubsystemName);
            socket.receive(receivePacket); // wait
        } catch (SocketException se) {
            System.out.println("UDP -- " + subsystemName + " CANCELED WAITING ON " + sourceSubsystemName + " (SOCKET HAS BEEN CLOSED)");
            return false;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.exit(1);
        }

        System.out.println("UDP -- " + subsystemName + " RECEIVED FROM " + sourceSubsystemName + ": " + new String(receivePacket.getData(), 0, receivePacket.getLength()) + "\n");
        return true;
    }


    public void sendUDPPacket(DatagramPacket sendPacket, String subsystemName, String destinationSubsystemName) {
        try {
            socket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("UDP -- " + subsystemName + " SENT TO " + destinationSubsystemName + ": " + new String(sendPacket.getData(), 0, sendPacket.getLength()) + "\n");
    }
}
