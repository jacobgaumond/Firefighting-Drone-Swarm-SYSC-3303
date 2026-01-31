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

    public void receiveUDPPacket(DatagramPacket receivePacket, String sourceName) {
        try {
            System.out.println("WAITING ON " + sourceName);
            socket.receive(receivePacket); // wait
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("RECEIVED: " + new String(receivePacket.getData(), 0, receivePacket.getLength()) + "\n");
    }
    public void sendUDPPacket(DatagramPacket sendPacket, String destinationName) {
        try {
            socket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("SENT TO " + destinationName + ": " + new String(sendPacket.getData(), 0, sendPacket.getLength()) + "\n");
    }
}
