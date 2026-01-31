import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;

/**
 * Iteration #1: Fire Incident System
 * Uses Scheduler as a table FireIncident signals a fire to the table,
 * table sends it to the dronesystem, dronesystem, tells the scheduler it's been
 * handled
 * sleeps are put in to keep readability however will be able to happen asynchronously as
 * nothing is waiting to receive a messaage
 */


class FireEvent {
    private String time;
    private int zoneId;
    private String type;
    private String severity;
    private boolean firehandled = false;

    public FireEvent(String time, int zoneId, String type, String severity) {
        this.time = time;
        this.zoneId = zoneId;
        this.type = type;
        this.severity = severity;
    }

    public void fireHandled(boolean status) { this.firehandled = status; }
    public int getZoneId() { return zoneId; }
    public String getTime(){return time;}

    @Override
    public String toString() {
        return String.format("[%s] Zone: %d | Type: %s | Severity: %s | Status: %s",
                time, zoneId, type, severity, firehandled ? "EXTINGUISHED" : "PENDING");
    }
}

// Scheduler Table
class SchedulerTable {

    private FireEvent currentTask = null;
    private boolean taskAvailable = false;
    private boolean responseAvailable = false;

    // Called by Fire Incident System (Producer)
    public synchronized void putEvent(FireEvent event) {
        // Wait if there is an unprocessed task or an unread response on the table
        while (taskAvailable || responseAvailable) {
            try { wait(); } catch (InterruptedException e) { return; }
        }
        this.currentTask = event;
        this.taskAvailable = true;
        //System.out.println("[SCHEDULER] Event received for Zone " + event.getZoneId());
        notifyAll();
    }

    // Called by Drone (Consumer/Worker)
    public synchronized FireEvent getTask() {
        while (!taskAvailable) {
            try { wait(); } catch (InterruptedException e) { return null; }
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
            try { wait(); } catch (InterruptedException e) { return null; }
        }
        FireEvent response = currentTask;
        responseAvailable = false;
        currentTask = null; // Clear the table for the next event
        notifyAll();
        return response;
    }
}

// --- FIRE INCIDENT SYSTEM (Client Thread) ---
class FireIncidentSystem implements Runnable {
    private SchedulerTable schedulerTable;
    private String fileName;

    public FireIncidentSystem(SchedulerTable table, String fileName) {
        this.schedulerTable = table;
        this.fileName = fileName;
    }

    public void run() {
        //waits for
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

        //Main Loop places fire events from the file
        try {
            File file = new File(fileName);
            Scanner reader = new Scanner(file);

            while (reader.hasNextLine()) {
                String line = reader.nextLine();
                if (line.trim().isEmpty()) continue;

                String[] p = line.split("\\s*,\\s*");
                FireEvent event = new FireEvent(p[0], Integer.parseInt(p[1]), p[2], p[3]);

                System.out.println("[FIRE_SUB] " + event);

                //adds event to the table
                schedulerTable.putEvent(event);

                Thread.sleep(500); //Create small delay
            }
            reader.close();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
        System.exit(0);
    }
}

// --- DRONE SYSTEM
class DroneSystem implements Runnable {
    private SchedulerTable schedulerTable;
    private String droneName;

    public DroneSystem(SchedulerTable table, String name) {
        this.schedulerTable = table;
        this.droneName = name;
    }
    public void run() {
        while (true) {
            FireEvent task = schedulerTable.getTask();
            if (task != null) {
                System.out.println("[" + droneName + "] DISPATCHING_DRONE to zone " + task.getZoneId()+"|"+task.getTime());
                try { Thread.sleep(200); } catch (InterruptedException e) {}//simulates drone dispatch time

                task.fireHandled(true);  //sets status of Fire
                schedulerTable.sendResult(task); //sends it back to the scheduler
            }
        }
    }
}

//Main
public class ThreadDrone_It1 {

    public static void main(String[] args) {

        SchedulerTable schedulerTable = new SchedulerTable();

        //input test file
        String inputFileName = "src/fire_events.txt";

        Thread fireSys = new Thread(new FireIncidentSystem(schedulerTable, inputFileName), "FireSubThread");
        Thread droneSys = new Thread(new DroneSystem(schedulerTable, "Drone-Alpha"), "DroneThread");

        droneSys.setDaemon(true);
        fireSys.start();
        droneSys.start();
    }
}