
import org.junit.jupiter.api.*;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import static org.junit.jupiter.api.Assertions.*;

class SchedulerUDPTest {

    private Scheduler scheduler;

    @BeforeEach
    void setup() throws InterruptedException {
        Thread.sleep(500); //needed for TearDown Time
        scheduler = new Scheduler();
        new Thread(scheduler, "SchedulerThread").start();
        Thread.sleep(200);
    }

    @AfterEach
    void teardown() {
        new Thread(() -> {
            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
            }
            scheduler.closeBox();
        }).start();
    }

    private void sendToScheduler(Message msg) throws Exception {
        byte[] data = msg.serialize().getBytes();
        java.net.DatagramSocket socket = new java.net.DatagramSocket();
        java.net.DatagramPacket packet = new java.net.DatagramPacket(
                data, data.length,
                java.net.InetAddress.getLocalHost(),
                UDPMessageBox.SCHEDULER_PORT
        );
        socket.send(packet);
        socket.close();
    }

    @Test
    void testSchedulerReceivesFireEvent() throws Exception {
        int fireZoneId = 3;
        FireEvent fire = new FireEvent("14:03:15", fireZoneId, "FIRE_DETECTED", "High", 250, 1050);
        sendToScheduler(new Message("FireIncidentSubsystem", "Scheduler", fire.serialize(), Message.MessageType.FireEvent));
        Thread.sleep(300);

        assertEquals(1, scheduler.getActiveFires().size());

        FireEvent received = scheduler.getActiveFires().get(fireZoneId).getFireEvent();
        assertEquals(3, received.getZoneId());
        assertEquals("High", received.getSeverity());
        assertEquals("FIRE_DETECTED", received.getEventType());
    }

    @Test
    void testSchedulerReceivesDroneRegistration() throws Exception {
        sendToScheduler(new Message("Scheduler", "DroneSubsystem", "1,9503", Message.MessageType.DroneRegistration));
        Thread.sleep(300);

        assertEquals(1, scheduler.getDroneRegistry().size());
        assertTrue(scheduler.getDroneRegistry().containsKey(1));
    }
}
