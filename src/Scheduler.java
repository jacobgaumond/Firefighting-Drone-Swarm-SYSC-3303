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
    DatagramPacket sendPacket, receivePacket;

    DatagramSocket serverSocket;

    public final static int SCHEDULER_PORT = 6000;

    public Scheduler() {
        try {
            serverSocket = new DatagramSocket(SCHEDULER_PORT);
        } catch (SocketException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Establishes the UDP connection to/from FireIncident and Drone subsystems
     */
    public void schedulerBridge() {
        // receive from FireIncidentSubsystem
        byte[] data = new byte[100];
        receivePacket = new DatagramPacket(data, data.length);

        try {
            System.out.println("WAITING ON FIRE INCIDENT");
            serverSocket.receive(receivePacket); // wait
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("RECEIVED: " + new String(data, 0, receivePacket.getLength()) + "\n");

        // send to DroneSubsystem
        sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), receivePacket.getAddress(), DroneSubsystem.DRONE_PORT);
        try {
            serverSocket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("SCHEDULER -> DRONE: " + new String(data, 0, sendPacket.getLength()) + "\n");

        // receive from DroneSubsystem
        try {
            System.out.println("WAITING ON DRONE");
            serverSocket.receive(receivePacket); // wait
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("RECEIVED: " + new String(data, 0, receivePacket.getLength()) + "\n");

        // send to FireIncidentSubsystem
        sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), receivePacket.getAddress(), FireIncidentSubsystem.FIRE_INCIDENT_PORT);
        try {
            serverSocket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("SENT -> FIRE INCIDENT: " + new String(data, 0, sendPacket.getLength()) + "\n");

        serverSocket.close();
    }

    public static void main(String args[]) {
        Scheduler server = new Scheduler();
        server.schedulerBridge();
    }
}
