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

    private final UDPMessageBox messageBox;
    private final ArrayList<String> fileEvents = new ArrayList<>();

    public static void main(String[] args) {
        String zoneFileName = "src/data/Sample_zone_file.csv";
        ZoneMap.loadZones(zoneFileName);

        String inputFileName = "src/data/Sample_event_file.csv";
        Thread fireIncidentSubsystem = new Thread(new FireIncidentSubsystem(inputFileName), "FireIncidentSubsystemThread");

        fireIncidentSubsystem.start();
    }

    public FireIncidentSubsystem(String fileName) {
        messageBox = new UDPMessageBox(UDPMessageBox.Subsystem.FIRE_INCIDENT);
        if (fileName != null) {
            loadFromFile(fileName);
        }
    }

    // Testing constructor (no file)
    public FireIncidentSubsystem() {
        messageBox = new UDPMessageBox(UDPMessageBox.Subsystem.FIRE_INCIDENT);
    }

    public void closeBox() {
        messageBox.closeBox();
    }

    @Override
    public void run() {
        // get all events from file
        ArrayList<FireEvent> fireEvents = new ArrayList<>();
        for (String event : fileEvents) {
            FireEvent fireEvent = FireEvent.parseFromCsv(event);
            if (fireEvent != null) {
                fireEvents.add(fireEvent);
            }
        }

        if (fireEvents.isEmpty()) {
            return; // no events
        }

        // Find starting timestamp (first fire)
        long startTime = fireEvents.get(0).getTimeInSeconds();

        // Dispatch events
        for (FireEvent fireEvent : fireEvents) {
            // TODO: broken
            // respect delay between timestamps (accounting for simulation speed)
            long delay = (long) ((fireEvent.getTimeInSeconds() - startTime) * (1000.0 / SimulationEnvironment.SIMULATION_SPEED));
            try {
                Thread.sleep(delay / 2);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            String serializedEvent = fireEvent.serialize();
            Message fireEventMessage = new Message(
                    "Scheduler",
                    "FireIncidentSubsystem",
                    serializedEvent,
                    Message.MessageType.FireEvent
            );
            messageBox.putMessage(fireEventMessage, UDPMessageBox.SCHEDULER_PORT);
        }

        boolean boxOpen = true;
        while (boxOpen) {
            Message message = messageBox.getMessage();
            if (message == null) {
                boxOpen = false;
            } else {
                System.out.println("[FireIncidentSubsystem] Received from " + message.getSourceName() + ": " + message.getMessageData());
                if (!message.getMessageData().equals("Acknowledged")) {
                    message = new Message("DroneSubsystem", "FireIncidentSubsystem", "Acknowledged", Message.MessageType.FireEvent);
                    System.out.println("[FireIncidentSubsystem] Sending to DroneSubsystem, through Scheduler: " + message.getMessageData());
                    messageBox.putMessage(message, UDPMessageBox.SCHEDULER_PORT);
                }
            }
        }
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
        messageBox.putMessage(message, UDPMessageBox.SCHEDULER_PORT);
    }
}
