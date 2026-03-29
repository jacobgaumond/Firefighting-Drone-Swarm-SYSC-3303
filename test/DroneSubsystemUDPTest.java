import org.junit.jupiter.api.*;
import java.net.*;
import static org.junit.jupiter.api.Assertions.*;

class DroneSubsystemUDPTest {

    @Test
    void testDroneReceivesMessageViaUDP() throws Exception {
        DroneSubsystem drone = new DroneSubsystem();

        DroneRequest req = new DroneRequest(
                DroneEvent.FIRE_ASSIGNED, "00:00", 1, "FIRE", "HIGH", 0, 0, 5, drone.getDroneId()
        );
        Message inbound = new Message("Scheduler", "DroneSubsystem", req.serialize(), Message.MessageType.DroneRequest);

        // Send a UDP packet directly to the drone's port
        byte[] data = inbound.serialize().getBytes();
        DatagramSocket sender = new DatagramSocket();
        DatagramPacket packet = new DatagramPacket(
                data, data.length,
                InetAddress.getLocalHost(),
               drone.getMessageBox().getPort()
        );
        sender.send(packet);
        sender.close();

        // getMessage() blocks until the listener delivers the packet into the box
        Message received = drone.getMessageBox().getMessage();

        assertNotNull(received);
        assertEquals(Message.MessageType.DroneRequest, received.getMessageType());
        sender.close();

        drone.closeBox();
    }

    @Test
    void testDroneSendsMessageToSchedulerViaUDP() throws Exception {
        // Open a raw socket on the scheduler port to catch the outbound message
        DatagramSocket schedulerSocket = new DatagramSocket(UDPMessageBox.SCHEDULER_PORT);
        schedulerSocket.setSoTimeout(3000);

        DroneSubsystem drone = new DroneSubsystem();

        Message outbound = new Message("Scheduler", "DroneSubsystem", drone.sendStatus().getMessageData(), Message.MessageType.DroneResponse);
        drone.getMessageBox().putMessage(outbound, UDPMessageBox.SCHEDULER_PORT);

        // Catch what the drone sent
        byte[] buf = new byte[200];
        DatagramPacket received = new DatagramPacket(buf, buf.length);
        schedulerSocket.receive(received);
        schedulerSocket.close();

        String receivedData = new String(buf, 0, received.getLength());
        assertNotNull(receivedData);
        assertFalse(receivedData.isEmpty());

        DroneResponse response = new DroneResponse(new Message(receivedData).getMessageData());
        assertEquals(drone.getDroneId(), response.getDroneID());

        drone.closeBox();
    }
}