/* DroneSubsystem.java
 *
 * This project uses the Client-Server model.
 *
 * This class represents a CLIENT in the Firefighting Drone Swarm.
 *
 * The DroneSubsystem receives packets from:
 *     Scheduler:  events (Time, Zone ID, Event type, Severity)
 *
 * The DroneSubsystem sends packets to:
 *     Scheduler:  updates on events and drone statuses
 */

class DroneSystem implements Runnable {
    private static final int TRAVEL_TIME = 15; // 15 m/s
    private static final int OPERATE_NOZZLE_DOORS_TIME = 1; // 1s
    private static final int DROPPING_RATE = 250; // 250 mL/s
    private static final int TAKE_OFF_ACCELERATION = 3; // 2-4 m/s²

    private Scheduler schedulerTable;
    private String droneName;

    public DroneSystem(Scheduler table, String name) {
        this.schedulerTable = table;
        this.droneName = name;
    }

    public void run() {
        while (true) {
            FireEvent task = schedulerTable.getTask();

            if (task != null) {
                System.out.println("[" + droneName + "] DISPATCHING_DRONE to zone " + task.getZoneId() + "|" + task.getTime());

                try {
                    Thread.sleep(200); // simulates drone dispatch time
                } catch (InterruptedException e) {
                }

                task.fireHandled(true);  // sets status of Fire
                schedulerTable.sendResult(task); // sends it back to the scheduler
            }
        }
    }
}
