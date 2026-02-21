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
    private static class DroneInfo {
        int droneId;
        String state;
        int x, y, fluid, battery;

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

    public Scheduler(MessageBox incomingMessageBox, MessageBox fireIncidentMessageBox, MessageBox droneMessageBox) {
        this.incomingMessageBox     = incomingMessageBox;
        this.fireIncidentMessageBox = fireIncidentMessageBox;
        this.droneMessageBox        = droneMessageBox;
    }

    public void registerDrone(int droneId) {
        droneRegistry.put(droneId, new DroneInfo(droneId));
        System.out.println("[Scheduler] Registered drone " + droneId);
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


    private void assignTaskToDrone(DroneInfo drone, FireEvent fireEvent) {
        DroneRequest request = new DroneRequest(
                DroneEvent.FIRE_ASSIGNED,
                fireEvent.getTime(),
                fireEvent.getZoneId(),
                fireEvent.getEventType(),
                fireEvent.getSeverity(),
                fireEvent.getTargetX(),
                fireEvent.getTargetY(),
                10,
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

    private int getAmountNeededForFire(String severity) {
        switch (severity.toLowerCase()) {
            case "high":    return 30;
            case "moderate": return 20;  // adjust these
            case "low":   return 10;
            default:
                System.out.println("[Scheduler] Unknown severity: " + severity + ", defaulting to 10");
                return 10;
        }
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
                sendToDrone(droneId, DroneEvent.EXTINGUISH_REQUEST, "");
                break;

            case "FIRE_HANDLED":
                if (!taskQueue.isEmpty()) {
                    assignTaskToDrone(drone, taskQueue.poll());
                } else {
                    sendToDrone(droneId, DroneEvent.RETURN_BASE_REQUEST, "");
                }
                break;

            case "IDLE":
                if (!taskQueue.isEmpty()) {
                    assignTaskToDrone(drone, taskQueue.poll());
                }
                break;

            case "FAULTED":
                System.out.println("[Scheduler] Drone " + droneId + " has faulted!");
                break;
        }
    }

    private void sendToDrone(int droneId, DroneEvent event, String payload) {
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

        DroneInfo available = findAvailableDrone(fireEvent);
        if (available != null) {
            assignTaskToDrone(available, fireEvent);
            System.out.println("assigning available drone");
        } else {
            taskQueue.add(fireEvent);
            System.out.println("[Scheduler] No drones available, task queued. Queue size: " + taskQueue.size());
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

    public Queue<FireEvent> getTaskQueue() {
        return taskQueue;
    }


}
