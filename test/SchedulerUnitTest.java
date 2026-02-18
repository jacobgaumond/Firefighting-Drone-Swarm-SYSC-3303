import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.BeforeEach.*;

import java.util.Queue;

public class SchedulerUnitTest {

    @Test
        void testSchedulerTasksAssignment() throws InterruptedException {
        MessageBox schedulerBox = new MessageBox();
        MessageBox fireIncidentBox = new MessageBox();
        MessageBox droneBox = new MessageBox();

        Scheduler scheduler = new Scheduler(schedulerBox, fireIncidentBox, droneBox);
        new Thread(scheduler, "SchedulerThread").start();

        Message fireTask1 = new Message("Scheduler", "FireIncidentSubsystem", "FireTask1", Message.MessageType.FireEvent);
        Message fireTask2 = new Message("Scheduler", "FireIncidentSubsystem", "FireTask2", Message.MessageType.FireEvent);

        schedulerBox.putMessage(fireTask1);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        boolean received = false;
        for (int i = 0; i < 10; i++) {
            if (droneBox.isFull()) {
                received = true;
                break;
            }
            Thread.sleep(100);
        }
        assertTrue(received, "DroneBox should have received the task");

        assertEquals(DroneState.EN_ROUTE, scheduler.getDrone().getCurrentState(), "Drone should be EN_ROUTE after first task");

        //second task while drone is busy
        schedulerBox.putMessage(fireTask2);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        //check if second task is queued
        Queue<Message> queue = scheduler.getTaskQueue();
        assertEquals(1, queue.size(), "1 task should be queued");
        assertEquals(fireTask2.getMessageData(), queue.peek().getMessageData(), "Queued task should match fireTask2");


        //drone finishes first task
        Message firstAssigned = droneBox.getMessage();
        assertEquals(fireTask1.getMessageData(), firstAssigned.getMessageData(), "First assigned message should be fireTask1");
        assertNotNull(firstAssigned.getMessageData(), "First assigned message should not be null");


        //simulate the drone finished the first task
        Message ack = new Message("Scheduler", "DroneSubsystem", "Acknowledged", Message.MessageType.DroneResponse);
        schedulerBox.putMessage(ack);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //2nd task assigned

//        Message secondAssigned = null;
//        for (int i = 0; i < 50; i++) {
//            synchronized (droneBox) {
//                if (droneBox.isFull()) {
//                    secondAssigned = droneBox.getMessage();
//                    break;
//                }
//            }
//            Thread.sleep(100);
//        }


        Message secondAssigned = droneBox.getMessage();
        assertNotNull(secondAssigned, "Drone should have received the second task");
        assertEquals(fireTask2.getMessageData(), secondAssigned.getMessageData(), "Assigned task should match fireTask2");

//
//        //should be busy with task2
//        assertEquals(DroneState.EN_ROUTE, scheduler.getDrone().getCurrentState(), "Drone should be EN_ROUTE after first task");
//
//
//        //simulate drone finished second task
//        Message ack2 = new Message("Scheduler", "DroneSubsystem", "Acknowledged", Message.MessageType.DroneResponse);
//        schedulerBox.putMessage(ack2);
//
//
//        //drone should be now idle
//        assertEquals(DroneState.IDLE, scheduler.getDrone().getCurrentState(), "Drone should be IDLE");
//        assertTrue(queue.isEmpty(), "Queue should be empty");
//        assertTrue(scheduler.getTaskQueue().isEmpty(), "Queue should be empty");
//
//
//        schedulerBox.closeBox();
//        fireIncidentBox.closeBox();
//        droneBox.closeBox();
    }



}
