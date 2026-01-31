/* DroneSubsystem.java
 *
 * This project uses the Client-Server model.
 *
 * This class represents a CLIENT in the Firefighting Drone Swarm.
 *
 * The DroneSubsystem receives packets from:
 *     Scheduler:  events (Time, Zone ID, Event type, Severity)
 *
 * The DroneSubsystem sends packets to:
 *     Scheduler:  updates on events and drone statuses
 */

import java.io.*;
import java.net.*;

public class DroneSubsystem {
    SocketWrapper clientSocket;

    public final static int DRONE_PORT = 5001;

    public DroneSubsystem() {
        try {
            clientSocket = new SocketWrapper(DRONE_PORT);
        } catch (SocketException se) {
            throw new RuntimeException(se);
        }
    }

    /**
     * Establishes the UDP connection with Scheduler
     */
    public void sendAndReceive() {
        byte[] packetBuf = new byte[100];
        DatagramPacket receivePacket = new DatagramPacket(packetBuf, packetBuf.length);
        DatagramPacket sendPacket;

        // receive from Scheduler
        clientSocket.receiveUDPPacket(receivePacket, "SCHEDULER");

        // Mimic putting out fire
        try {
            System.out.println("Putting out fire...\n");
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // send to Scheduler
        String responseMsg = "Fire 1 extinguished";
        byte[] msg = responseMsg.getBytes();

        sendPacket = new DatagramPacket(msg, msg.length, receivePacket.getAddress(), Scheduler.SCHEDULER_PORT);
        clientSocket.sendUDPPacket(sendPacket, "SCHEDULER");

        sendPacket = new DatagramPacket(msg, msg.length, receivePacket.getAddress(), DroneGUI.DRONEGUI_PORT);
        clientSocket.sendUDPPacket(sendPacket, "DRONE GUI");

        clientSocket.close();
    }

    public static void main(String args[]) {
        DroneSubsystem client = new DroneSubsystem();
        client.sendAndReceive();
    }
}
