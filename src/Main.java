import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;

// Main
public class Main {
    public static void main(String[] args) {
        Scheduler scheduler = new Scheduler();

        // input test file
        String inputFileName = "/src/data/Sample_event_file.csv";

        Thread fireSys = new Thread(new FireIncidentSystem(scheduler, inputFileName), "FireSubThread");
        Thread droneSys = new Thread(new DroneSystem(scheduler, "Drone-Alpha"), "DroneThread");

        droneSys.setDaemon(true);
        fireSys.start();
        droneSys.start();
    }
}