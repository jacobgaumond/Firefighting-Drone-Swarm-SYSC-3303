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
    DatagramPacket sendPacket, receivePacket;
    DatagramSocket sendReceiveSocket;

    public final static int FIRE_INCIDENT_PORT = 5000;

    public FireIncidentSubsystem() {
        try {
            sendReceiveSocket = new DatagramSocket(FIRE_INCIDENT_PORT);
        } catch (SocketException se) {
            throw new RuntimeException(se);
        }
    }

    /**
     * Establishes the UDP connection with Scheduler
     */
    public void sendAndReceive() {
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
        byte msg[] = event.getBytes(); // use first event as example

        // create packet
        try {
            sendPacket = new DatagramPacket(msg, msg.length, InetAddress.getLocalHost(), Scheduler.FIRE_INCIDENT_SCHEDULER_PORT);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // send to Scheduler
        try {
            sendReceiveSocket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("Fire Incident -> Scheduler: " + event);

        // receive from Scheduler
        byte data[] = new byte[100];
        receivePacket = new DatagramPacket(data, data.length);

        try {
            System.out.println("WAITING ON SCHEDULER");
            sendReceiveSocket.receive(receivePacket); // wait
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("RECEIVED: " + new String(data, 0, receivePacket.getLength()));

        sendReceiveSocket.close();
    }

    public static void main(String args[]) {
        FireIncidentSubsystem client = new FireIncidentSubsystem();
        client.sendAndReceive();
    }
}
