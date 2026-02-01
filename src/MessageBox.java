public class MessageBox {
    private FireEvent currentTask = null;
    private boolean taskAvailable = false;
    private boolean responseAvailable = false;

    // add message:
    // - fire event (Fire -> Scheduler)
    // - drone instructions (Scheduler -> Drone)
    // - drone update (Drone -> Scheduler)
    // - fire update (Scheduler -> Fire)
    public synchronized void putEvent(FireEvent event) {
        // Wait if there is an unprocessed task or an unread response
        while (taskAvailable || responseAvailable) {
            try {
                wait();
            } catch (InterruptedException e) {
                return;
            }
        }
        this.currentTask = event;
        this.taskAvailable = true;
        notifyAll();
    }

    // get message
    // - receive fire event (Scheduler)
    // - receive instructions (Drone)
    // - receive update (Fire)
    public synchronized FireEvent getTask() {
        while (!taskAvailable) {
            try {
                wait();
            } catch (InterruptedException e) {
                return null;
            }
        }
        FireEvent task = currentTask;
        currentTask = null;
        taskAvailable = false;
        return task;
    }

    // Sends Fire update
    public synchronized void sendResult(FireEvent event) {
        this.currentTask = event;
        this.responseAvailable = true;
        notifyAll();
    }

    // Waits for an update
    public synchronized FireEvent waitForResponse() {
        while (!responseAvailable) {
            try {
                wait();
            } catch (InterruptedException e) {
                return null;
            }
        }
        FireEvent response = currentTask;
        responseAvailable = false;
        currentTask = null; // Clear the table for the next event
        notifyAll();
        return response;
    }
}
