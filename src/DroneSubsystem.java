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

    private static final double FLUID_RATE_ML_MS= 0.25;

    private MessageBox incomingMessageBox;
    private MessageBox schedulerMessageBox;

    private static int nextIdValue = 1;//self ID creation

    private int droneId;
    private int coordX;
    private int coordY;

    private int targetCoordX;
    private int targetCoordY;
    private int batteryTravelDistance;
    private int fluidAmount;

    private int fluidAmountToDrop;

    private final DroneStateMachine droneSM;

    // WITHOUT MESSAGE BOXING
    public DroneSubsystem() {
        this(null, null);
    }

    //WITH MESSAGEBOX
    public DroneSubsystem(MessageBox incomingMessageBox, MessageBox schedulerMessageBox) {
        this.schedulerMessageBox = schedulerMessageBox;
        this.incomingMessageBox = incomingMessageBox;

        //Core initlization
        this.droneSM = new DroneStateMachine();
        this.droneId = nextIdValue++;

        //Default starting values
        this.coordX = 0;
        this.coordY = 0;
        this.fluidAmount = 15;
        this.batteryTravelDistance = 1000;  // TravelDistanceLevel change eventually
    }

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
            }
            if (droneEvent.getDroneEvent() == DroneEvent.FIRE_ASSIGNED) {
                this.fluidAmountToDrop = droneEvent.getAmountToDrop();
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
                fluidAmountToDrop
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
        coordX = targetCoordX;
        coordY = targetCoordY;
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        droneSM.handleEvent(DroneEvent.ARRIVAL, payload, this);
    }

    public void returnToBase(String payload) {
        //batteryTravelDistance -= calculateBatteryUsage();
        //for this iteration
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        coordX = 0;
        coordY = 0;
        droneSM.handleEvent(DroneEvent.ARRIVAL, payload, this);
        System.out.println("[Drone " + droneId + "] Returning to base.");
    }

    public void openNozzle() {
        System.out.println("[Drone " + droneId + "] Nozzle opened, dropping agent.");
        long dropTime = (long) (fluidAmountToDrop / FLUID_RATE_ML_MS)+ NOZZLE_OPEN_DELAY_MS;
        try {
            Thread.sleep(dropTime);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        fluidAmount -= fluidAmountToDrop;
        System.out.println("[Drone " + droneId + "] extinguishing fire.");
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
        this.batteryTravelDistance = 1000;
        this.fluidAmount = 15;
    }

    //This is to be decided later with different information//
    public void handleFault() {
        // TODO: log fault, notify scheduler, await repair event
        System.out.println("[Drone " + droneId + "] FAULTED. Awaiting repair.");
        schedulerMessageBox.putMessage(sendStatus());
    }


    // DRONE CHECKING//
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

    //** GETTERS AND SETTERS **//
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

    // Static method to check how many drones have been created
    public static int getTotalDronesCreated() {
        return nextIdValue - 1;
    }

}
