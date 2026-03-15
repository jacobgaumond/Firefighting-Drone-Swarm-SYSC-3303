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

//import java.io.*;
//import java.net.*;

public class DroneSubsystem implements Runnable {
//    SocketWrapper clientSocket;
//
//    public final static int DRONE_PORT = 5901;
//
//    public DroneSubsystem() {
//        try {
//            clientSocket = new SocketWrapper(DRONE_PORT);
//        } catch (SocketException se) {
//            throw new RuntimeException(se);
//        }
//    }

    private static final int NOZZLE_OPEN_DELAY_MS = 100;
    private static final int NOZZLE_CLOSE_DELAY_MS = 100;
    private static final int FLUID_MAX = 15;
    private static final int BATTERY_MAX = 100;

    private static final double MS_PER_UNIT = 0.15;
    private static final double MAX_DRONE_RANGE = 5048.0; // 2524 * 2 round trip

    private static final double FLUID_RATE_ML_MS = 0.25;

    private UDPMessageBox incomingMessageBox;
    private UDPMessageBox schedulerMessageBox;
    private DroneGUI gui;

    private static int nextIdValue = 1;//self ID creation

    private int droneId;
    private int coordX;
    private int coordY;

    private int targetCoordX;
    private int targetCoordY;
    private int batteryTravelDistance;
    private int fluidAmount;

    private int fluidAmountToDrop;
    private int fluidReleasedAtZone;

    private final DroneStateMachine droneSM;

    public static void main(String[] args) {
        DroneGUI gui = null; // TODO: Fix gui.

        int TOTAL_DRONE_COUNT = 2;
        Thread[] droneThreads = new Thread[TOTAL_DRONE_COUNT];
        for (int i = 0; i < TOTAL_DRONE_COUNT; i++) {
            droneThreads[i] = new Thread(new DroneSubsystem(gui), "DroneSubsystemThread-" + (i + 1));
        }

        for (Thread droneThread : droneThreads) {
            droneThread.start();
        }
    }    
    
    // Constructor
    public DroneSubsystem(DroneGUI gui) {
        schedulerMessageBox = new UDPMessageBox(UDPMessageBox.Subsystem.DRONE, UDPMessageBox.Subsystem.SCHEDULER);;
        incomingMessageBox  = new UDPMessageBox(UDPMessageBox.Subsystem.DRONE, UDPMessageBox.Subsystem.VOID);;
        this.gui = gui;

        //Core initlization
        this.droneSM = new DroneStateMachine();
        this.droneId = nextIdValue++;

        //Default starting values
        this.coordX = 0;
        this.coordY = 0;
        this.fluidAmount = FLUID_MAX;
        this.batteryTravelDistance = BATTERY_MAX;  // TravelDistanceLevel change eventually
        
        // Create drone label
        if (gui != null) {
            gui.createDroneLabel(droneId);
        }
    }
    
    // Testing constructor (no GUI)
    public DroneSubsystem() { this(null); }

    @Override
    public void run() {
        Message registerMessage = new Message(
                "Scheduler",
                "DroneSubsystem",
                String.valueOf(droneId),
                Message.MessageType.DroneRegistration
        );
        schedulerMessageBox.putMessage(registerMessage);

        boolean boxOpen = true;
        do {
            Message message = incomingMessageBox.getMessage();
            if (message == null) {
                boxOpen = false;
            } else {
                handleMessage(message);
            }

        } while (boxOpen);
    }

    public void handleMessage(Message message) {
        if (message.getMessageType() == Message.MessageType.DroneRequest) {
            DroneRequest droneEvent = new DroneRequest(message.getMessageData());
            System.out.println("[DroneSubsystem] Received DroneEvent: " + droneEvent);

            if (droneEvent.getDroneEvent() == DroneEvent.FIRE_ASSIGNED || droneEvent.getDroneEvent() == DroneEvent.RETURN_BASE_REQUEST) {
                this.targetCoordX = droneEvent.getTargetX();
                this.targetCoordY = droneEvent.getTargetY();
                this.fluidAmountToDrop = droneEvent.getAmountToDrop();
                this.fluidReleasedAtZone = 0;
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
        DroneResponse status = new DroneResponse(
                droneId,
                droneSM.getCurrentState().toString(),
                coordX, coordY,
                fluidAmount,
                batteryTravelDistance,
                fluidReleasedAtZone
        );
        // Return a new Message object intended for the Scheduler
        return new Message("Scheduler", "DroneSubsystem", status.serialize(), Message.MessageType.DroneResponse);
    }

    public void updateScheduler() { //updates scheduling logic
        System.out.println("[Drone " + droneId + "] Notifying scheduler of status.");
        schedulerMessageBox.putMessage(sendStatus());
    }

    //** Drone Movement & Modification Functions **//
    public void flyToFire(String payload) {
        //batteryTravelDistance -= calculateBatteryUsage();
        System.out.println("[Drone " + droneId + "] Flying to fire: " + payload);
        
        //just for this iteration
        double distance = Math.sqrt(Math.pow(targetCoordX - coordX, 2) + Math.pow(targetCoordY - coordY, 2));
        long travelTime = (long) ((distance * MS_PER_UNIT) * 10);
        
        if (gui != null) {
            DroneRequest request = new DroneRequest(payload);
            gui.moveDroneToZone(droneId, request.getZoneId(), travelTime, fluidAmount);
        }

        try {
            Thread.sleep(travelTime);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        batteryTravelDistance -= (int)((distance / MAX_DRONE_RANGE) * BATTERY_MAX);
        coordX = targetCoordX;
        coordY = targetCoordY;
        droneSM.handleEvent(DroneEvent.ARRIVAL, payload, this);
    }

    public void returnToBase(String payload) {
        double distance = Math.sqrt(Math.pow(targetCoordX - coordX, 2) + Math.pow(targetCoordY - coordY, 2));
        long travelTime = (long) ((distance * MS_PER_UNIT) * 10);
        
        if (gui != null) {
            gui.returnDrone(droneId, travelTime);
        }
        try {
            Thread.sleep(travelTime);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        batteryTravelDistance -= (int)((distance / MAX_DRONE_RANGE) * BATTERY_MAX);
        coordX = 0;
        coordY = 0;
        System.out.println("[Drone " + droneId + "] Returning to base.");
        droneSM.handleEvent(DroneEvent.ARRIVAL, payload, this);
    }

    public void openNozzle(String payload) {
        System.out.println("[Drone " + droneId + "] Nozzle opened, dropping agent.");
        long dropTime = (long) ((fluidAmountToDrop / FLUID_RATE_ML_MS) + NOZZLE_OPEN_DELAY_MS)*10;

        if (gui != null) {
            DroneRequest request = new DroneRequest(payload);
            gui.extinguishFire(droneId, request.getZoneId(), dropTime);
        }

        System.out.println("[Drone " + droneId + "] extinguishing fire.");
        try {
            Thread.sleep(dropTime);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        fluidAmount -= fluidAmountToDrop;
        fluidReleasedAtZone += fluidAmountToDrop;
        
        droneSM.handleEvent(DroneEvent.FIRE_EXTINGUISHED, "", this);
    }

    public void closeNozzle(String payload) {
        try {
            Thread.sleep(NOZZLE_CLOSE_DELAY_MS); //Nozzle Door Close Time
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("[Drone " + droneId + "] Nozzle closed.");
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
        schedulerMessageBox.putMessage(sendStatus());
    }

    // ========== Helpers ==========
    public boolean hasBattery() {
        double droneToFire = Math.sqrt(Math.pow(targetCoordX - coordX, 2) + Math.pow(targetCoordY - coordY, 2));
        double fireToBase = Math.sqrt(Math.pow(targetCoordX, 2) + Math.pow(targetCoordY, 2));
        double totalDistance = droneToFire + fireToBase;
        return batteryTravelDistance >= totalDistance;
    }

    public int calculateBatteryUsage() {
        double droneToFire = Math.sqrt(Math.pow(targetCoordX - coordX, 2) + Math.pow(targetCoordY - coordY, 2));
        double fireToBase = Math.sqrt(Math.pow(targetCoordX, 2) + Math.pow(targetCoordY, 2));
        return (int) Math.ceil(droneToFire + fireToBase);
    }

    public boolean hasAgent() {
        return fluidAmount > 0;
    }

    // ========== GETTERS AND SETTERS ==========
    public int getDroneId() {
        return droneId;
    }
    public int getCoordX() {
        return coordX;
    }
    public void setCoordX(int coordX) {
        this.coordX = coordX;
    }
    public int getCoordY() {
        return coordY;
    }
    public void setCoordY(int coordY) {
        this.coordY = coordY;
    }
    public int getFluidAmount() {
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
}
