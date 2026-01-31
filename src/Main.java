public class Main {
    public static void main(String[] args) {
        String inputFileName = "src/data/Sample_event_file.csv";

        MessageBox schedulerBox     = new MessageBox();
        MessageBox fireIncidentBox  = new MessageBox();
        MessageBox droneBox         = new MessageBox();

        Thread scheduler = new Thread(new Scheduler(schedulerBox, fireIncidentBox, droneBox),
                "SchedulerThread");
        Thread fireIncidentSubsystem = new Thread(new FireIncidentSubsystem(fireIncidentBox, schedulerBox, inputFileName),
                "FireIncidentSubsystemThread");
        Thread droneSubsystem = new Thread(new DroneSubsystem(droneBox, schedulerBox),
                "DroneSubsystemThread");

        scheduler.start();
        fireIncidentSubsystem.start();
        droneSubsystem.start();
    }
}
