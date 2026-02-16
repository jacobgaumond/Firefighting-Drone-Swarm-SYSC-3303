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

    //constructor just for testing [FireIncidentSub -> testParseFireEvent] (doesn't load file)
    public FireIncidentSubsystem(MessageBox incomingMessageBox, MessageBox schedulerMessageBox) {
        this.incomingMessageBox = incomingMessageBox;
        this.schedulerMessageBox = schedulerMessageBox;
    }

    @Override
    public void run() {
        for (String event : fileEvents) {
            Message message = new Message("DroneSubsystem", "FireIncidentSubsystem", event, Message.MessageType.FireEvent);
            schedulerMessageBox.putMessage(message);
            System.out.println("[FireIncidentSubsystem] Sending to DroneSubsystem, through Scheduler: " + message.getMessageData());



            //parse the event CSV
            FireTask fireTask = parseFireEvent(event);
            if(fireTask == null) {
                System.out.println("FireTask is null, skipping.");
                continue;
            }

            //check the parsed info
            String messageInfo = fireTask.toString();
            System.out.println(messageInfo);


            //create a Message for the Scheduler -> DroneSubsystem
            Message fireTaskMessage = new Message(
                    "Scheduler",
                    "FireIncidentSubsystem",
                    messageInfo,
                    Message.MessageType.FireEvent
            );

            schedulerMessageBox.putMessage(fireTaskMessage);
        }

        boolean boxOpen = true;
        do {
            Message message = incomingMessageBox.getMessage();
            if (message == null) {
                boxOpen = false;
            }
            else {
                System.out.println("[FireIncidentSubsystem] Received from " + message.getSourceName() + ": " + message.getMessageData());
                if (!message.getMessageData().equals("Acknowledged")) {
                    message = new Message("DroneSubsystem", "FireIncidentSubsystem", "Acknowledged", Message.MessageType.FireEvent);
                    System.out.println("[FireIncidentSubsystem] Sending to DroneSubsystem, through Scheduler: " + message.getMessageData());
                    schedulerMessageBox.putMessage(message);
                }
            }
        } while (boxOpen);

//        schedulerMessageBox.closeBox();
//        incomingMessageBox.closeBox();
    }

    private void loadFromFile(String fileName) {
        File file = new File(fileName);

        try {
            Scanner reader = new Scanner(file);

            if (reader.hasNextLine()) {
                // Discard the header
                reader.nextLine();
            }
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


    //parse the csv file
    public FireTask parseFireEvent(String line){
        String[] parts = line.split(",");
        if(parts.length != 4){
            System.err.println("Error: FireIncidentSubsystem: Invalid FireEvent");
            return null;
        }

        String time = parts[0];
        int zoneId;
        try {
            zoneId = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            System.err.println("Error: FireIncidentSubsystem: Invalid zoneId");
            return null;
        }
        String eventType = parts[2];
        String severity = parts[3];


        //coordinates where drone should go to
        //for now Center of the fire zone is used -> Later can be randomized or specific coordinates
        int targetX = ZoneMap.getX(zoneId);
        int targetY = ZoneMap.getY(zoneId);

        return new FireTask(time, zoneId, eventType, severity, targetX, targetY);
    }


}
