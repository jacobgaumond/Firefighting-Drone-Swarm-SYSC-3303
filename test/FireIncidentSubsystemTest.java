import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FireIncidentSubsystemTest {

    String inputFileName = "src/data/Sample_event_file.csv";

    MessageBox fireIncidentBox = new MessageBox();
    MessageBox schedulerBox = new MessageBox();

    private void startFireSubsystem() {
        FireIncidentSubsystem fireSys =
                new FireIncidentSubsystem(fireIncidentBox, schedulerBox, inputFileName);
        new Thread(fireSys, "FireIncidentSubsystemThread").start();
    }

    @Test
    void toSchedulerMessageBox() throws InterruptedException {
        startFireSubsystem();
        Thread.sleep(500);

        assertTrue(schedulerBox.isFull());
    }
}
