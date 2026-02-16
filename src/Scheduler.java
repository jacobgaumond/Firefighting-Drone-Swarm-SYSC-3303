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

    private Queue<String> taskQueue =  new LinkedList<>();
    private Drone drone;

    public Scheduler(MessageBox incomingMessageBox, MessageBox fireIncidentMessageBox, MessageBox droneMessageBox) {
        this.incomingMessageBox     = incomingMessageBox;

        this.fireIncidentMessageBox = fireIncidentMessageBox;
        this.droneMessageBox        = droneMessageBox;

        this.drone = new Drone();
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
                System.out.println("[Scheduler] Received from " + message.getSourceName() + ": " + message.getMessageData());

                if (message.getDestinationName().equals("FireIncidentSubsystem")) {
                    System.out.println("[Scheduler] Sending to FireIncidentSubsystem: " + message.getMessageData());
                    fireIncidentMessageBox.putMessage(message);
                }
                else if (message.getDestinationName().equals("DroneSubsystem")) {
                    System.out.println("[Scheduler] Sending to DroneSubsystem: " + message.getMessageData());
                    droneMessageBox.putMessage(message);
                }

                if(drone.getCurrentState() == DroneState.IDLE){
                    //assign task

                } else{
                    //taskQueue.add(fireevent);
                }

            }
        } while (boxOpen);
    }


    private void assignTaskToDrone(){
        //change its state
        drone.setCurrentState(DroneState.EN_ROUTE);

    }





}
