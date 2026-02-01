import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SchedulerTest {
    MessageBox schedulerBox = new MessageBox();
    MessageBox fireIncidentBox = new MessageBox();
    MessageBox droneBox = new MessageBox();

    @Test
    void toFireIncidentMessageBox() throws InterruptedException {
        Scheduler scheduler = new Scheduler(schedulerBox, fireIncidentBox, droneBox);
        new Thread(scheduler, "SchedulerThread").start();

        Message testMessage = new Message("FireIncidentSubsystem", "Scheduler", "TEST MESSAGE", Message.MessageType.FireEvent);
        schedulerBox.putMessage(testMessage);
        assertTrue(schedulerBox.isFull());

        Thread.sleep(500);

        assertFalse(schedulerBox.isFull());
        assertTrue(fireIncidentBox.isFull());
    }

    void toDroneMessageBox() throws InterruptedException {
        Scheduler scheduler = new Scheduler(schedulerBox, fireIncidentBox, droneBox);
        new Thread(scheduler, "SchedulerThread").start();

        Message testMessage = new Message("DroneSubsystem", "Scheduler", "TEST MESSAGE", Message.MessageType.FireEvent);
        schedulerBox.putMessage(testMessage);
        assertTrue(schedulerBox.isFull());

        Thread.sleep(500);

        assertFalse(schedulerBox.isFull());
        assertTrue(droneBox.isFull());
    }
}
