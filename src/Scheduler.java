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

import java.io.BufferedReader;
import java.io.FileReader;

class Scheduler implements Runnable {
    private MessageBox fireBox;
    private MessageBox droneBox;

    public Scheduler(MessageBox fire_scheduler, MessageBox scheduler_drone) {
        this.fireBox = fire_scheduler;
        this.droneBox = scheduler_drone;
    }

    public void run() {
        while (true) {
            // Wait for fire event
            FireEvent task = fireBox.getTask();

            if (task != null) {
                System.out.println("[SCHEDULER] FIRE -> DRONE");
                droneBox.putEvent(task);
                
                FireEvent fireUpdate = droneBox.waitForResponse();

                System.out.println("[SCHEDULER] DRONE -> FIRE");
                fireBox.sendResult(fireUpdate);

                task = null;
            }
        }
    }
}
