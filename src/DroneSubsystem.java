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
    private static final int NOZZLE_OPEN_DELAY_MS = 100;
    private static final int NOZZLE_CLOSE_DELAY_MS = 100;
    private static final int FLUID_MAX = 15;
    private static final int BATTERY_MAX = 100;

    private static final double MS_PER_UNIT = 0.15;
    private static final double MAX_DRONE_RANGE = 5048.0; // 2524 * 2 round trip

    private static final double FLUID_RATE_ML_TICK = 0.25;

    private UDPMessageBox messageBox;

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


    private boolean moving = false;
    private double speedPertick = 1.0;
    private boolean droppingFluid = false;

    public static void main(String[] args) {
        int TOTAL_DRONE_COUNT = 2;
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
        messageBox  = new UDPMessageBox(UDPMessageBox.Subsystem.DRONE);

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
            Message message = messageBox.pollMessage(); //attempts to pull a message
            if (message != null) { // if message handle the message
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
        messageBox.putMessage(sendStatus(), UDPMessageBox.SCHEDULER_PORT);
    }

    public void tick(){
        if(this.getCurrentState()==DroneState.EN_ROUTE_FIRE||this.getCurrentState()==DroneState.EN_ROUTE_BASE){
            //moveTick(speedPertick);
        }
        else if (this.getCurrentState()==DroneState.DROPPING_AGENT){
            releaseFluidPerTick(FLUID_RATE_ML_TICK);
            System.out.println("Amount of fluid drone has:"+fluidAmount+"Fluid release at zone:"+fluidReleasedAtZone+"fluid to drop"+fluidAmountToDrop);
        }
    }


    //** Drone Movement & Modification Functions **//
    public void flyToFire(String payload) {
        // batteryTravelDistance -= calculateBatteryUsage();
        System.out.println("[Drone " + droneId + "] Flying to fire: " + payload);

        // travel time
        double distance = Math.sqrt(Math.pow(targetCoordX - coordX, 2) + Math.pow(targetCoordY - coordY, 2));
        long travelTime = (long) ((distance * MS_PER_UNIT) * 10);

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

        try {
            Thread.sleep(travelTime);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        batteryTravelDistance -= (int)((distance / MAX_DRONE_RANGE) * BATTERY_MAX);
        coordX = targetCoordX;
        coordY = targetCoordY;
        System.out.println("[Drone " + droneId + "] Returning to base.");
        droneSM.handleEvent(DroneEvent.ARRIVAL, payload, this);
    }

    /*public void openNozzle(String payload) {
        System.out.println("[Drone " + droneId + "] Nozzle opened, dropping agent.");
        long dropTime = (long) ((fluidAmountToDrop / FLUID_RATE_ML_MS) + NOZZLE_OPEN_DELAY_MS)*10;

        System.out.println("[Drone " + droneId + "] extinguishing fire.");
        try {
            Thread.sleep(dropTime);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        fluidAmount -= fluidAmountToDrop;
        fluidReleasedAtZone += fluidAmountToDrop;

        droneSM.handleEvent(DroneEvent.FIRE_EXTINGUISHED, "", this);
    }*/

    public void closeNozzle(String payload) {
        try {
            Thread.sleep(NOZZLE_CLOSE_DELAY_MS); //Nozzle Door Close Time
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("[Drone " + droneId + "] Nozzle closed.");
        System.out.println(fluidReleasedAtZone);
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
        messageBox.putMessage(sendStatus(), UDPMessageBox.SCHEDULER_PORT);
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

    private double calculateDistance(double x1, double y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }


    private void moveTowardsTarget(int targetX, int targetY, double speedPerTick) {
        double dx = targetX - coordX;
        double dy = targetY - coordY;
        double distance = calculateDistance(coordX, coordY, targetX, targetY);

        if (distance <= speedPerTick) {
            coordX = targetX;
            coordY = targetY;
        } else {
            double ratio = speedPerTick / distance;
            coordX += (int) Math.round(dx * ratio);
            coordY += (int) Math.round(dy * ratio);
        }

        batteryTravelDistance -= (int) Math.ceil(speedPerTick / MAX_DRONE_RANGE * BATTERY_MAX);
    }


    private void releaseFluidPerTick(double fluidPerTick) {
        if (fluidReleasedAtZone + fluidPerTick >= fluidAmountToDrop) {
            fluidPerTick = fluidAmountToDrop - fluidReleasedAtZone;
        }
        System.out.println("Fluid per Tick "+ fluidPerTick );

        fluidReleasedAtZone += fluidPerTick;
        fluidAmount -= fluidPerTick;

        if (fluidReleasedAtZone >= fluidAmountToDrop || fluidAmount<=0) {
            droneSM.handleEvent(DroneEvent.FIRE_EXTINGUISHED, "", this);
        }
    }

    // Called by the state machine instead of blocking methods
    public void startFlyingTo(int x, int y, String payload) {
        this.targetCoordX = x;
        this.targetCoordY = y;
        this.moving = true;
    }

    // Called by tick()
    private void moveTick(double speed) {
        if (!moving) return;

        double dx = targetCoordX - coordX;
        double dy = targetCoordY - coordY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance <= speed) {
            coordX = targetCoordX;
            coordY = targetCoordY;
            moving = false;
            droneSM.handleEvent(DroneEvent.ARRIVAL, "", this);
        } else {
            coordX += (dx / distance) * speed;
            coordY += (dy / distance) * speed;
        }
    }






    public boolean hasAgent() {
        return fluidAmount > 0;
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
