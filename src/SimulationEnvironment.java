
/**
 * Shared simulation environment for all subsystems.
 *
 * We actively avoid shared resources in distributed systems, but this
 * clock is essential for simulation purposes, allowing us to to change
 * simulation speed while having all subsystems align.
 *
 * In a real environment, each subsystem would use its own system time, as they would match.
 */
public class SimulationEnvironment {

    public static final int SIMULATION_SPEED = 1;
    public static final int SIMULATION_SECOND_MS = (1000 / SIMULATION_SPEED);

    public volatile static long currentTime = 0;

    public static synchronized void startClock(long startTime) {
        currentTime = startTime;

        // Ticking thread
        Thread clockThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(SIMULATION_SECOND_MS);
                    currentTime++;
                } catch (InterruptedException e) {
                    break;
                }
            }
        });

        clockThread.setDaemon(true); // closes with others
        clockThread.start();
    }

    public static long getCurrentTimeSeconds() {
        return currentTime;
    }

    public static String getFormattedTime() {
        long timeSeconds = currentTime;
        int hours = (int) (timeSeconds / 3600);
        int minutes = (int) ((timeSeconds % 3600) / 60);
        int seconds = (int) (timeSeconds % 60);
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
