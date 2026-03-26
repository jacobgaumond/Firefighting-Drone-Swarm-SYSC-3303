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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class Scheduler implements Runnable {

    private DroneGUI gui;
    private UDPMessageBox messageBox;

    private final SchedulerStateMachine schedulerSM = new SchedulerStateMachine();
    private static final double MS_PER_UNIT = 0.15;
    private boolean clockStarted = false;

    // State Tracking
    private final Queue<FireEvent> taskQueue = new LinkedList<>();
    private final Map<Integer, DroneInfo> droneRegistry = new HashMap<>();
    private final Map<Integer, FireTask> activeFires = new HashMap<>();

    public static void main(String[] args) {
        DroneGUI gui = new DroneGUI();
        gui.setVisible(true);
        Thread scheduler = new Thread(new Scheduler(gui), "SchedulerThread");
        scheduler.start();
    }

    // Constructor
    public Scheduler(DroneGUI gui) {
        this.gui = gui;
        messageBox = new UDPMessageBox(UDPMessageBox.Subsystem.SCHEDULER);
    }

    // Testing constructor (no GUI)
    public Scheduler() {
        this(null);
    }

    public void closeBox() {
        if (messageBox != null) {
            messageBox.closeBox();
        }
    }

    @Override
    public void run() {
        boolean boxOpen = true;

        while (boxOpen) {
            Message message = messageBox.getMessage();
            if (message == null) {
                boxOpen = false;
            } else {
                processIncomingMessage(message);
            }
        }
    }

    // ========== Message Routing ==========
    private void processIncomingMessage(Message message) {
        if (message.getMessageType() == Message.MessageType.DroneRegistration) {
            registerDrone(message);
        }
        if (message.getMessageType() == Message.MessageType.FireEvent) {
            processFireEvent(message);
        }
        if (message.getMessageType() == Message.MessageType.DroneResponse) {
            processDroneMessage(message);
        }
    }

    public void registerDrone(Message message) {
        String[] parts = message.getMessageData().split(",");
        int droneId = Integer.parseInt(parts[0]); // id
        int dronePort = Integer.parseInt(parts[1]); // port

        DroneInfo droneInfo = new DroneInfo(droneId, dronePort);
        droneRegistry.put(droneId, droneInfo);

        System.out.println("[SCHEDULER] Registered drone " + droneId + " (PORT: " + dronePort + ")");
        if (gui != null) {
            gui.createDroneLabel(droneId);
        }
    }

    public void processFireEvent(Message message) {
        FireEvent fireEvent = new FireEvent(message.getMessageData());
        taskQueue.add(fireEvent);

        if (gui != null) {
            if (!clockStarted) {
                SimulationEnvironment.startClock(fireEvent.getTimeInSeconds());
                gui.startClockDisplay();
                clockStarted = true;
            }
            gui.fireStatusChange(fireEvent.getZoneId(), fireEvent.getSeverity());
        }

        System.out.println("[SCHEDULER] Handling fire event: " + fireEvent);
        schedulerSM.handleEvent(SchedulerEvent.FIRE_EVENT, this);
    }

    private void processDroneMessage(Message message) {
        // task finished -> assign next in the queue or mark drone as idle
        DroneResponse status = new DroneResponse(message.getMessageData());

        // Update drone registry
        DroneInfo drone = droneRegistry.get(status.getDroneID());
        drone.state = status.getState();
        drone.x = status.getX();
        drone.y = status.getY();
        drone.fluid = status.getFluidAmount();
        drone.battery = status.getBattery();

        switch (drone.state) {
            case "ARRIVED_AT_FIRE":
                sendEventToDrone(status.getDroneID(), DroneEvent.EXTINGUISH_REQUEST, "");
                if (gui != null) {
                    gui.extinguishFire(drone.droneId, drone.assignedZoneID, calculateGuiDroneExtinguishTime(drone.fluidAssigned));
                }
                break;

            case "FIRE_HANDLED":
                FireTask activeTask = activeFires.get(drone.assignedZoneID);

                if (activeTask != null) { //Checks to see if the zoneFire is fully out or not
                    activeTask.fluidDropped += status.getFluidDropped();
                    int zoneId = activeTask.fireEvent.getZoneId();

                    if (activeTask.isExtinguished()) {
                        System.out.println("[Scheduler] Zone " + zoneId + " EXTINGUISHED!");
                        activeFires.remove(zoneId);

                        // Update GUI: extinguished
                        if (gui != null) {
                            gui.fireStatusChange(zoneId, capitalizeSeverity(getFireSeverity(activeTask)));
                        }

                        if (taskQueue.isEmpty() && activeFires.isEmpty()) {
                            schedulerSM.handleEvent(SchedulerEvent.ALL_FIRES_EXTINGUISHED, this);
                        }
                    } else {
                        System.out.println("[Scheduler] Zone " + zoneId
                                + " still needs " + activeTask.remainingFluidNeeded() + " more fluid, requeueing.");

                        if (gui != null) {
                            gui.fireStatusChange(zoneId, capitalizeSeverity(getFireSeverity(activeTask)));
                        }
                    }
                }

                // only assign leftover if the drone actually dropped fluid at this fire
                if (status.getFluidDropped() > 0 && drone.fluid > 0 && !activeFires.isEmpty()) {
                    FireEvent leftOverTarget = findNearbyFire(drone);
                    if (leftOverTarget != null) {
                        System.out.println("[Scheduler] Drone " + drone.droneId + " using leftover water on nearby fire " + leftOverTarget.getZoneId());
                        drone.assignedZoneID = leftOverTarget.getZoneId(); //mark fire
                        drone.state = "EN_ROUTE_FIRE";                     //mark busy
                        assignFireTaskToDrone(drone, leftOverTarget);
                    }
                } else {
                    // no leftover -> return to base
                    drone.assignedZoneID = -1;
                    drone.state = "EN_ROUTE_BASE";
                    if (gui != null) {
                        gui.returnDrone(drone.droneId, calculateGuiDroneTravelTime(drone.x, drone.y, 0, 0));
                    }
                    sendEventToDrone(drone.droneId, DroneEvent.RETURN_BASE_REQUEST, "");
                }
                break;

            case "IDLE":
                tryAssignTask();
                break;

            case "FAULTED":
                System.out.println("[Scheduler] Drone " + status.getDroneID() + " has faulted!");
                break;
        }
    }

    // ========== Drone and Task Logic ==========
    public void tryAssignTask() {
        while (!taskQueue.isEmpty()) {
            FireEvent next = taskQueue.peek();
            DroneInfo available = findAvailableDrone(next);

            if (available != null) {
                assignFireTaskToDrone(available, next);

                if (willBeExtinguishedByAssignedDrones(activeFires.get(next.getZoneId()))) {
                    taskQueue.poll();
                }
            } else {
                schedulerSM.handleEvent(SchedulerEvent.NOT_ENOUGH_DRONES_AVAILABLE, this);
                break; // no drones available, wait
            }
        }
        if (taskQueue.isEmpty() && !activeFires.isEmpty()) {
            boolean allWillBeExtinguished = activeFires.values().stream().allMatch(this::willBeExtinguishedByAssignedDrones);
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
        int amountToDrop = Math.min((int) drone.fluid, fireTask.remainingFluidNeeded());
        drone.assignedZoneID = fireEvent.getZoneId();
        drone.state = "EN_ROUTE_FIRE";
        drone.fluidAssigned = amountToDrop;
        drone.dispatchCount++;

        DroneRequest request = new DroneRequest(
                DroneEvent.FIRE_ASSIGNED,
                fireEvent.getTime(),
                fireEvent.getZoneId(),
                fireEvent.getEventType(),
                fireEvent.getSeverity(),
                fireEvent.getTargetX(),
                fireEvent.getTargetY(),
                amountToDrop,
                drone.droneId, // assign to specific drone
                fireEvent.getFaultType()
        );
        if (gui != null) {
            gui.moveDroneToZone(drone.droneId,
                    fireEvent.getZoneId(),
                    calculateGuiDroneTravelTime(drone.x, drone.y, fireEvent.getTargetX(), fireEvent.getTargetY()),
                    drone.fluid
            ); // TODO time
        }

        Message droneMessage = new Message(
                "DroneSubsystem",
                "Scheduler",
                request.serialize(),
                Message.MessageType.DroneRequest
        );
        fireEvent.setFaultType("NONE");
        messageBox.putMessage(droneMessage, drone.port);
    }

    private void sendEventToDrone(int droneId, DroneEvent event, String payload) {
        DroneInfo drone = droneRegistry.get(droneId);

        DroneRequest request = new DroneRequest(
                event, "", 0, "", "", 0, 0, 0, droneId,"NONE"
        );
        Message message = new Message(
                "DroneSubsystem",
                "Scheduler",
                request.serialize(),
                Message.MessageType.DroneRequest
        );

        messageBox.putMessage(message, drone.port);
    }

    // ========== Helper Functions ==========
    private FireEvent findNearbyFire(DroneInfo drone) {
        FireEvent nearest = null;
        double shortestDistance = Double.MAX_VALUE;

        for (FireTask task : activeFires.values()) {
            if (task.remainingFluidNeeded() <= 0) {
                continue; // skip extinguished

            }
            double distance = calculateDistance(drone.x, drone.y,
                    task.fireEvent.getTargetX(),
                    task.fireEvent.getTargetY());
            if (distance < shortestDistance) {
                shortestDistance = distance;
                nearest = task.fireEvent;
            }
        }

        return nearest;
    }

    private String capitalizeSeverity(String severity) {
        if (severity == null || severity.isEmpty()) {
            return "";
        }
        return severity.substring(0, 1).toUpperCase() + severity.substring(1).toLowerCase();
    }

    private DroneInfo findAvailableDrone(FireEvent fireEvent) {
        DroneInfo best = null;
        int leastDispatches = Integer.MAX_VALUE;
        double shortestDistance = Double.MAX_VALUE;

        // Find least dispatched count
        for (DroneInfo drone : droneRegistry.values()) {
            if (!drone.canHandleTask(fireEvent)) {
                continue;
            }

            if (drone.dispatchCount < leastDispatches) {
                leastDispatches = drone.dispatchCount;
            }
        }

        // Of drones with minimum dispatches, find the nearest to fire that is available
        for (DroneInfo drone : droneRegistry.values()) {
            if (!drone.canHandleTask(fireEvent)) {
                continue;
            }

            if (drone.dispatchCount == leastDispatches) {
                // Get drone's current position
                int x = 0;
                int y = 0;

                if (gui != null) {
                    int[] guiPosition = gui.getCurrentDronePosition(drone.droneId);
                    x = guiPosition[0];
                    y = guiPosition[1];
                }

                double distance = calculateDistance(x, y, fireEvent.getTargetX(), fireEvent.getTargetY());

                if (distance < shortestDistance) {
                    shortestDistance = distance;
                    best = drone; // nearest drone
                }
            }
        }

        // Check for rerouting opportunities for drones in transit to lower severity fires
        if (best == null) {
            for (DroneInfo drone : droneRegistry.values()) {
                if (drone.state.equals("EN_ROUTE_FIRE")) {
                    FireTask currentTask = activeFires.get(drone.assignedZoneID);

                    String currentFireSeverity = currentTask.fireEvent.getSeverity();
                    String newFireSeverity = fireEvent.getSeverity();

                    if (isHigherSeverity(newFireSeverity, currentFireSeverity)) {
                        // Check if this drone can handle the new fire (battery/fuel requirements)
                        if (drone.canHandleTask(fireEvent)) {
                            // Remove drone's assignment from current fire task
                            currentTask.fluidRequired += drone.fluidAssigned;
                            drone.fluidAssigned = 0;

                            best = drone;
                            break; // Take the first available reroutable drone
                        }
                    }
                }
            }
        }

        return best;
    }

    private boolean willBeExtinguishedByAssignedDrones(FireTask fireTask) {
        int totalFluidEnRoute = droneRegistry.values().stream()
                .filter(d -> d.assignedZoneID == fireTask.fireEvent.getZoneId())
                .mapToInt(d -> d.fluidAssigned)
                .sum();

        return totalFluidEnRoute >= fireTask.remainingFluidNeeded();
    }

    private int getRequiredFluid(String severity) {
        return switch (severity.toLowerCase()) {
            case "high" ->
                30;
            case "moderate" ->
                20;
            case "low" ->
                10;
            default ->
                0;
        };
    }

    private boolean isHigherSeverity(String severity1, String severity2) {
        int priority1 = getSeverityPriority(severity1);
        int priority2 = getSeverityPriority(severity2);
        return priority1 > priority2;
    }

    private int getSeverityPriority(String severity) {
        return switch (severity.toLowerCase()) {
            case "high" ->
                3;
            case "moderate" ->
                2;
            case "low" ->
                1;
            default ->
                0;
        };
    }

    private double calculateDistance(double x1, double y1, int x2, int y2) {
        // a^2 + b^2 = c^2
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    private String getFireSeverity(FireTask fireTask) {
        int remaining = fireTask.remainingFluidNeeded();

        if (remaining >= 30) {
            return "high";
        } else if (remaining >= 20) {
            return "moderate";
        } else if (remaining > 0) {
            return "low";
        } else {
            return "";
        }
    }

    // ========== Getters and Setters ==========
    public Queue<FireEvent> getTaskQueue() {
        return taskQueue;
    }

    public Map<Integer, DroneInfo> getDroneRegistry() {
        return droneRegistry;
    }

    public Map<Integer, FireTask> getActiveFires() {
        return activeFires;
    }

    // ========== Sub Classes ==========
    private static class DroneInfo {

        int droneId;
        int port;
        String state;
        int fluidAssigned, battery;
        double fluid;
        double x, y;
        int assignedZoneID = -1;
        int dispatchCount = 0;

        public DroneInfo(int droneId, int port) {
            this.droneId = droneId;
            this.port = port;
            this.state = "IDLE";
            this.x = 0;
            this.y = 0;
            this.fluid = 15;
            this.battery = 1000;
            this.fluidAssigned = 0;
        }

        public boolean canHandleTask(FireEvent fire) {
            boolean isRightState = state.equals("IDLE") || state.equals("EN_ROUTE_BASE") || state.equals("FIRE_HANDLED");
            if (!isRightState) {
                return false;
            }
            if (this.fluid <= 0) {
                return false;
            }

            double distToFire = calculateDistance(this.x, this.y, fire.getTargetX(), fire.getTargetY());
            double distBackToBase = calculateDistance(fire.getTargetX(), fire.getTargetY(), 0, 0);
            double totalDistance = distToFire + distBackToBase;

            int batteryNeeded = (int) ((totalDistance / 5048.0) * 100);
            return this.battery >= batteryNeeded;
        }

        private double calculateDistance(double x1, double y1, int x2, int y2) {
            return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
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

    public long calculateGuiDroneTravelTime(double coordX, double coordY, double targetCoordX, double targetCoordY) {
        double distance = Math.sqrt(Math.pow(targetCoordX - coordX, 2) + Math.pow(targetCoordY - coordY, 2));
        return (long) (((distance * MS_PER_UNIT) * 10 / 2) / SimulationEnvironment.SIMULATION_SPEED);
    }

    private long calculateGuiDroneExtinguishTime(int fluidToDrop) {
        // TODO FIX
        double fluidRateMlMs = 0.25;
        int nozzleOpenDelayMs = 100;
        return (long) ((((fluidToDrop / fluidRateMlMs) + nozzleOpenDelayMs) * 10) / SimulationEnvironment.SIMULATION_SPEED);
    }
}
