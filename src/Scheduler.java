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


    //Message Boxes//
    private MessageBox incomingMessageBox;
    private MessageBox fireIncidentMessageBox;
    private MessageBox droneMessageBox;

    private final SchedulerStateMachine schedulerSM = new SchedulerStateMachine();

    //Data Tracking//
    private Queue<FireEvent> taskQueue = new LinkedList<>();
    private Map<Integer, DroneInfo> droneRegistry = new HashMap<>();
    private Map<Integer, FireTask> activeFires = new HashMap<>();

    //Constructor//
    public Scheduler(MessageBox incomingMessageBox, MessageBox fireIncidentMessageBox, MessageBox droneMessageBox) {
        this.incomingMessageBox = incomingMessageBox;
        this.fireIncidentMessageBox = fireIncidentMessageBox;
        this.droneMessageBox = droneMessageBox;
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
            } else {
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
        System.out.println("\n[Scheduler] Received from " + message.getSourceName() + ": " + message.getMessageData());

        if (message.getMessageType() == Message.MessageType.DroneRegistration) {
            registerDrone(Integer.parseInt(message.getMessageData()));
            return;
        }

        if (message.getMessageType() == Message.MessageType.DroneResponse) {
            processDroneMessage(message);
            return;
        }

        if (message.getMessageType() == Message.MessageType.FireEvent) {
            processEvent(message);
            return;
        }

        System.out.println("[Scheduler] Unknown destination: " + message.getDestinationName());
    }


    // -------------------------
    // Drone and Task Logic
    // -------------------------

    public void registerDrone(int droneId) {
        droneRegistry.put(droneId, new DroneInfo(droneId));
        System.out.println("[Scheduler] Registered drone " + droneId);
    }

    public void tryAssignTask() {
        while (!taskQueue.isEmpty()) {
            FireEvent next = taskQueue.peek();
            DroneInfo available = findAvailableDrone(next);
            if (available != null) {
                taskQueue.poll();
                assignFireTaskToDrone(available, next);
            } else {
                schedulerSM.handleEvent(SchedulerEvent.NOT_ENOUGH_DRONES_AVAILABLE, this);
                break; // no drones available, wait
            }
        }
        if (taskQueue.isEmpty() && !activeFires.isEmpty()) {
            boolean allWillBeExtinguished = activeFires.values().stream()
                    .allMatch(this::willBeExtinguishedByAssignedDrones);
            if (allWillBeExtinguished) {
                schedulerSM.handleEvent(SchedulerEvent.DRONES_AVAILABLE, this);
            } else {
                schedulerSM.handleEvent(SchedulerEvent.NOT_ENOUGH_DRONES_AVAILABLE, this);
            }
        }

    }

    private void assignFireTaskToDrone(DroneInfo drone, FireEvent fireEvent) {
        FireTask fireTask = activeFires.computeIfAbsent(
                fireEvent.getZoneId(),
                id -> new FireTask(fireEvent, getRequiredFluid(fireEvent.getSeverity()))
        );
        int amountToDrop = Math.min(drone.fluid, fireTask.remainingFluidNeeded());
        drone.assignedZoneID = fireEvent.getZoneId();
        drone.state = "EN_ROUTE_FIRE";
        drone.fluidAssigned = amountToDrop;
        System.out.println("Drone will drop" + drone.fluidAssigned);

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
        DroneResponse status = new DroneResponse(message.getMessageData());

        // Update drone registry
        DroneInfo drone = droneRegistry.get(status.getDroneID());
        if (drone == null) {
            drone = new DroneInfo(status.getDroneID());
            droneRegistry.put(status.getDroneID(), drone);
        }

        drone.state = status.getState();
        drone.x = status.getX();
        drone.y = status.getY();
        drone.fluid = status.getFluidAmount();
        drone.battery = status.getBattery();

        System.out.println("[Scheduler] " + status);

        switch (drone.state) {
            case "ARRIVED_AT_FIRE":
                sendEventToDrone(status.getDroneID(), DroneEvent.EXTINGUISH_REQUEST, "");
                break;

            case "FIRE_HANDLED":
                FireTask activeTask = activeFires.get(drone.assignedZoneID);
                if (activeTask != null) {
                    activeTask.fluidDropped += status.getFluidDropped();
                    if (activeTask.isExtinguished()) {
                        System.out.println("[Scheduler] Zone " + activeTask.fireEvent.getZoneId() + " EXTINGUISHED!");
                        activeFires.remove(activeTask.fireEvent.getZoneId());

                        if (taskQueue.isEmpty() && activeFires.isEmpty()) {
                            schedulerSM.handleEvent(SchedulerEvent.ALL_FIRES_EXTINGUISHED, this);
                        }
                    } else {
                        System.out.println("[Scheduler] Zone " + activeTask.fireEvent.getZoneId() +
                                " still needs " + activeTask.remainingFluidNeeded() + " more fluid, requeueing.");
                        taskQueue.add(activeTask.fireEvent);
                    }
                }
                drone.assignedZoneID = -1;
                sendEventToDrone(status.getDroneID(), DroneEvent.RETURN_BASE_REQUEST, "");
                break;

            case "IDLE":
                tryAssignTask();
                break;

            case "FAULTED":
                System.out.println("[Scheduler] Drone " + status.getDroneID() + " has faulted!");
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
        //tryAssignTask();
        schedulerSM.handleEvent(SchedulerEvent.FIRE_EVENT, this);
    }


    // -- Utiliy Functions --//
    private DroneInfo findAvailableDrone(FireEvent fireEvent) {
        DroneInfo best = null;
        double bestDistance = Double.MAX_VALUE;
        for (DroneInfo drone : droneRegistry.values()) {
            if (drone.canHandleTask(fireEvent)) {
                best = drone;
            }
        }
        return best;
    }

    private boolean willBeExtinguishedByAssignedDrones(FireTask fireTask) {
        int totalFluidEnRoute = droneRegistry.values().stream()
                .filter(d -> d.assignedZoneID == fireTask.fireEvent.getZoneId())
                .mapToInt(d -> d.fluidAssigned)
                .sum();
        System.out.println("[Scheduler] Zone " + fireTask.fireEvent.getZoneId() +
                " | totalFluidEnRoute: " + totalFluidEnRoute +
                " | remaining: " + fireTask.remainingFluidNeeded());
        return totalFluidEnRoute >= fireTask.remainingFluidNeeded();
    }

    private int getRequiredFluid(String severity) {
        return switch (severity.toLowerCase()) {
            case "high" -> 30;
            case "moderate" -> 20;
            default -> 10;
        };
    }

    //--Getters and Setters --//-

    public Queue<FireEvent> getTaskQueue() {
        return taskQueue;
    }

    public Map<Integer, DroneInfo> getDroneRegistry() {
        return droneRegistry;
    }

    public Map<Integer, FireTask> getActiveFires() {
        return activeFires;
    }


    //--Inner Classes DroneInfo and FireTask --//

    private static class DroneInfo {
        int droneId;
        String state;
        int x, y, fluid, fluidAssigned, battery;
        int assignedZoneID = -1;

        public DroneInfo(int droneId) {
            this.droneId = droneId;
            this.state = "IDLE";
            this.x = 0;
            this.y = 0;
            this.fluid = 15;
            this.battery = 1000;
            this.fluidAssigned = 0;
        }

        public boolean canHandleTask(FireEvent fire) {
            boolean isRightState = state.equals("IDLE") || state.equals("EN_ROUTE_BASE") || state.equals("FIRE_HANDLED");
            if (!isRightState) return false;

            // 1. Fluid Check: Does it have enough to even make a dent?
            if (this.fluid <= 0) return false;

            // 2. Battery Check: Current -> Fire -> Base
           /* int distToFire = calculateDistance(this.x, this.y, fire.getTargetX(), fire.getTargetY());
            int distBackToBase = calculateDistance(fire.getTargetX(), fire.getTargetY(), 0, 0);
            int totalDistanceRequired = distToFire + distBackToBase;

            return this.battery >= totalDistanceRequired;*/
            return true;
        }
        /*
        private int calculateDistance(int x1, int y1, int x2, int y2) {
            return (int) Math.ceil(Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2)));
        }
        */

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


}
