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

//import java.io.*;
//import java.net.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class FireIncidentSubsystem implements Runnable {
//    SocketWrapper clientSocket;
//
//    public final static int FIRE_INCIDENT_PORT = 9502;
//
//    public FireIncidentSubsystem() {
//        try {
//            clientSocket = new SocketWrapper(FIRE_INCIDENT_PORT);
//        } catch (SocketException se) {
//            throw new RuntimeException(se);
//        }
//    }

    private MessageBox incomingMessageBox;
    private MessageBox schedulerMessageBox;

    private ArrayList<String> fileEvents = new ArrayList<String>();

    public FireIncidentSubsystem(MessageBox incomingMessageBox, MessageBox schedulerMessageBox, String fileName) {
        this.incomingMessageBox = incomingMessageBox;
        this.schedulerMessageBox = schedulerMessageBox;

        loadFromFile(fileName);
    }

    @Override
    public void run() {
        for (String event : fileEvents) {
            Message message = new Message("DroneSubsystem", "FireIncidentSubsystem", event, Message.MessageType.FireEvent);
            schedulerMessageBox.putMessage(message);
            System.out.println("[DroneSubsystem] Sending to FireIncidentSubsystem, through Scheduler: " + message.getMessageData());
        }
        schedulerMessageBox.closeBox();
        incomingMessageBox.closeBox();
    }

    private void loadFromFile(String fileName) {
        File file = new File(fileName);

        try {
            Scanner reader = new Scanner(file);

            while (reader.hasNextLine()) {
                String line = reader.nextLine();

                if (!line.trim().isEmpty()) {
                    fileEvents.add(line);
                }
            }

            reader.close();
        } catch (FileNotFoundException e) {
            System.err.println("Error: " + e.getMessage());

            System.exit(1);
        }
    }
}
