/* Scheduler.java
 *
 * This project uses the Client-Server model.
 *
 * This class represents the SERVER in the Firefighting Drone Swarm.
 *
 * The Scheduler receives packets from:
 *     FireIncidentSubsystem:  events (Time, Zone ID, Event type, Severity)
 *     DroneSubsystem:         consults the Scheduler for tasks to perform
 *
 * The Scheduler sends packets to:
 *     FireIncidentSubsystem:  updates on events
 *     DroneSubsystem:         updates on events and drone statuses
 */

import java.io.*;
import java.net.*;

public class Scheduler {
    SocketWrapper serverSocket;

    public final static int SCHEDULER_PORT = 6000;

    public Scheduler() {
        try {
            serverSocket = new SocketWrapper(SCHEDULER_PORT);
        } catch (SocketException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Establishes the UDP connection to/from FireIncident and Drone subsystems
     */
    public void schedulerBridge() {
        byte[] packetBuf = new byte[100];
        DatagramPacket receivePacket = new DatagramPacket(packetBuf, packetBuf.length);
        DatagramPacket sendPacket;

        // receive from FireIncidentSubsystem
        serverSocket.receiveUDPPacket(receivePacket, "FIRE INCIDENT SUBSYSTEM");

        // send to DroneSubsystem
        sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), receivePacket.getAddress(), DroneSubsystem.DRONE_PORT);
        serverSocket.sendUDPPacket(sendPacket, "DRONE SUBSYSTEM");

        // receive from DroneSubsystem
        serverSocket.receiveUDPPacket(receivePacket, "DRONE SUBSYSTEM");

        // send to FireIncidentSubsystem
        sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), receivePacket.getAddress(), FireIncidentSubsystem.FIRE_INCIDENT_PORT);
        serverSocket.sendUDPPacket(sendPacket, "FIRE INCIDENT SUBSYSTEM");

        // close the socket
        serverSocket.close();
    }

    public static void main(String args[]) {
        Scheduler server = new Scheduler();
        server.schedulerBridge();
    }
}
