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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class FireIncidentSubsystem implements Runnable {
    private UDPMessageBox messageBox;

    private ArrayList<String> fileEvents = new ArrayList<String>();

    public static void main(String[] args) {
        String zoneFileName = "src/data/Sample_zone_file.csv";
        ZoneMap.loadZones(zoneFileName);

        String inputFileName = "src/data/Sample_event_file.csv";
        Thread fireIncidentSubsystem = new Thread(new FireIncidentSubsystem(inputFileName), "FireIncidentSubsystemThread");

        fireIncidentSubsystem.start();
    }

    public FireIncidentSubsystem(String fileName) {
        messageBox  = new UDPMessageBox(UDPMessageBox.Subsystem.FIRE_INCIDENT);
        if (fileName != null) loadFromFile(fileName);
    }

    // Testing constructor (no file)
    public FireIncidentSubsystem() {
        messageBox  = new UDPMessageBox(UDPMessageBox.Subsystem.FIRE_INCIDENT);
    }

    public void closeBox(){
        messageBox.closeBox();
    }

    @Override
    public void run() {
        for (String event : fileEvents) {
            //parse the event CSV
            FireEvent fireEvent = parseFireEvent(event);
            if(fireEvent == null) continue;

            //convert object into serialized string
            String serializedEvent = fireEvent.serialize();

            //create a Message for the Scheduler -> DroneSubsystem
            Message fireEventMessage = new Message(
                    "Scheduler",
                    "FireIncidentSubsystem",
                    serializedEvent,
                    Message.MessageType.FireEvent
            );

            messageBox.putMessage(fireEventMessage, UDPMessageBox.Subsystem.SCHEDULER);

            try {
                Thread.sleep(3000); // wait between events
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        boolean boxOpen = true;
        do {
            Message message = messageBox.getMessage();
            if (message == null) {
                boxOpen = false;
            }
            else {
                System.out.println("[FireIncidentSubsystem] Received from " + message.getSourceName() + ": " + message.getMessageData());
                if (!message.getMessageData().equals("Acknowledged")) {
                    message = new Message("DroneSubsystem", "FireIncidentSubsystem", "Acknowledged", Message.MessageType.FireEvent);
                    System.out.println("[FireIncidentSubsystem] Sending to DroneSubsystem, through Scheduler: " + message.getMessageData());
                    messageBox.putMessage(message, UDPMessageBox.Subsystem.SCHEDULER);
                }
            }
        } while (boxOpen);
    }

    private void loadFromFile(String fileName) {
        File file = new File(fileName);

        try {
            Scanner reader = new Scanner(file);

            if (reader.hasNextLine()) {
                reader.nextLine(); // Discard header
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

    public void sendMessage(Message message) {
        messageBox.putMessage(message, UDPMessageBox.Subsystem.SCHEDULER);
    }

    //parse csv file
    public FireEvent parseFireEvent(String line){
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

        return new FireEvent(time, zoneId, eventType, severity, targetX, targetY);
    }
}
