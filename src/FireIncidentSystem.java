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

import java.io.BufferedReader;
import java.io.FileReader;

class FireIncidentSystem implements Runnable {
    private MessageBox schedulerBox;
    private String filePath;

    public FireIncidentSystem(MessageBox fire_scheduler, String filePath) {
        this.schedulerBox = fire_scheduler;
        this.filePath = System.getProperty("user.dir") + filePath;
    }

    public void run() {
        // Main Loop places fire events from the file
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line = reader.readLine(); // skip header

            // get event data from Sample_event_file.csv
            while ((line = reader.readLine()) != null) {

                String[] parts = line.split(",");

                String time = parts[0];
                int zoneId = Integer.parseInt(parts[1]);
                String type = parts[2];
                String severity = parts[3];

                FireEvent event = new FireEvent(time, zoneId, type, severity);

                System.out.println("\n" + "[FIRE] " + event);

                // adds event to the scheduler
                schedulerBox.putEvent(event);
                FireEvent fireUpdate = schedulerBox.waitForResponse();

                System.out.println("[FIRE] " + fireUpdate);

                Thread.sleep(500); // small delay
            }
            reader.close();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
        System.exit(0);
    }
}