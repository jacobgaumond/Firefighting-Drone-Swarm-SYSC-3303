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

//import java.io.*;
//import java.net.*;

public class DroneSubsystem implements Runnable {
//    SocketWrapper clientSocket;
//
//    public final static int DRONE_PORT = 5901;
//
//    public DroneSubsystem() {
//        try {
//            clientSocket = new SocketWrapper(DRONE_PORT);
//        } catch (SocketException se) {
//            throw new RuntimeException(se);
//        }
//    }

    private MessageBox incomingMessageBox;
    private MessageBox schedulerMessageBox;

    public DroneSubsystem(MessageBox incomingMessageBox, MessageBox schedulerMessageBox) {
        this.schedulerMessageBox = schedulerMessageBox;
        this.incomingMessageBox = incomingMessageBox;
    }

    @Override
    public void run() {
        boolean boxOpen = true;
        do {
            Message message = incomingMessageBox.getMessage();
            if (message == null) {
                boxOpen = false;
            }
            else {
                System.out.println("[DroneSubsystem] Received from " + message.getSourceName() + ": " + message.getMessageData());
                if (!message.getMessageData().equals("Acknowledged")) {
                    message = new Message("FireIncidentSubsystem", "DroneSubsystem", "Acknowledged", Message.MessageType.FireEvent);
                    schedulerMessageBox.putMessage(message);
                    System.out.println("[DroneSubsystem] Sending to FireIncidentSubsystem, through Scheduler: " + message.getMessageData());
                }
            }
        } while (boxOpen);
    }
}
