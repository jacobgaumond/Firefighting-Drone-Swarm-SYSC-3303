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

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Scheduler implements Runnable {

    private DroneGUI gui;
    private EventLogger logger = new EventLogger(250, "log.txt");
    private UDPMessageBox messageBox;

    private final SchedulerStateMachine schedulerSM = new SchedulerStateMachine();
    private static final double MS_PER_UNIT = 0.15;
    private boolean clockStarted = false;

    // State Tracking
    private final Map<Integer, DroneInfo> droneRegistry = new HashMap<>();
    private final Map<Integer, FireTask> activeFires = new HashMap<>();

    private boolean stopWatchdog = false;

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
        startWatchdog(); //makes sure the drones are responding
    }

    // Testing constructor (no GUI)
    public Scheduler() {
        this(null);
    }

    public void closeBox() {
        stopWatchdog = true;

        if (messageBox != null) {
            messageBox.closeBox();
        }
    }

    @Override
    public void run() {
        boolean boxOpen = true;
        boolean allFiresHandled = false;

        while (boxOpen) {
            Message message = messageBox.getMessage();

            if (message == null) {
                boxOpen = false;
            } else {
                if (message.getMessageData().equals("ALL_FIRES_HANDLED")) {
                    allFiresHandled = true; // TODO: how do we exit this only once all drones are back?
                } else {
                    processIncomingMessage(message);
                }
                if (allFiresHandled && allDronesFinalPosition() && activeFires.isEmpty()) {
                    break;
                }
            }
        }

        // Generate and log metrics analysis
        gui.stopClock();
        try {
            if (gui != null) {
                gui.logMessage(logger.analyzeAverageEventResponseTime());
                gui.logMessage(logger.analyzeMaximumEventResponseTime());
                gui.logMessage(logger.analyzeAverageEventCompletionTime());
                gui.logMessage(logger.analyzeMaximumEventCompletionTime());
                gui.logMessage(logger.analyzeDroneUtilization());
            }
        } catch (IOException e) {
            System.out.println("[SCHEDULER] Error generating metrics");
        }

        logger.shutdown();

        while (true) {
            // keep open for user
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
        int zoneId = fireEvent.getZoneId();

        activeFires.computeIfAbsent(
                zoneId,
                id -> new FireTask(fireEvent)
        );

        if (gui != null) {
            if (!clockStarted) {
                SimulationEnvironment.startClock(fireEvent.getTimeInSeconds());
                gui.startClockDisplay();
                clockStarted = true;
                logger.log("FIRST_FIRE_EVENT", SimulationEnvironment.getCurrentTimeSeconds(), "");


            }
            gui.fireStatusChange(zoneId, fireEvent.getSeverity());
        }
        logger.log("FIRE_EVENT_CREATED", SimulationEnvironment.getCurrentTimeSeconds(), "zone=" + zoneId);

        System.out.println("[SCHEDULER] Handling fire event: " + fireEvent);
        schedulerSM.handleEvent(SchedulerEvent.FIRE_EVENT, this);
    }

    //Simple Message checking//
    private boolean checkMessage(Message message) {
        if (message == null || message.getMessageData() == null) {
            return false;
        }
        DroneResponse status;
        try {
            status = new DroneResponse(message.getMessageData());
        } catch (Exception e) {
            System.out.println("[Scheduler] Failed to parse message (corrupted)");
            return false;
        }
        // ===== field validation =====
        if (status.getDroneID() < 0) {
            return false;
        }
        if (status.getBattery() < 0 || status.getBattery() > DroneSubsystem.BATTERY_MAX) {
            return false;
        }
        if (Double.isNaN(status.getX()) || Double.isInfinite(status.getX())) {
            return false;
        }
        if (Double.isNaN(status.getY()) || Double.isInfinite(status.getY())) {
            return false;
        }
        if (status.getFluidAmount() < 0) {
            return false;
        }
        return true;
    }

    private void processDroneMessage(Message message) {
        if (!checkMessage(message)) {
            sendEventToDrone(-1, DroneEvent.REQUEST_STATUS, String.valueOf(message.getSenderPort()));
            this.gui.logMessage("Corrupted Packet Received");
            return;
        }

        // task finished -> assign next in the queue or mark drone as idle
        DroneResponse status = new DroneResponse(message.getMessageData());

        // Update drone registry
        DroneInfo drone = droneRegistry.get(status.getDroneID());
        drone.lastHeardFrom = SimulationEnvironment.getCurrentTimeSeconds();
        drone.awaitingResponse = false;
        drone.x = status.getX();
        drone.y = status.getY();
        drone.fluid = status.getFluidAmount();
        drone.battery = status.getBattery();
        if(drone.state.equals("FAULTED")){
            gui.moveDroneToZone(drone.droneId,drone.assignedZoneID,calculateGuiDroneTravelTime(drone.x,drone.y,drone.assignedX,drone.assignedY),drone.fluid);
        }
        drone.state = status.getState();
        if (gui != null) {
            gui.updateDroneStatus(status.getDroneID(), status.getFluidAmount(), status.getBattery(), status.getState());
        }

        switch (drone.state) {
            case "ARRIVED_AT_FIRE":
                logger.log("DRONE_ARRIVED_AT_FIRE", SimulationEnvironment.getCurrentTimeSeconds(), "drone=" + drone.droneId+", zone=" + drone.assignedZoneID);

                sendEventToDrone(status.getDroneID(), DroneEvent.EXTINGUISH_REQUEST, "");
                if (gui != null) {
                    gui.extinguishFire(drone.droneId, drone.assignedZoneID, calculateGuiDroneExtinguishTime(drone.fluidAssigned));
                }
                break;

            case "FIRE_HANDLED":
                FireTask activeTask = activeFires.get(drone.assignedZoneID);

                if (activeTask != null) { //Checks to see if the zoneFire is fully out or not
                    int zoneId = activeTask.fireEvent.getZoneId();
                    activeTask.assignedDrones.remove(drone.droneId);
                    activeTask.fluidDropped += (int) status.getFluidDropped();
                    if (activeTask.isExtinguished()) {
                        System.out.println("[Scheduler] Zone " + zoneId + " EXTINGUISHED!");
                        logger.log("FIRE_EXTINGUISHED", SimulationEnvironment.getCurrentTimeSeconds(), "zone=" + zoneId);
                        activeFires.remove(zoneId);
                        if (gui != null) {
                            gui.fireStatusChange(zoneId, capitalizeSeverity(getFireSeverity(activeTask)));
                        }

                        if (activeFires.isEmpty()) {
                            System.out.println("[SCHEDULER] Logging ALL_FIRES_EXTINGUISHED");
                            logger.log("ALL_FIRES_EXTINGUISHED", SimulationEnvironment.getCurrentTimeSeconds(), "");
                            schedulerSM.handleEvent(SchedulerEvent.ALL_FIRES_EXTINGUISHED, this);
                        }
                    } else {
                        if (gui != null) {
                            gui.fireStatusChange(zoneId, capitalizeSeverity(getFireSeverity(activeTask)));
                        }
                    }
                }
                // assign if it has remaining fluid
                if (drone.fluid > 0 && !activeFires.isEmpty()) {
                    drone.assignedZoneID = -1;
                    drone.fluidAssigned = 0;
                    FireEvent reassigned = findNearbyFire(drone);
                    if (reassigned != null) {
                        assignFireTaskToDrone(drone, reassigned);
                    }
                    if (drone.assignedZoneID == -1) {
                        sendEventToDrone(drone.droneId, DroneEvent.RETURN_BASE_REQUEST, "");
                        gui.returnDrone(drone.droneId, calculateGuiDroneTravelTime(drone.x, drone.y, 0, 0));
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
                logger.log("DRONE_IDLE", SimulationEnvironment.getCurrentTimeSeconds(), "drone=" + drone.droneId);

                tryAssignTask();
                break;

            case "FAULTED":
                System.out.println("[Scheduler] Drone " + status.getDroneID() + " has faulted with " + status.getFaultType());
                if(status.getFaultType().equals("stuck")){ //handles getting the drone back online
                    sendEventToDrone(status.getDroneID(), DroneEvent.DRONE_BACKONLINE, String.valueOf(message.getSenderPort()));
                }
                else {
                    for (FireTask task : activeFires.values()) {
                        if (task.assignedDrones.containsKey(status.getDroneID())) {
                            System.out.println("Removing the faulted drones zone");
                            task.assignedDrones.remove(status.getDroneID()); //remove the drone assignment
                            tryAssignTask();
                            break;
                        }
                    }
                }

                gui.faultDrone(status.getDroneID(), status.getFaultType());
                break;
        }
    }

    // ========== Drone and Task Logic ==========
    public void tryAssignTask() {
        boolean assigned;

        do {
            assigned = false;

            // Find fires that still need fluid, sorted by priority
            List<FireTask> sortedFires = activeFires.values().stream()
                    .filter(task -> task.netFluidStillNeeded() > 0)
                    .sorted(Comparator
                            .comparingInt((FireTask t) -> getSeverityPriority(t.fireEvent.getSeverity()))
                            .reversed() // highest severity first
                            .thenComparingLong(t -> t.createdAt)) // oldest first on ties
                    .collect(Collectors.toList());

            // Try to assign a drone to any fire that needs it
            for (FireTask task : sortedFires) {
                DroneInfo available = findAvailableDrone(task.fireEvent);
                if (available != null) {
                    assignFireTaskToDrone(available, task.fireEvent);
                    assigned = true;
                    break; // Break after assigning one drone, then restart the loop
                }
            }

        } while (assigned); // Keep looping as long as we assigned at least one drone

        // After loop, check the state
        if (activeFires.isEmpty()) {
            schedulerSM.handleEvent(SchedulerEvent.ALL_FIRES_EXTINGUISHED, this);
        } else {
            boolean allHaveEnough = activeFires.values().stream()
                    .allMatch(task -> task.netFluidStillNeeded() <= 0);

            if (allHaveEnough) {
                schedulerSM.handleEvent(SchedulerEvent.DRONES_AVAILABLE, this);
            } else {
                schedulerSM.handleEvent(SchedulerEvent.NOT_ENOUGH_DRONES_AVAILABLE, this);
            }
        }
    }

    private void assignFireTaskToDrone(DroneInfo drone, FireEvent fireEvent) {
        FireTask fireTask = activeFires.computeIfAbsent(
                fireEvent.getZoneId(),
                id -> new FireTask(fireEvent)
        );
        int amountToDrop = (int) Math.min(drone.fluid, fireTask.netFluidStillNeeded());
        fireTask.assignedDrones.put(drone.droneId, amountToDrop); //assigns what the current drone is going to drop
        drone.assignedZoneID = fireEvent.getZoneId();
        drone.state = "EN_ROUTE_FIRE";
        drone.fluidAssigned = amountToDrop;
        drone.assignedX = fireEvent.getTargetX();
        drone.assignedY= fireEvent.getTargetY();
        drone.dispatchCount++;

        logger.log("DRONE_ASSIGNED", SimulationEnvironment.getCurrentTimeSeconds(), "drone=" + drone.droneId);
        logger.log("DRONE_DEPARTED", SimulationEnvironment.getCurrentTimeSeconds(), "drone=" + drone.droneId);

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
        System.out.println("Assigning zone " + fireEvent.getZoneId() + " to drone" + drone.droneId);
        if (gui != null) {
            gui.moveDroneToZone(drone.droneId,
                    fireEvent.getZoneId(),
                    calculateGuiDroneTravelTime(drone.x, drone.y, fireEvent.getTargetX(), fireEvent.getTargetY()),
                    drone.fluid
            );
        }

        Message droneMessage = new Message(
                "DroneSubsystem",
                "Scheduler",
                request.serialize(),
                Message.MessageType.DroneRequest
        );
        fireEvent.setFaultType("");
        drone.lastSentRequest = request;
        drone.awaitingResponse = true;

        double distance = calculateDistance(drone.x, drone.y, fireEvent.getTargetX(), fireEvent.getTargetY());

        double travelTimeSeconds = distance * MS_PER_UNIT / 1000.0;
        drone.dispatchTime = SimulationEnvironment.getCurrentTimeSeconds();
        drone.expectedResponseTime = (long) (travelTimeSeconds + 6);

        messageBox.putMessage(droneMessage, drone.port);
    }

    private void sendEventToDrone(int droneId, DroneEvent event, String payload) {

        DroneRequest request = new DroneRequest(
                event, "", 0, "", "", 0, 0, 0, droneId, "NONE"
        );
        if (droneId == -1) { // corrupted drone
            System.out.println(" ReRequesting information back from a corrupted drone message");
            gui.faultDrone(droneId, "corruption");
            Message message = new Message(
                    "DroneSubsystem",
                    "Scheduler",
                    request.serialize(),
                    Message.MessageType.DroneRequest
            );
            messageBox.putMessage(message, Integer.parseInt(payload));
            return;
        }
        DroneInfo drone = droneRegistry.get(droneId);
        System.out.println("Sending Event To Drone:" + droneId + "Event:" + event);
        drone.dispatchTime = SimulationEnvironment.getCurrentTimeSeconds();
        drone.expectedResponseTime =6;
        drone.lastSentRequest = request;
        drone.awaitingResponse = true;

        Message message = new Message(
                "DroneSubsystem",
                "Scheduler",
                request.serialize(),
                Message.MessageType.DroneRequest
        );
        drone.lastSentRequest = request;
        drone.awaitingResponse = true;
        messageBox.putMessage(message, drone.port);
    }

    // ========== Helper Functions ==========
    private FireEvent findNearbyFire(DroneInfo drone) {
        FireEvent nearest = null;
        double shortestDistance = Double.MAX_VALUE;

        for (FireTask task : activeFires.values()) {
            if (task.netFluidStillNeeded() <= 0) {
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

    public boolean allDronesFinalPosition() {
        for (DroneInfo drone : droneRegistry.values()) {
            boolean isFaulted = drone.state.equals("FAULTED");
            boolean isAtBase = drone.x == 0 && drone.y == 0;

            if (!isFaulted && !isAtBase) {
                return false;
            }
        }
        return true;
    }

    private static int getRequiredFluid(String severity) {
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
        double remaining = fireTask.getFireSeverity();

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

    public void startWatchdog() {
        Thread watchdog = new Thread(() -> {
            while (!stopWatchdog) {
                try {
                    Thread.sleep(SimulationEnvironment.SIMULATION_SECOND_MS);
                } catch (InterruptedException e) {
                    break;
                }
                long now = SimulationEnvironment.getCurrentTimeSeconds();
                for (DroneInfo drone : droneRegistry.values()) {
                    if (drone.awaitingResponse && (now - drone.dispatchTime) > drone.expectedResponseTime) {
                        System.out.println("[Scheduler] Drone " + drone.droneId + " never responded, resending.");
                        drone.dispatchTime = now;

                        if (drone.lastSentRequest != null) {
                            DroneRequest resendRequest = drone.lastSentRequest;
                            this.gui.logMessage("Resending packet to: " + drone.droneId);
                            resendRequest.setFaultType("NONE"); // strip fault before resending,
                            Message resend = new Message(
                                    "DroneSubsystem",
                                    "Scheduler",
                                    resendRequest.serialize(),
                                    Message.MessageType.DroneRequest
                            );
                            messageBox.putMessage(resend, drone.port);
                        }
                    }
                }
            }
        }, "WatchdogThread");
        watchdog.setDaemon(true);
        watchdog.start();
    }

    // ========== Getters and Setters ==========
    public Map<Integer, DroneInfo> getDroneRegistry() {
        return droneRegistry;
    }

    public Map<Integer, FireTask> getActiveFires() {
        return activeFires;
    }

    // ========== Sub Classes ==========
    public class DroneInfo {

        int droneId;
        int port;
        String state;
        int fluidAssigned, battery;
        double fluid;
        double x, y;

        double assignedX, assignedY;
        int assignedZoneID = -1;
        int dispatchCount = 0;

        DroneRequest lastSentRequest;
        long lastHeardFrom = SimulationEnvironment.getCurrentTimeSeconds();
        boolean awaitingResponse = false;

        private long dispatchTime;
        private long expectedResponseTime;

        public DroneInfo(int droneId, int port) {
            this.droneId = droneId;
            this.port = port;
            this.state = "IDLE";
            this.x = 0;
            this.y = 0;
            this.fluid = 15;
            this.battery = 1000;
            this.fluidAssigned = 0;
            lastSentRequest = null;
        }

        public boolean canHandleTask(FireEvent fire) {
            boolean isRightState = state.equals("IDLE") || state.equals("FIRE_HANDLED");
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

        public long getDispatchTime() {
            return dispatchTime;
        }
        public void setTargetXY(int x, int y){
            this.assignedX=x;
            this.assignedY=y;
        }

        public long getExpectedResponseTime() {
            return expectedResponseTime;
        }

        public void setDispatchTime(long dispatchTime) {
            this.dispatchTime = dispatchTime;
        }

        public void setExpectedResponseTime(long expectedResponseTime) {
            this.expectedResponseTime = expectedResponseTime;
        }
    }

    public class FireTask {

        FireEvent fireEvent;
        double fluidRequired;
        double fluidDropped = 0.0;
        Map<Integer, Integer> assignedDrones = new HashMap<>();
        long createdAt = SimulationEnvironment.getCurrentTimeSeconds();

        public FireTask(FireEvent fireEvent) {
            this.fireEvent = fireEvent;
            this.fluidRequired = getRequiredFluid(fireEvent.getSeverity());
        }

        public int getFluidCurrentlyEnRoute() {//firetasks are like this now
            return assignedDrones.values().stream().mapToInt(Integer::intValue).sum();
        }

        public FireEvent getFireEvent() {
            return fireEvent;
        }

        public double netFluidStillNeeded() {//sees if it needs more help
            return fluidRequired - fluidDropped - getFluidCurrentlyEnRoute();
        }

        public double getFireSeverity() {
            return fluidRequired - fluidDropped;
        }

        public boolean isExtinguished() {
            return fluidDropped >= fluidRequired;
        }

    }

    public long calculateGuiDroneTravelTime(double coordX, double coordY, double targetCoordX, double targetCoordY) {
        double distance = Math.sqrt(Math.pow(targetCoordX - coordX, 2) + Math.pow(targetCoordY - coordY, 2));
        double timeSeconds = distance / 15.0; // simulated seconds
        double timeMs = timeSeconds * 1000;   // simulated ms
        return (long) (timeMs / SimulationEnvironment.SIMULATION_SPEED); // convert to real ms
    }

    private long calculateGuiDroneExtinguishTime(int fluidToDrop) {
        int nozzleOpenDelayMs = 100 / SimulationEnvironment.SIMULATION_SPEED;
        double ticksNeeded = fluidToDrop / 0.5;
        return (long) (ticksNeeded * SimulationEnvironment.SIMULATION_SECOND_MS) + nozzleOpenDelayMs;
    }
}
