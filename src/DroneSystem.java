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

    private MessageBox schedulerBox;
    private String droneName;

    public DroneSystem(MessageBox schedulerBox, String name) {
        this.schedulerBox = schedulerBox;
        this.droneName = name;
    }

    public void run() {
        while (true) {
            FireEvent task = schedulerBox.getTask();

            if (task != null) {
                System.out.println("[DRONE] " + droneName + " to Zone: " + task.getZoneId());

                try {
                    Thread.sleep(200); // simulates drone dispatch time
                } catch (InterruptedException e) {
                }

                System.out.println("[DRONE] Zone " + task.getZoneId() + " fire extinguished");

                // update FireEvent
                task.fireHandled(true);
                schedulerBox.sendResult(task);
                task = null;
            }
        }
    }
}
