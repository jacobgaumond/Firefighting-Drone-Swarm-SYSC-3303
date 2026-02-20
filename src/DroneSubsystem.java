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
    private int batteryTravelDistance;
    private int fluidAmount;

    private final DroneStateMachine droneSM;



    // WITHOUT MESSAGE BOXING
    public DroneSubsystem() {
        this.droneSM = new DroneStateMachine();
        this.drone_ID = nextIdValue++;
        this.x_coord =0;
        this.y_coord = 0;
        this.fluidAmount = 15;
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
        boolean boxOpen = true;
        do {
            Message message = incomingMessageBox.getMessage();
            if (message == null) {
                boxOpen = false;
            }
            else {
                System.out.println("[DroneSubsystem] Received from " + message.getSourceName() + ": " + message.getMessageData());
                if (!message.getMessageData().equals("Acknowledged")) {
                    message = new Message("FireIncidentSubsystem", "DroneSubsystem", "Acknowledged", Message.MessageType.FireEvent);
                    System.out.println("[DroneSubsystem] Sending to FireIncidentSubsystem, through Scheduler: " + message.getMessageData());
                    schedulerMessageBox.putMessage(message);
                }
            }
        } while (boxOpen);
    }


    public Message getStatus(){ // returns state, coords, fluid amounts, fuel, id
        return null;
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
