// DroneSubsystem.java
// This class is one of the CLIENT sides of the Firefighting Drone Swarm

// The DroneSubsystem receives packets from:
//      Scheduler:  events (Time, Zone ID, Event type, Severity)

// The DroneSubsystem sends packets to:
//      Scheduler:  updates on events and drone statuses

import java.io.*;
import java.net.*;

public class DroneSubsystem {
    DatagramPacket sendPacket, receivePacket;
    DatagramSocket sendReceiveSocket;

    public final static int DRONE_PORT = 5001;

    public DroneSubsystem() {
        try {
            sendReceiveSocket = new DatagramSocket(5001);
        } catch (SocketException se) {
            throw new RuntimeException(se);
        }
    }

    /**
     * Establishes the UDP connection with Scheduler
     */
    public void sendAndReceive() {
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

        // Mimic putting out fire
        try {
            System.out.println("Putting out fire...");
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // send to Scheduler
        String responseMsg = "Fire 1 extinguished";
        byte msg[] = responseMsg.getBytes();


        try {
            sendPacket = new DatagramPacket(msg, msg.length, receivePacket.getAddress(), Scheduler.DRONE_SCHEDULER_PORT);
            sendReceiveSocket.send(sendPacket);
            sendPacket = new DatagramPacket(msg, msg.length, receivePacket.getAddress(), DroneGUI.DRONEGUI_PORT);
            sendReceiveSocket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("\nDrone -> Scheduler: " + responseMsg);

        sendReceiveSocket.close();
    }

    public static void main(String args[]) {
        DroneSubsystem client = new DroneSubsystem();
        client.sendAndReceive();
    }
}
