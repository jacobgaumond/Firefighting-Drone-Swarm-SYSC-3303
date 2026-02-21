public class Main {
    public static void main(String[] args) {
        String inputFileName = "src/data/Sample_event_file.csv";
        String zoneFileName = "src/data/Sample_zone_file.csv";

        // Load zones
        ZoneMap.loadZones(zoneFileName);
        ZoneMap.printZones();

        // Build GUI
        DroneGUI gui = new DroneGUI();
        gui.setVisible(true);

        // Setup Message Boxes
        MessageBox schedulerBox = new MessageBox();
        MessageBox fireIncidentBox = new MessageBox();
        MessageBox droneBox = new MessageBox();

        // Setup Threads
        Thread scheduler = new Thread(new Scheduler(schedulerBox, fireIncidentBox, droneBox, gui),
                "SchedulerThread");
        Thread fireIncidentSubsystem = new Thread(new FireIncidentSubsystem(fireIncidentBox, schedulerBox, inputFileName, gui),
                "FireIncidentSubsystemThread");
        Thread droneSubsystem = new Thread(new DroneSubsystem(droneBox, schedulerBox, gui),
                "DroneSubsystemThread");

        // Start Threads
        scheduler.start();
        fireIncidentSubsystem.start();
        droneSubsystem.start();
    }
}
