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
                handleDroneMessage(message);
                break;
            case "FireIncidentSubsystem":
                handleFireIncidentMessage(message);
                break;
            case "Scheduler":
                //checks if the incoming message is a fire task
                if (message.getMessageType() == Message.MessageType.FireEvent) {
                    handleFireIncidentMessage(message); //assign or queue task
                } else if (message.getMessageType() == Message.MessageType.DroneResponse) {
                    handleDroneMessage(message); //drone finished a task
                }
                break;
            default:
                System.out.println("[Scheduler] Unknown destination: " + message.getDestinationName());
        }

    }




    private void assignTaskToDrone(Message task){
        //change its state
        drone.setCurrentState(DroneState.EN_ROUTE); //dron is busy
        System.out.println("[Scheduler] Assigning task to Drone: " + task.getMessageData());
        droneMessageBox.putMessage(task); //send task to droneSubsystem
    }

    //handles if the (task finished) -> assigns next task in the queue or marks drone as idle
    private void handleDroneMessage(Message message){
        if(message.getMessageData().equals("Acknowledged")){
            System.out.println("[Scheduler] Drone finished task");

            //check for queued tasks
            if(!taskQueue.isEmpty()){
                Message nextTask = taskQueue.poll();
                assignTaskToDrone(nextTask);
            } else  {
                drone.setCurrentState(DroneState.IDLE);
                System.out.println("[Scheduler] No more tasks to process");
            }
        }
    }

    private void handleFireIncidentMessage(Message message){
        System.out.println("[Scheduler] Handling fire message: " + message.getMessageData());

        //only handle FireEvent or DroneRequest messages
        if(message.getMessageType() == Message.MessageType.FireEvent || message.getMessageType() == Message.MessageType.DroneRequest){

            //handles assigning of tasks for now
            switch(drone.getCurrentState()){
                case IDLE:
                    assignTaskToDrone(message);
                    break;

                case EN_ROUTE:
                    taskQueue.add(message);
                    System.out.println("[Scheduler] Drone busy (EN_ROUTE), task queued: " + message.getMessageData());
                    System.out.println("[Scheduler] TaskQUEUE: Current queue (" + taskQueue.size() + " tasks): "
                            + taskQueue.stream().map(Message::getMessageData).toList());
                    break;
                case DROPPING_AGENT:
                case REFILLING:
                    //drone is busy -> queue the task
                    taskQueue.add(message);
                    System.out.println("[Scheduler] Drone busy refilling, tasks queued: " + message.getMessageData());
                    break;

                case FAULTED:
                    System.out.println("[Scheduler] Drone is faulted, cannot assign task: " + message.getMessageData());
                    break;
            }
        }
    }


    //getters
    public Drone getDrone() {
        return this.drone;
    }

    public Queue<Message> getTaskQueue() {
        return taskQueue;
    }
}
