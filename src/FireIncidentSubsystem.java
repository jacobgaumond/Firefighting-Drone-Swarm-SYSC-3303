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
    private DroneGUI gui;
    private UDPMessageBox incomingMessageBox;
    private UDPMessageBox schedulerMessageBox;

    private ArrayList<String> fileEvents = new ArrayList<String>();

    public static void main(String[] args) {
        String zoneFileName = "src/data/Sample_zone_file.csv"; // TODO: Fix ZoneMap
        ZoneMap.loadZones(zoneFileName);
        ZoneMap.printZones();

        DroneGUI gui = null; // TODO: Fix gui

        String inputFileName = "src/data/Sample_event_file.csv";

        Thread fireIncidentSubsystem = new Thread(new FireIncidentSubsystem(inputFileName, gui),
                "FireIncidentSubsystemThread");

        fireIncidentSubsystem.start();
    }

    public FireIncidentSubsystem(String fileName, DroneGUI gui) {
        incomingMessageBox  = new UDPMessageBox(UDPMessageBox.Subsystem.FIRE_INCIDENT, UDPMessageBox.Subsystem.VOID);
        schedulerMessageBox = new UDPMessageBox(UDPMessageBox.Subsystem.FIRE_INCIDENT, UDPMessageBox.Subsystem.SCHEDULER);
        this.gui = gui;

        loadFromFile(fileName);
    }

    // Testing constructor (no GUI, with file)
    public FireIncidentSubsystem(String fileName) {
        this(fileName, null);
    }
    // Testing constructor (no GUI, no file)
    public FireIncidentSubsystem() {
        incomingMessageBox  = new UDPMessageBox(UDPMessageBox.Subsystem.FIRE_INCIDENT, UDPMessageBox.Subsystem.VOID);
        schedulerMessageBox = new UDPMessageBox(UDPMessageBox.Subsystem.FIRE_INCIDENT, UDPMessageBox.Subsystem.SCHEDULER);
        this.gui = null;
    }

    @Override
    public void run() {
        for (String event : fileEvents) {
            //parse the event CSV
            FireEvent fireEvent = parseFireEvent(event);
            if(fireEvent == null) continue;

            //convert object into serialized string
            String serializedEvent = fireEvent.serialize();
            System.out.println("Serialized: " + serializedEvent);

            //create a Message for the Scheduler -> DroneSubsystem
            Message fireEventMessage = new Message(
                    "Scheduler",
                    "FireIncidentSubsystem",
                    serializedEvent,
                    Message.MessageType.FireEvent
            );

            // Update GUI with fire status
            if (gui != null) {
                gui.fireStatusChange(fireEvent.getZoneId(), fireEvent.getSeverity());
            }

            schedulerMessageBox.putMessage(fireEventMessage);

            try {
                Thread.sleep(3000); // wait between events
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
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
