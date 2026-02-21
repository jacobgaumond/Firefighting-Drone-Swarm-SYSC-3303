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

        // GUI TEST TO BE REMOVED
        gui.createDroneLabel(1);
        javax.swing.Timer loopTimer = new javax.swing.Timer(6000, null);
        final boolean[] isOutbound = {true};
        loopTimer.addActionListener(e -> {
            if (isOutbound[0]) {
                gui.moveDroneToZone(1, 5);
                isOutbound[0] = false;
            } else {
                gui.returnDrone(1);
                isOutbound[0] = true;
            }
        });
        javax.swing.Timer startTimer = new javax.swing.Timer(1500, e -> {
            gui.moveDroneToZone(1, 5);
            isOutbound[0] = false;
            loopTimer.start();
            ((javax.swing.Timer) e.getSource()).stop();
        });
        startTimer.setRepeats(false);
        startTimer.start();

        // Setup Message Boxes
        MessageBox schedulerBox = new MessageBox();
        MessageBox fireIncidentBox = new MessageBox();
        MessageBox droneBox = new MessageBox();

        // Setup Threads
        Thread scheduler = new Thread(new Scheduler(schedulerBox, fireIncidentBox, droneBox),
                "SchedulerThread");
        Thread fireIncidentSubsystem = new Thread(new FireIncidentSubsystem(fireIncidentBox, schedulerBox, inputFileName),
                "FireIncidentSubsystemThread");
        Thread droneSubsystem = new Thread(new DroneSubsystem(droneBox, schedulerBox),
                "DroneSubsystemThread");

        // Start Threads
        scheduler.start();
        fireIncidentSubsystem.start();
        droneSubsystem.start();
    }
}
