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

class Scheduler {
    private FireEvent currentTask = null;
    private boolean taskAvailable = false;
    private boolean responseAvailable = false;

    // Called by Fire Incident System (Producer)
    public synchronized void putEvent(FireEvent event) {
        // Wait if there is an unprocessed task or an unread response on the table
        while (taskAvailable || responseAvailable) {
            try {
                wait();
            } catch (InterruptedException e) {
                return;
            }
        }
        this.currentTask = event;
        this.taskAvailable = true;
        // System.out.println("[SCHEDULER] Event received for Zone " + event.getZoneId());
        notifyAll();
    }

    // Called by Drone (Consumer/Worker)
    public synchronized FireEvent getTask() {
        while (!taskAvailable) {
            try {
                wait();
            } catch (InterruptedException e) {
                return null;
            }
        }
        FireEvent task = currentTask;
        taskAvailable = false;
        return task;
    }

    // Sends Drone Result
    public synchronized void sendResult(FireEvent event) {
        this.currentTask = event;
        this.responseAvailable = true;
        notifyAll();
    }

    // Confirms Fire is extinguished
    public synchronized FireEvent waitForResponse() {
        while (!responseAvailable) {
            try {
                wait();
            } catch (InterruptedException e) {
                return null;
            }
        }
        FireEvent response = currentTask;
        responseAvailable = false;
        currentTask = null; // Clear the table for the next event
        notifyAll();
        return response;
    }
}
