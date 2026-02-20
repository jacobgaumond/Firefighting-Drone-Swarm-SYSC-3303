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

    private Queue<Message> taskQueue =  new LinkedList<>();
    private DroneSubsystem drone;

    public Scheduler(MessageBox incomingMessageBox, MessageBox fireIncidentMessageBox, MessageBox droneMessageBox) {
        this.incomingMessageBox     = incomingMessageBox;

        this.fireIncidentMessageBox = fireIncidentMessageBox;
        this.droneMessageBox        = droneMessageBox;

        this.drone = new DroneSubsystem();
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



    private void processIncomingMessageBox(Message message) {
        System.out.println("\n[Scheduler] Received from " + message.getSourceName() + ": \n" + message.getMessageData());

        //if for droneSub or fireIncidentSub
//        if(message.getDestinationName().equals("DroneSubsystem")) {
//            handleDroneMessage(message);
//        } else if(message.getDestinationName().equals("FireIncidentSubsystem")) {
//            handleFireIncidentMessage(message);
//        }

        switch (message.getDestinationName()) {
            case "DroneSubsystem":
                processDroneAcknowledgement(message);
                break;
            case "FireIncidentSubsystem":
                processEvent(message);
                break;
            case "Scheduler":
                //checks if the incoming message is a fire task
                if (message.getMessageType() == Message.MessageType.FireEvent) {
                    processEvent(message); //assign or queue task
                } else if (message.getMessageType() == Message.MessageType.DroneResponse) {
                    processDroneAcknowledgement(message); //drone finished a task
                }
                break;
            default:
                System.out.println("[Scheduler] Unknown destination: " + message.getDestinationName());
        }

    }


    private void assignTaskToDrone(FireEvent fireEvent) {

        //send to droneSubsystem
        Message droneMessage = new Message(
                "DroneSubsystem",
                "Scheduler",
                fireEvent.serialize(),
                Message.MessageType.FireEvent
        );

        System.out.println("[Scheduler] Assigning task to Drone: \n" + fireEvent.toString());
        droneMessageBox.putMessage(droneMessage); //send task to droneSubsystem
    }

    //handles if the (task finished) -> assigns next task in the queue or marks drone as idle
    private void processDroneAcknowledgement(Message message){
        //only handles drone acknowledged
        if(!message.getMessageData().equals("Acknowledged")) return;

        System.out.println("[Scheduler] Drone finished task");

        if(!taskQueue.isEmpty()){
            //take next task from the queue
            Message nextTaskMessage = taskQueue.poll();
            FireEvent nextTask = new FireEvent(nextTaskMessage.getMessageData());

            //next task
            drone.handleEvent(DroneEvent.FIRE_ASSIGNED, nextTask.serialize());

            //send to drone nextTask
            assignTaskToDrone(nextTask);
            System.out.println("[Scheduler] Assigning task to Drone: \n" + nextTask);
        } else {
            drone.setCurrentState(DroneState.IDLE);
            System.out.println("[Scheduler] No more task to process, drone is idle");
        }

    }

    private void processEvent(Message message){
        //only handle FireEvent
        if(message.getMessageType() != Message.MessageType.FireEvent) return;
        //deserialize the fire event
        FireEvent fireEvent = new FireEvent(message.getMessageData());
        System.out.println("[Scheduler] Handling fire message: " + message.getMessageData());

        //check if drone is IDLE
        if(drone.getCurrentState() == DroneState.IDLE){
            //update FSM
            drone.handleEvent(DroneEvent.FIRE_ASSIGNED, fireEvent.serialize());

            assignTaskToDrone(fireEvent);
            System.out.println("[Scheduler] Fire event send to drone: " + fireEvent);
        } else{
            //drone busy -> queue the task
            taskQueue.add(message);
            System.out.println("[Scheduler] Drone busy (" + drone.getCurrentState() + "), task queued: " + fireEvent);
            System.out.println("[Scheduler] Current task queue size: " + taskQueue.size());
        }
    }


    //getters
    public DroneSubsystem getDrone() {
        return this.drone;
    }

    public Queue<Message> getTaskQueue() {
        return taskQueue;
    }


}
