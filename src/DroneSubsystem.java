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

public class DroneSubsystem implements Runnable {

    //STATIC DELAYS
    private static final int NOZZLE_OPEN_DELAY_MS = 100;
    private static final int NOZZLE_CLOSE_DELAY_MS = 100;
    private static final int FLUID_MAX = 15;
    private static final int BATTERY_MAX = 100;

    private static final double MS_PER_UNIT = 0.15;

    private static final double FLUID_RATE_ML_TICK = 0.5; //change it to be faster

    private UDPMessageBox messageBox;

    //Drone Information
    private static int nextIdValue = 1;//self ID creation

    private int droneId;
    private double coordX;
    private double coordY;

    private int targetCoordX;
    private int targetCoordY;
    private int batteryTravelDistance;
    private double fluidAmount;

    private int fluidAmountToDrop;
    private double fluidReleasedAtZone;

    private final DroneStateMachine droneSM;

    private String pendingFault, currentFault;

    private double initialDistance; // used for tracking when to fault stuck

    private double speedPertick = 100.0 / (MS_PER_UNIT * 10);

    public static void main(String[] args) {
        int TOTAL_DRONE_COUNT = 10;
        Thread[] droneThreads = new Thread[TOTAL_DRONE_COUNT];
        for (int i = 0; i < TOTAL_DRONE_COUNT; i++) {
            droneThreads[i] = new Thread(new DroneSubsystem(), "DroneSubsystemThread-" + (i + 1));
        }

        for (Thread droneThread : droneThreads) {
            droneThread.start();
        }
    }

    // Constructor
    public DroneSubsystem() {
        pendingFault = "";
        currentFault = "";

        messageBox = new UDPMessageBox(UDPMessageBox.Subsystem.DRONE);

        //Core initlization
        this.droneSM = new DroneStateMachine();
        this.droneId = nextIdValue++;

        //Default starting values
        this.coordX = 0;
        this.coordY = 0;
        this.fluidAmount = FLUID_MAX;
        this.batteryTravelDistance = BATTERY_MAX;  // TravelDistanceLevel change eventually

    }

    // Testing constructor (no GUI)
    //public DroneSubsystem() { this(null); }
    public void closeBox() {
        if (messageBox != null) {
            messageBox.closeBox();
        }
    }

    @Override
    public void run() {
        Message registerMessage = new Message(
                "Scheduler",
                "DroneSubsystem",
                String.valueOf(droneId) + "," + this.messageBox.getPort(),
                Message.MessageType.DroneRegistration
        );
        messageBox.putMessage(registerMessage, UDPMessageBox.SCHEDULER_PORT);
        while (true) {
            if (messageBox.isFull()) {
                Message message = messageBox.getMessage();
                handleMessage(message);
            }
            tick();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                System.out.println("[Drone " + droneId + "] Thread interrupted.");
                break;
            }
        }
    }

    public void handleMessage(Message message) {
        if (message.getMessageType() == Message.MessageType.DroneRequest) {
            DroneRequest droneEvent = new DroneRequest(message.getMessageData());
            System.out.println("[DroneSubsystem] Received DroneEvent: " + droneEvent);

            if (droneEvent.getDroneEvent() == DroneEvent.FIRE_ASSIGNED) {
                this.pendingFault = droneEvent.getFaultType();
            }
            if (pendingFault.equals("packet_loss")) {
                System.out.println("FAULTED: The drone never got the packet");
                pendingFault = "";
                return;
            }
            if (droneEvent.getDroneEvent() == DroneEvent.REQUEST_STATUS) {//if scheduler is requesting status send new  status
                System.out.println("Received update scheduler");
                updateScheduler();
            }
            //Handles event and requests
            if (droneEvent.getDroneEvent() == DroneEvent.FIRE_ASSIGNED || droneEvent.getDroneEvent() == DroneEvent.RETURN_BASE_REQUEST) {
                this.targetCoordX = droneEvent.getTargetX();
                this.targetCoordY = droneEvent.getTargetY();
                this.fluidAmountToDrop = droneEvent.getAmountToDrop();
                this.fluidReleasedAtZone = 0;
                this.initialDistance = calculateDistance(coordX, coordY, targetCoordX, targetCoordY);
            }
            //sends the message to the statemachine
            droneSM.handleEvent(droneEvent.getDroneEvent(), droneEvent.serialize(), this);

            //updates the Scheduler
            updateScheduler();

        } else {
            System.out.println("[DroneSubsystem] Received unknown message type: " + message.getMessageType());
        }

    }

    public Message sendStatus() { //creates the message
        if (droneSM.getCurrentState() == DroneState.FIRE_HANDLED && pendingFault.equals("corrupted")) { //corrupts on the fire extinguished message
            System.out.println("Corrupting a message");
            DroneResponse status = new DroneResponse(
                    droneId * -1,
                    generateCorruptedString(9),
                    generateCorruptedNumber(), generateCorruptedNumber(),
                    generateCorruptedNumber(),
                    batteryTravelDistance,
                    fluidReleasedAtZone,
                    generateCorruptedString(15)
            );
            System.out.println(status);
            pendingFault = "";
            return new Message("Scheduler", "DroneSubsystem", status.serialize(), Message.MessageType.DroneResponse);
        }

        DroneResponse status = new DroneResponse(
                droneId,
                droneSM.getCurrentState().toString(),
                coordX, coordY,
                fluidAmount,
                batteryTravelDistance,
                fluidReleasedAtZone,
                currentFault
        );
        // Return a new Message object intended for the Scheduler
        return new Message("Scheduler", "DroneSubsystem", status.serialize(), Message.MessageType.DroneResponse);
    }

    public void updateScheduler() { //updates scheduling logic
        System.out.println("[Drone " + droneId + "] Notifying scheduler of status.");
        messageBox.putMessage(sendStatus(), UDPMessageBox.SCHEDULER_PORT);
    }

    public void tick() {
        if (this.getCurrentState() == DroneState.EN_ROUTE_FIRE || this.getCurrentState() == DroneState.EN_ROUTE_BASE) {
            moveTick(speedPertick);
        } else if (this.getCurrentState() == DroneState.DROPPING_AGENT) {
            releaseFluidPerTick(FLUID_RATE_ML_TICK);
        }
    }

    //** Drone Movement & Modification Functions **//
    public boolean openNozzle(String payload) {
        System.out.println("Opening Nozzle");
        if (pendingFault.equals("jammed")) {
            currentFault = pendingFault;
            System.out.println("Drone nozzle jammed faulting");
            //handleFault();
            return false;
        } else {
            try {
                Thread.sleep(NOZZLE_OPEN_DELAY_MS);
                return true;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void closeNozzle(String payload) {
        try {
            Thread.sleep((long) (NOZZLE_CLOSE_DELAY_MS / SimulationEnvironment.SIMULATION_SPEED));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("[Drone " + droneId + "] Nozzle closed.");
        updateScheduler();
    }

    public void restore() {//restores battery and restores fuel level
        System.out.println("[Drone " + droneId + "] restocking drone.");
        this.batteryTravelDistance = BATTERY_MAX;
        this.fluidAmount = FLUID_MAX;
    }

    //This is to be decided later with different information//
    public void handleFault() {
        // TODO: log fault, notify scheduler, await repair event
        System.out.println("[Drone " + droneId + "] FAULTED. Awaiting repair.");
        currentFault = pendingFault;
        //updateScheduler();
        pendingFault = "";
    }

    // ========== Helpers ==========
    private double calculateDistance(double x1, double y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    public boolean hasBattery() {
        double droneToFire = calculateDistance(coordX, coordY, targetCoordX, targetCoordY);
        double fireToBase = calculateDistance(targetCoordX, targetCoordY, 0, 0);
        double totalDistance = droneToFire + fireToBase;
        return batteryTravelDistance >= totalDistance;
    }

    public int calculateBatteryUsage() {
        double droneToFire = calculateDistance(coordX, coordY, targetCoordX, targetCoordY);
        double fireToBase = calculateDistance(targetCoordX, targetCoordY, 0, 0);
        return (int) Math.ceil(droneToFire + fireToBase);
    }

    private void releaseFluidPerTick(double fluidPerTick) {
        if (fluidReleasedAtZone + fluidPerTick >= fluidAmountToDrop) {
            fluidPerTick = fluidAmountToDrop - fluidReleasedAtZone;
        }
        //System.out.println("Fluid per Tick " + fluidPerTick);

        fluidReleasedAtZone += fluidPerTick;
        fluidAmount -= fluidPerTick;

        if (fluidReleasedAtZone >= fluidAmountToDrop || fluidAmount <= 0) {
            fluidReleasedAtZone = fluidAmountToDrop;
            System.out.println("Released: "+fluidAmountToDrop+" fluid remaining: "+fluidReleasedAtZone);
            droneSM.handleEvent(DroneEvent.FIRE_EXTINGUISHED, "", this);
        }
    }

    // Called by tick()
    private void moveTick(double speed) {
        double dx = targetCoordX - coordX;
        double dy = targetCoordY - coordY;
        double distance = calculateDistance(coordX, coordY, targetCoordX, targetCoordY);

        if ("stuck".equals(pendingFault)) {
            double distanceTravelled = initialDistance - calculateDistance(coordX, coordY, targetCoordX, targetCoordY);
            if (distanceTravelled >= initialDistance / 2) {
                System.out.println("Drone stuck at "+coordX+" "+coordY +"Required"+targetCoordX+targetCoordY);
                droneSM.handleEvent(DroneEvent.FAILURE, "stuck", this);
                System.out.println("Current Fault is"+ currentFault);
                updateScheduler();
                return;
            }
        }

        if (distance <= speed) {
            coordX = targetCoordX;
            coordY = targetCoordY;
            droneSM.handleEvent(DroneEvent.ARRIVAL, "", this);
            updateScheduler();
        } else {
            coordX += (dx / distance) * speed;
            coordY += (dy / distance) * speed;
        }
    }

    public boolean hasAgent() {
        return fluidAmount > 0;
    }

    private String generateCorruptedString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+[]{};:,.<>/?";

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(index));
        }

        return sb.toString();
    }

    private double generateCorruptedNumber() {
        return (int) (Math.random() * 1_000_000);
    }

    // ========== GETTERS AND SETTERS ==========
    public int getDroneId() {
        return droneId;
    }

    public double getCoordX() {
        return coordX;
    }

    public void setCoordX(int coordX) {
        this.coordX = coordX;
    }

    public double getCoordY() {
        return coordY;
    }

    public void setCoordY(int coordY) {
        this.coordY = coordY;
    }

    public double getFluidAmount() {
        return fluidAmount;
    }

    public void setFluidAmount(int fluidAmount) {
        this.fluidAmount = fluidAmount;
    }

    public void setCurrentState(DroneState state) {
        droneSM.setCurrentState(state);
    }

    public DroneState getCurrentState() {
        return droneSM.getCurrentState();
    }

    public static int getTotalDronesCreated() {
        return nextIdValue - 1;
    }

    public UDPMessageBox getMessageBox() {
        return messageBox;
    }
}
