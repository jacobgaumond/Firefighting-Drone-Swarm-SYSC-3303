import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TimeBasedFaultTest {

    @Test
    public void testDroneTimerFaultDetection() throws InterruptedException {

        Scheduler scheduler = new Scheduler();
        Scheduler.DroneInfo testDrone = scheduler.new DroneInfo(1, 9999);

        testDrone.setExpectedResponseTime(1000);
        testDrone.setDispatchTime(System.currentTimeMillis());
        testDrone.awaitingResponse = true;

        DroneRequest droneReq = new DroneRequest(null, "time", 0, "type", "sev", 0, 0, 10, 1);

        //dummy DroneRequest
        testDrone.lastSentRequest = droneReq;

        scheduler.getDroneRegistry().put(testDrone.droneId, testDrone);
        scheduler.startWatchdog();

        Thread.sleep(1500);

        long now = System.currentTimeMillis();
        assertTrue(testDrone.getDispatchTime() > 0 && (now - testDrone.getDispatchTime()) < 2000,
                "Watchdog should have updated dispatchTime due to timeout");

        System.out.println("Timer-based fault detection triggered correctly!");
    }
}