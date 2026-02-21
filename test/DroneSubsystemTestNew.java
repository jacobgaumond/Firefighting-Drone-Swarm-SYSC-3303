import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DroneSubsystemTestNew {

    private MessageBox droneBox;
    private MessageBox schedulerBox;
    private DroneSubsystem drone;
    private Thread droneThread;

    @BeforeEach
    void setup() {
        droneBox = new MessageBox();
        schedulerBox = new MessageBox();
        drone = new DroneSubsystem(droneBox, schedulerBox);

        // Start the drone in its own thread
        droneThread = new Thread(drone, "DroneThread");
        droneThread.start();
    }

    @Test
    void testFlyToFireAndReturn() throws InterruptedException {
        //assign a fire
        DroneRequest fireEvent = new DroneRequest(
                DroneEvent.FIRE_ASSIGNED, "12:00", 1,
                "Fire", "High", 3, 4, 5, 1
        );
        droneBox.putMessage(new Message(
                "Scheduler", "Test", fireEvent.serialize(),
                Message.MessageType.DroneRequest
        ));

        Thread.sleep(300);

        //check drone reached fire
        Message responseMsg;
        do {
            responseMsg = schedulerBox.getMessage();
        } while (responseMsg != null && responseMsg.getMessageType() != Message.MessageType.DroneResponse);

        assertNotNull(responseMsg, "DroneResponse was not received");


        //use DroneResponse for assertions
        DroneResponse response = new DroneResponse(responseMsg.getMessageData());
        assertEquals(3, response.getX());
        assertEquals(4, response.getY());
        assertEquals(DroneState.ARRIVED_AT_FIRE.toString(), response.getState());



        //return to base
        drone.returnToBase("RETURN_BASE");
        assertEquals(0, drone.getCoordX());
        assertEquals(0, drone.getCoordY());
    }

}