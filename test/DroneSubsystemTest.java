import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DroneSubsystemTest {
    MessageBox droneBox = new MessageBox();
    MessageBox schedulerBox = new MessageBox();

    private void startDroneSubsystem() {
        DroneSubsystem droneSys = new DroneSubsystem(droneBox, schedulerBox);
        new Thread(droneSys, "DroneSubsystemThread").start();
    }

    @Test
    void toSchedulerMessageBox() throws InterruptedException {
        startDroneSubsystem();
        Message testMessage = new Message("DroneSubsystem", "Scheduler", "TEST MESSAGE", Message.MessageType.FireEvent);

        droneBox.putMessage(testMessage); // From scheduler
        assertTrue(droneBox.isFull());

        Thread.sleep(500);

        assertFalse(droneBox.isFull());
        assertTrue(schedulerBox.isFull());
    }
}
