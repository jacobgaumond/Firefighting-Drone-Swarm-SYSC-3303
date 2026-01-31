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
    private Scheduler schedulerTable;
    private String filePath;

    public FireIncidentSystem(Scheduler table, String filePath) {
        this.schedulerTable = table;
        this.filePath = System.getProperty("user.dir") + filePath;
    }

    public void run() {
        // waits for main init
        Thread listener = new Thread(() -> {
            while (true) {
                FireEvent completed = schedulerTable.waitForResponse();
                if (completed != null) {
                    System.out.println("[FIRE_SUB]: Zone " + completed.getZoneId() + " is cleared.\n\n");
                }
            }
        });
        listener.setDaemon(true);
        listener.start();

        // Main Loop places fire events from the file
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            // get event data from Sample_event_file.csv
            String line = reader.readLine(); // skip header

            while ((line = reader.readLine()) != null) {
                String[] p = line.split(",");
                FireEvent event = new FireEvent(p[0], Integer.parseInt(p[1]), p[2], p[3]);

                System.out.println("[FIRE_SUB] " + event);

                // adds event to the table
                schedulerTable.putEvent(event);
                
                Thread.sleep(500); // small delay
            }
            reader.close();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
        System.exit(0);
    }
}