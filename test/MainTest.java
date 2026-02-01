import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @Test
    void main() {
        MessageBox fire_scheduler = new MessageBox();
        MessageBox drone_scheduler = new MessageBox();

        // input test file
        String inputFileName = "/src/data/Sample_event_file.csv";

        Thread fireSys = new Thread(new FireIncidentSystem(fire_scheduler, inputFileName), "FireSubThread");
        Thread scheduler = new Thread(new Scheduler(fire_scheduler, drone_scheduler), "SchedulerThread");
        Thread droneSys = new Thread(new DroneSystem(drone_scheduler, "Drone-Alpha"), "DroneThread");

        droneSys.setDaemon(true);

        fireSys.start();
        scheduler.start();
        droneSys.start();
    }
}