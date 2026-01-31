/* FireIncidentSubsystem.java
 *
 * This project uses the Client-Server model.
 *
 * This class represents a CLIENT in the Firefighting Drone Swarm.
 *
 * The FireIncidentSubsystem receives packets from:
 *     Scheduler:  updates on events
 *
 * The FireIncidentSubsystem sends packets to:
 *     Scheduler:  events (Time, Zone ID, Event type, Severity)
 */

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class FireIncidentSubsystem {
    SocketWrapper clientSocket;

    public final static int FIRE_INCIDENT_PORT = 9502;

    public FireIncidentSubsystem() {
        try {
            clientSocket = new SocketWrapper(FIRE_INCIDENT_PORT);
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

        // get event data from Sample_event_file.csv
        String event = new String(); // TODO: refine in future iterations (Event class)

        try (BufferedReader br = new BufferedReader(new FileReader(System.getProperty("user.dir") + "/src/data/Sample_event_file.csv"))) {
            String line = br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                event = line;
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        byte[] msg = event.getBytes(); // use first event as example

        // send to Scheduler
        try {
            sendPacket = new DatagramPacket(msg, msg.length, InetAddress.getLocalHost(), Scheduler.SCHEDULER_PORT);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(1);
        }

        clientSocket.sendUDPPacket(sendPacket, "SCHEDULER");

        // receive from Scheduler
        clientSocket.receiveUDPPacket(receivePacket, "SCHEDULER");

        // close the socket
        clientSocket.close();
    }

    public static void main(String args[]) {
        FireIncidentSubsystem client = new FireIncidentSubsystem();
        client.sendAndReceive();
    }
}
