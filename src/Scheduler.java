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

//import java.io.*;
//import java.net.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.LinkedList;


public class Scheduler implements Runnable {
//    SocketWrapper serverSocket;
//
//    public final static int SCHEDULER_PORT = 9500;
//
//    public Scheduler() {
//        try {
//            serverSocket = new SocketWrapper(SCHEDULER_PORT);
//        } catch (SocketException e) {
//            e.printStackTrace();
//            System.exit(1);
//        }
//    }

    private MessageBox incomingMessageBox;
    private MessageBox fireIncidentMessageBox;
    private MessageBox droneMessageBox;

    private Queue<FireEvent> taskQueue =  new LinkedList<>();
    private Map<Integer, DroneInfo> droneRegistry = new HashMap<>();
    private Map<Integer, FireTask> activeFires = new HashMap<>();
    private static class DroneInfo {
        int droneId;
        String state;
        int x, y, fluid, battery;
        int assignedZoneID =-1;
        public DroneInfo(int droneId) {
            this.droneId = droneId;
            this.state = "IDLE";
            this.x = 0;
            this.y = 0;
            this.fluid = 15;
            this.battery = 1000;
        }
        public boolean isAvailable() {
            return state.equals("IDLE") || state.equals("EN_ROUTE_BASE");
        }
    }

        private static class FireTask {
            FireEvent fireEvent;
            int fluidRequired;
            int fluidDropped;

            public FireTask(FireEvent fireEvent, int fluidRequired) {
                this.fireEvent = fireEvent;
                this.fluidRequired = fluidRequired;
                this.fluidDropped = 0;
            }

            public boolean isExtinguished() {
                return fluidDropped >= fluidRequired;
            }

            public int remainingFluidNeeded() {
                return fluidRequired - fluidDropped;
            }
        }



    public Scheduler(MessageBox incomingMessageBox, MessageBox fireIncidentMessageBox, MessageBox droneMessageBox) {
        this.incomingMessageBox     = incomingMessageBox;
        this.fireIncidentMessageBox = fireIncidentMessageBox;
        this.droneMessageBox        = droneMessageBox;
    }

    @Override
    public void run() {
        boolean boxOpen = true;
        do {
            Message message = incomingMessageBox.getMessage();
            if (message == null) {
                boxOpen = false;

//                // In case one or the other is still open...
//                incomingMessageBox.closeBox();
//                fireIncidentMessageBox.closeBox();
//                droneMessageBox.closeBox();
            }
            else {
//                System.out.println("[Scheduler] Received from " + message.getSourceName() + ": " + message.getMessageData());
//
//                if (message.getDestinationName().equals("FireIncidentSubsystem")) {
//                    System.out.println("[Scheduler] Sending to FireIncidentSubsystem: " + message.getMessageData());
//                    fireIncidentMessageBox.putMessage(message);
//                }
//                else if (message.getDestinationName().equals("DroneSubsystem")) {
//                    System.out.println("[Scheduler] Sending to DroneSubsystem: " + message.getMessageData());
//                    droneMessageBox.putMessage(message);
//                }



                processIncomingMessageBox(message);

            }
        } while (boxOpen);
    }

//** MESSAGE ROUTING **//
//Processes all incoming messages and sends to desination
    private void processIncomingMessageBox(Message message) {
        System.out.println("\n[Scheduler] Received from " + message.getSourceName() + ": \n" + message.getMessageData());


        switch (message.getDestinationName()) {
            case "DroneSubsystem":
                if (message.getMessageType() == Message.MessageType.DroneRegistration) {
                    int id = Integer.parseInt(message.getMessageData());
                    registerDrone(id);
                    return;
                }
                else processDroneMessage(message);
                break;
            case "FireIncidentSubsystem":
                processEvent(message);
                break;
            case "Scheduler":
                //checks if the incoming message is a fire task
                if (message.getMessageType() == Message.MessageType.FireEvent) {
                    processEvent(message); //assign or queue task
                } else if (message.getMessageType() == Message.MessageType.DroneResponse) {
                    processDroneMessage(message); //drone finished a task
                }
                else if (message.getMessageType() == Message.MessageType.DroneRegistration) {
                    int id = Integer.parseInt(message.getMessageData());
                    registerDrone(id);
                }
                break;
            default:
                System.out.println("[Scheduler] Unknown destination: " + message.getDestinationName());
        }

    }

    public void registerDrone(int droneId) {
        droneRegistry.put(droneId, new DroneInfo(droneId));
        System.out.println("[Scheduler] Registered drone " + droneId);
    }


    // -------------------------
    // Task Assignment
    // -------------------------

    private void tryAssignTask() {
        while (!taskQueue.isEmpty()) {
            FireEvent next = taskQueue.peek();
            DroneInfo available = findAvailableDrone(next);
            if (available != null) {
                taskQueue.poll();
                assignFireTaskToDrone(available, next);
            } else {
                break; // no drones available, wait
            }
        }
    }
    private void assignFireTaskToDrone(DroneInfo drone, FireEvent fireEvent) {
        FireTask fireTask = activeFires.computeIfAbsent(
                fireEvent.getZoneId(),
                id -> new FireTask(fireEvent, getAmountNeededForFire(fireEvent.getSeverity()))
        );
        int amountToDrop = Math.min(drone.fluid, fireTask.remainingFluidNeeded());
        drone.assignedZoneID = fireEvent.getZoneId();
        drone.state = "EN_ROUTE_FIRE";

        DroneRequest request = new DroneRequest(
                DroneEvent.FIRE_ASSIGNED,
                fireEvent.getTime(),
                fireEvent.getZoneId(),
                fireEvent.getEventType(),
                fireEvent.getSeverity(),
                fireEvent.getTargetX(),
                fireEvent.getTargetY(),
                amountToDrop,
                drone.droneId  // assign to specific drone
        );


        Message droneMessage = new Message(
                "DroneSubsystem",
                "Scheduler",
                request.serialize(),
                Message.MessageType.DroneRequest
        );
        droneMessageBox.putMessage(droneMessage);
    }

    //handles if the (task finished) -> assigns next task in the queue or marks drone as idle
    private void processDroneMessage(Message message) {
        String[] parts = message.getMessageData().split("~");

        int droneId = Integer.parseInt(parts[0]);
        String state = parts[1];
        int x = Integer.parseInt(parts[2]);
        int y = Integer.parseInt(parts[3]);
        int fluid = Integer.parseInt(parts[4]);
        int battery = Integer.parseInt(parts[5]);
        int fluidDroppedThisRun = Integer.parseInt(parts[6]);

        // Update drone registry
        DroneInfo drone = droneRegistry.get(droneId);
        if (drone == null) {
            System.out.println("[Scheduler] Unknown drone ID: " + droneId + ", registering.");
            drone = new DroneInfo(droneId);
            droneRegistry.put(droneId, drone);
        }

        drone.state = state;
        drone.x = x;
        drone.y = y;
        drone.fluid = fluid;
        drone.battery = battery;

        System.out.println("[Scheduler] Drone " + droneId + " status: " + state +
                " | pos(" + x + "," + y + ") | fluid: " + fluid + " | battery: " + battery);

        switch (state) {
            case "ARRIVED_AT_FIRE":
                sendEventToDrone(droneId, DroneEvent.EXTINGUISH_REQUEST, "");
                break;

            case "FIRE_HANDLED":
                FireTask activeTask = activeFires.get(drone.assignedZoneID);
                if (activeTask != null) {
                    activeTask.fluidDropped += fluidDroppedThisRun;
                    if (activeTask.isExtinguished()) {
                        System.out.println("[Scheduler] Zone " + activeTask.fireEvent.getZoneId() + " EXTINGUISHED!");
                        activeFires.remove(activeTask.fireEvent.getZoneId());
                    } else {
                        System.out.println("[Scheduler] Zone " + activeTask.fireEvent.getZoneId() +
                                " still needs " + activeTask.remainingFluidNeeded() + " more fluid, requeueing.");
                        taskQueue.add(activeTask.fireEvent);
                    }
                }
                drone.assignedZoneID = -1;
                sendEventToDrone(droneId, DroneEvent.RETURN_BASE_REQUEST, "");
                break;

            case "IDLE":
                tryAssignTask();
                break;

            case "FAULTED":
                System.out.println("[Scheduler] Drone " + droneId + " has faulted!");
                break;
        }
    }

    private void sendEventToDrone(int droneId, DroneEvent event, String payload) {
        DroneRequest request = new DroneRequest(
                event, "", 0, "", "", 0, 0, 0, droneId
        );
        Message message = new Message(
                "DroneSubsystem",
                "Scheduler",
                request.serialize(),
                Message.MessageType.DroneRequest
        );
        System.out.println("[Scheduler] Sending " + event + " to Drone " + droneId);
        droneMessageBox.putMessage(message);
    }

    private void processEvent(Message message) {
        if (message.getMessageType() != Message.MessageType.FireEvent) return;

        FireEvent fireEvent = new FireEvent(message.getMessageData());
        System.out.println("[Scheduler] Handling fire event: " + fireEvent);
        taskQueue.add(fireEvent);
        tryAssignTask();
    }


    //HELPER FUNCTIONS
    private int getAmountNeededForFire(String severity) {
        switch (severity.toLowerCase()) {
            case "high":    return 30;
            case "moderate": return 20;  // adjust these
            case "low":   return 10;
            default:
                System.out.println("[Scheduler] Unknown severity: " + severity + ", defaulting to 10");
                return 0;
        }
    }
    // Find best available drone for the task
    private DroneInfo findAvailableDrone(FireEvent fireEvent) {
        DroneInfo best = null;
        double bestDistance = Double.MAX_VALUE;

        for (DroneInfo drone : droneRegistry.values()) {
            if (drone.state.equals("IDLE")) {
                best = drone;
            }
        }
        return best;
    }


    // -------------------------
    // Getters
    // -------------------------

    public Queue<FireEvent> getTaskQueue() { return taskQueue; }
    public Map<Integer, DroneInfo> getDroneRegistry() { return droneRegistry; }
    public Map<Integer, FireTask> getActiveFires() { return activeFires; }

}
