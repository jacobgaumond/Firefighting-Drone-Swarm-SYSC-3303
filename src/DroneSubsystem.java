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

    private MessageBox incomingMessageBox;
    private MessageBox schedulerMessageBox;

    private static int nextIdValue = 1;//self ID creation

    private int drone_ID;
    private int x_coord;
    private int y_coord;

    private int x_targetcoords;
    private int y_targetcoords;
    private int batteryTravelDistance;
    private int fluidAmount;

    private int fluidAmountToDrop;

    private static double fluidrate =0.25;

    private final DroneStateMachine droneSM;



    // WITHOUT MESSAGE BOXING
    public DroneSubsystem() {
        this.droneSM = new DroneStateMachine();
        this.drone_ID = nextIdValue++;
        this.x_coord =0;
        this.y_coord = 0;
        this.fluidAmount = 15;
        this.x_targetcoords=0;
        this.y_targetcoords=0;
        this.batteryTravelDistance =1000;  // TravelDistanceLevel to be decided

    }

    //WITH MESSAGEBOX
    public DroneSubsystem(MessageBox incomingMessageBox, MessageBox schedulerMessageBox) {
        this.schedulerMessageBox = schedulerMessageBox;
        this.incomingMessageBox = incomingMessageBox;
        this.droneSM = new DroneStateMachine();
        this.drone_ID = nextIdValue++;
        this.x_coord = 0;
        this.y_coord = 0;
        this.fluidAmount = 15;
        this.batteryTravelDistance = 1000;  // TravelDistanceLevel change eventually
    }



    @Override
    public void run() {

        Message registerMessage = new Message(
                "Scheduler",
                "DroneSubsystem",
                String.valueOf(drone_ID),
                Message.MessageType.DroneRegistration
        );
        schedulerMessageBox.putMessage(registerMessage);

        boolean boxOpen = true;
        do {
            Message message = incomingMessageBox.getMessage();
            if (message == null) {
                boxOpen = false;
            }  else {
                    handleMessage(message);
            }

        } while (boxOpen);
    }


    public void handleMessage(Message message){
        if(message.getMessageType() == Message.MessageType.DroneRequest) {
            DroneRequest droneEvent = new DroneRequest(message.getMessageData());
            System.out.println("[DroneSubsystem] Received DroneEvent: " + droneEvent);

            if(droneEvent.getDroneEvent()==DroneEvent.FIRE_ASSIGNED||droneEvent.getDroneEvent()==DroneEvent.RETURN_BASE_REQUEST) {
                this.x_targetcoords = droneEvent.getTargetX();
                this.y_targetcoords = droneEvent.getTargetY();
            }
            if(droneEvent.getDroneEvent()==DroneEvent.FIRE_ASSIGNED) {
                this.fluidAmountToDrop = droneEvent.getAmountToDrop();
            }
            //sends the message to the statemachine
            handleEvent(droneEvent.getDroneEvent(),droneEvent.serialize());

            //sends the new status back to the Scheduler
            schedulerMessageBox.putMessage(sendStatus());
        } else if(message.getMessageType() == Message.MessageType.DroneResponse) {
            //handle other types
            System.out.println("[DroneSubsystem] Received response: " + message.getMessageData());
        } else {
            System.out.println("[DroneSubsystem] Received unknown message type: " + message.getMessageType());
        }

    }

    public Message sendStatus(){ // int ID: statemachine drone: int x: int y: int fluidAmount: int battery
        DroneResponse status = new DroneResponse(
                drone_ID,
                droneSM.getCurrentState().toString(),
                x_coord, y_coord,
                fluidAmount,
                batteryTravelDistance,
                fluidAmountToDrop
        );
        // Return a new Message object intended for the Scheduler
        return new Message("Scheduler", "DroneSubsystem", status.serialize(), Message.MessageType.DroneResponse);
    }

    private void sendAcknowledgement(){
        System.out.println("[DroneSubsystem] Sending DroneSubsystem to Scheduler");
        schedulerMessageBox.putMessage(sendStatus());
    }

    public void handleEvent(DroneEvent event, String payload) {
        droneSM.handleEvent(event, payload,this);
    }

    // Drone Movement functions
    public void flyToFire(String payload) {
        //batteryTravelDistance -= calculateBatteryUsage();
        System.out.println("[Drone " + drone_ID + "] Flying to fire: " + payload);
        //just for this iteration
        x_coord=x_targetcoords;
        y_coord=y_targetcoords;
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        droneSM.handleEvent(DroneEvent.ARRIVAL,payload,this);
    }

    public void returnToBase(String payload) {
        //batteryTravelDistance -= calculateBatteryUsage();
       //for this iteration
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        x_coord=0;
        y_coord=0;
        droneSM.handleEvent(DroneEvent.ARRIVAL,payload,this);
        System.out.println("[Drone " + drone_ID + "] Returning to base.");
    }

    public void openNozzle() {
        System.out.println("[Drone " + drone_ID + "] Nozzle opened, dropping agent.");
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        fluidAmount -= fluidAmountToDrop;
        System.out.println("[Drone " + drone_ID + "] extinguishing fire.");
        droneSM.handleEvent(DroneEvent.FIRE_EXTINGUISHED, "", this);
    }

    public void closeNozzle(String payload) {
        System.out.println("[Drone " + drone_ID + "] Nozzle closed.");
    }

    public void restore() {//restores battery and restores fuel level
        System.out.println("[Drone " + drone_ID + "] restocking drone.");
        this.batteryTravelDistance = 1000;
        this.fluidAmount = 15;
    }


    public void updateScheduler() { //updates scheduling logic
        System.out.println("[Drone " + drone_ID + "] Notifying scheduler of status.");
        schedulerMessageBox.putMessage(sendStatus());
    }

    public void handleFault() {
        // TODO: log fault, notify scheduler, await repair event
        System.out.println("[Drone " + drone_ID + "] FAULTED. Awaiting repair.");
        schedulerMessageBox.putMessage(sendStatus());
    }


    // Specific checks to be done

    public boolean hasBattery() {
        double droneToFire = Math.sqrt(Math.pow(x_targetcoords - x_coord, 2) + Math.pow(y_targetcoords - y_coord, 2));
        double fireToBase = Math.sqrt(Math.pow(x_targetcoords, 2) + Math.pow(y_targetcoords, 2));
        double totalDistance = droneToFire + fireToBase;
        return batteryTravelDistance >= totalDistance;
    }

    public int calculateBatteryUsage() {
        double droneToFire = Math.sqrt(Math.pow(x_targetcoords - x_coord, 2) + Math.pow(y_targetcoords - y_coord, 2));
        double fireToBase = Math.sqrt(Math.pow(x_targetcoords, 2) + Math.pow(y_targetcoords, 2));
        return (int) Math.ceil(droneToFire + fireToBase);
    }

    public boolean hasAgent() {
        return fluidAmount > 0;
    }
    public int getDrone_ID() {return drone_ID;}

    public int getX_coord() { return x_coord; }
    public void setX_coord(int x_coord) { this.x_coord = x_coord; }

    public int getY_coord() { return y_coord; }
    public void setY_coord(int y_coord) { this.y_coord = y_coord; }

    public int getFluidAmount() { return fluidAmount; }
    public void setFluidAmount(int fluidAmount) { this.fluidAmount = fluidAmount; }

    public void setCurrentState(DroneState state){droneSM.setCurrentState(state);}

    public DroneState getCurrentState(){return droneSM.getCurrentState();}

    // Static method to check how many drones have been created
    public static int getTotalDronesCreated() {
        return nextIdValue - 1;
    }

}
