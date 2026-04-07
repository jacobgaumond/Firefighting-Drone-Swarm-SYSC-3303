
import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.*;

public class EventLogger {

    private class Event {

        private long time;      // creation timestamp
        private String code;    // event code ("PLACED_COMPONENTS", "DRONE_ASSEMBLED", "WAITING", "DONE")
        private String[] data;  // extra information (which components, drone count so far, time spent)

        /**
         * Constructor for Event
         *
         * @param time creation timestamp in milliseconds
         * @param code event code
         * @param data optional extra information
         */
        public Event(long time, String code, String... data) {
            this.time = time;
            this.code = code;
            this.data = data;
        }

        // Format event into log line: Event log: [timestamp, code, ...data]
        public String format() {
            // Use the stored creation timestamp, not the current flush time
            LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault());
            String timestamp = ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));

            String log = "[" + timestamp + ", " + code;

            // Additional fields
            if (data != null) {
                for (String d : data) {
                    log += ", " + d;
                }
            }

            return log + "]";
        }
    }

    // Thread-safe queue to store log events
    private final ConcurrentLinkedQueue<Event> queue = new ConcurrentLinkedQueue<>();

    // Scheduler to flush events periodically
    private final ScheduledExecutorService scheduler;

    // Log file path and writer
    private final String logFilePath;
    private final PrintWriter fileWriter;

    /**
     * Constructor for EventLogger
     *
     * @param periodMs interval between flush() calls in ms
     * @param logFilePath path to the log file to write events to
     */
    public EventLogger(long periodMs, String logFilePath) {
        this.logFilePath = logFilePath;

        // Open log file (overwrite on each run)
        PrintWriter fw;
        try {
            fw = new PrintWriter(new BufferedWriter(new FileWriter(logFilePath, false)));
        } catch (IOException e) {
            e.printStackTrace();
            fw = null;
        }
        this.fileWriter = fw;

        // Create daemon (non-JVM-blocking) flusher thread
        ThreadFactory factory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "log-flusher-daemon");
                t.setDaemon(true);
                return t;
            }
        };

        // Create scheduler with one background thread
        scheduler = Executors.newSingleThreadScheduledExecutor(factory);

        // Periodic flush()
        scheduler.scheduleAtFixedRate(
                this::flush, // method to execute
                periodMs, // initial delay before first run
                periodMs, // interval between runs
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Add Event to log queue
     *
     * @param eventCode type of event
     * @param data optional extra information
     */
    public void log(String eventCode, String... data) {
        queue.add(new Event(
                System.currentTimeMillis(),
                eventCode,
                data
        ));
    }

    // Flush queued events to log file
    public void flush() {
        Event e;
        // Remove events until queue is empty
        while ((e = queue.poll()) != null) {
            String line = e.format();
            if (fileWriter != null) {
                fileWriter.println(line);
                fileWriter.flush();
            }
        }
    }

    // Shutdown method called once all logging is complete
    public void shutdown() {
        scheduler.shutdownNow(); // stop
        flush();                 // flush remaining events
        if (fileWriter != null) {
            fileWriter.close(); // close file
        }
    }

    // ============================ Metrics Analysis ============================
    // Average time between event creation and first drone arrival
    public String analyzeAverageEventResponseTime() throws IOException {
        Map<Integer, Double> fireCreatedTimes = new HashMap<>(); // zone: creation time
        Map<Integer, Double> firstArrivalTimes = new HashMap<>(); // zone: first arrival time

        try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath))) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.contains("FIRE_EVENT_CREATED")) {
                    String[] parts = line.split(", ");
                    int zoneId = Integer.parseInt(parts[2].split("=")[1]);
                    double timestamp = Double.parseDouble(parts[3].replace("]", ""));

                    fireCreatedTimes.put(zoneId, timestamp);

                } else if (line.contains("DRONE_ARRIVED_AT_FIRE")) {
                    String[] parts = line.split(", ");
                    int zoneId = Integer.parseInt(parts[3].split("=")[1]);
                    double timestamp = Double.parseDouble(parts[4].replace("]", ""));

                    if (!firstArrivalTimes.containsKey(zoneId)) {
                        // Only first arrival
                        firstArrivalTimes.put(zoneId, timestamp);
                    }
                }
            }
        }

        // Calculate average response time for all zones
        double totalResponseTime = 0.0;
        int firesHandled = 0;

        for (Integer zone : fireCreatedTimes.keySet()) {
            if (firstArrivalTimes.containsKey(zone)) {
                double responseTime = firstArrivalTimes.get(zone) - fireCreatedTimes.get(zone);
                totalResponseTime += responseTime;
                firesHandled++;
            }
        }

        double averageResponseTime = firesHandled > 0 ? totalResponseTime / firesHandled : 0.0;

        return "\n- Average Event Response Time: " + String.format("%.2f", averageResponseTime) + "\n";
    }

    // Maximum response time
    public String analyzeMaximumEventResponseTime() throws IOException {
        Map<Integer, Double> fireCreatedTimes = new HashMap<>(); // zone: creation time
        Map<Integer, Double> firstArrivalTimes = new HashMap<>(); // zone: first arrival time

        try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath))) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.contains("FIRE_EVENT_CREATED")) {
                    String[] parts = line.split(", ");
                    int zoneId = Integer.parseInt(parts[2].split("=")[1]);
                    double timestamp = Double.parseDouble(parts[3].replace("]", ""));

                    fireCreatedTimes.put(zoneId, timestamp);
                } else if (line.contains("DRONE_ARRIVED_AT_FIRE")) {
                    String[] parts = line.split(", ");
                    int zoneId = Integer.parseInt(parts[3].split("=")[1]);
                    double timestamp = Double.parseDouble(parts[4].replace("]", ""));

                    if (!firstArrivalTimes.containsKey(zoneId)) {
                        // Only first arrival
                        firstArrivalTimes.put(zoneId, timestamp);
                    }
                }
            }
        }

        // Find maximum response time
        double maxResponseTime = 0.0;

        for (Integer zoneId : fireCreatedTimes.keySet()) {
            if (firstArrivalTimes.containsKey(zoneId)) {
                double responseTime = firstArrivalTimes.get(zoneId) - fireCreatedTimes.get(zoneId);
                if (responseTime > maxResponseTime) {
                    maxResponseTime = responseTime;
                }
            }
        }

        return "\n- Maximum Response Time: " + String.format("%.2f", maxResponseTime) + "\n";
    }

    // Average time from event creation to full completion of service
    public String analyzeAverageEventCompletionTime() throws IOException {
        Map<Integer, Double> fireCreatedTimes = new HashMap<>(); // zone: creation time
        Map<Integer, Double> fireExtinguishedTimes = new HashMap<>(); // zone: extinguished time

        try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath))) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.contains("FIRE_EVENT_CREATED")) {
                    String[] parts = line.split(", ");
                    int zoneId = Integer.parseInt(parts[2].split("=")[1]);
                    double timestamp = Double.parseDouble(parts[3].replace("]", ""));

                    fireCreatedTimes.put(zoneId, timestamp);
                } else if (line.contains("FIRE_EXTINGUISHED")) {
                    String[] parts = line.split(", ");
                    int zoneId = Integer.parseInt(parts[2].split("=")[1]);
                    double timestamp = Double.parseDouble(parts[3].replace("]", ""));

                    fireExtinguishedTimes.put(zoneId, timestamp);
                }
            }
        }

        // Calculate running average of completion times
        double totalCompletionTime = 0.0;
        int completionCount = 0;

        for (Integer zoneId : fireCreatedTimes.keySet()) {
            if (fireExtinguishedTimes.containsKey(zoneId)) {
                double completionTime = fireExtinguishedTimes.get(zoneId) - fireCreatedTimes.get(zoneId);
                totalCompletionTime += completionTime;
                completionCount++;
            }
        }

        double averageCompletionTime = completionCount > 0 ? totalCompletionTime / completionCount : 0.0;

        return "\n- Average Event Completion Time: " + String.format("%.2f", averageCompletionTime) + "\n";
    }

    // Maximum completion time
    public String analyzeMaximumEventCompletionTime() throws IOException {
        Map<Integer, Double> fireCreatedTimes = new HashMap<>(); // zone: creation time
        Map<Integer, Double> fireExtinguishedTimes = new HashMap<>(); // zone: extinguished time

        try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath))) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.contains("FIRE_EVENT_CREATED")) {
                    String[] parts = line.split(", ");
                    int zoneId = Integer.parseInt(parts[2].split("=")[1]);
                    double timestamp = Double.parseDouble(parts[3].replace("]", ""));

                    fireCreatedTimes.put(zoneId, timestamp);
                } else if (line.contains("FIRE_EXTINGUISHED")) {
                    String[] parts = line.split(", ");
                    int zoneId = Integer.parseInt(parts[2].split("=")[1]);
                    double timestamp = Double.parseDouble(parts[3].replace("]", ""));

                    fireExtinguishedTimes.put(zoneId, timestamp);
                }
            }
        }

        // Find maximum completion time
        double maxCompletionTime = 0.0;

        for (Integer zoneId : fireCreatedTimes.keySet()) {
            if (fireExtinguishedTimes.containsKey(zoneId)) {
                double completionTime = fireExtinguishedTimes.get(zoneId) - fireCreatedTimes.get(zoneId);
                if (completionTime > maxCompletionTime) {
                    maxCompletionTime = completionTime;
                }
            }
        }

        return "\n- Maximum Event Completion Time: " + String.format("%.2f", maxCompletionTime) + "\n";
    }

    // Drone Utilization time active vs total
    public String analyzeDroneUtilization() throws IOException {
        double firstFireEventTime = 0.0;
        double lastEventTime = 0.0;
        Map<Integer, Double> droneActiveTime = new HashMap<>(); // drone: total active time
        Map<Integer, Double> droneLastAssignedTime = new HashMap<>(); // drone: last DRONE_ASSIGNED timestamp 

        try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath))) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.contains("FIRST_FIRE_EVENT")) {
                    String[] parts = line.split(", ");
                    firstFireEventTime = Double.parseDouble(parts[2].replace("]", ""));
                } else if (line.contains("DRONE_ASSIGNED")) {
                    String[] parts = line.split(", ");
                    int drone = Integer.parseInt(parts[2].split("=")[1]);
                    double timestamp = Double.parseDouble(parts[4].replace("]", ""));
                    droneLastAssignedTime.put(drone, timestamp);
                    lastEventTime = Math.max(lastEventTime, timestamp);
                } else if (line.contains("DRONE_IDLE")) {
                    String[] parts = line.split(", ");
                    int drone = Integer.parseInt(parts[2].split("=")[1]);
                    double timestamp = Double.parseDouble(parts[3].replace("]", ""));

                    if (droneLastAssignedTime.containsKey(drone)) {
                        // calc active time since last assigned
                        double activeTime = timestamp - droneLastAssignedTime.get(drone);

                        if (!droneActiveTime.containsKey(drone)) {
                            droneActiveTime.put(drone, 0.0);
                        }
                        droneActiveTime.put(drone, droneActiveTime.get(drone) + activeTime);
                        droneLastAssignedTime.remove(drone);
                    }
                    lastEventTime = Math.max(lastEventTime, timestamp);
                }
            }
        }

        // Build result
        String result = "\n- Drone Utilization:\n";
        result += "Drone ID - Utilization\n";
        result += "-------------------------------\n";

        double totalSimulationTime = lastEventTime - firstFireEventTime;

        for (Integer drone : droneActiveTime.keySet()) {
            double activeTime = droneActiveTime.get(drone);
            double utilizationPercent = (totalSimulationTime > 0) ? (activeTime / totalSimulationTime) * 100.0 : 0.0;

            int res = (int) Math.round(utilizationPercent);

            result += drone + " - " + res + "%\n";
        }

        return result;
    }

    public String analyzeAverageDroneIdleTime() throws IOException {
        Map<Integer, Double> droneLastIdleTime = new HashMap<>();
        Map<Integer, Double> droneTotalIdleTime = new HashMap<>();
        double allFiresExtinguishedTime = 0.0;

        try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("ALL_FIRES_EXTINGUISHED")) {
                    String[] parts = line.split(", ");
                    allFiresExtinguishedTime = Double.parseDouble(parts[2].replace("]", ""));
                } else if (line.contains("DRONE_IDLE")) {
                    String[] parts = line.split(", ");
                    int drone = Integer.parseInt(parts[2].split("=")[1]);
                    double timestamp = Double.parseDouble(parts[3].replace("]", ""));
                    droneLastIdleTime.put(drone, timestamp);
                } else if (line.contains("DRONE_ASSIGNED")) {
                    String[] parts = line.split(", ");
                    int drone = Integer.parseInt(parts[2].split("=")[1]);
                    double timestamp = Double.parseDouble(parts[4].replace("]", ""));
                    if (droneLastIdleTime.containsKey(drone)) {
                        double idleTime = timestamp - droneLastIdleTime.get(drone);
                        droneTotalIdleTime.merge(drone, idleTime, Double::sum);
                        droneLastIdleTime.remove(drone);
                    }
                }
            }
        }

        for (Map.Entry<Integer, Double> entry : droneLastIdleTime.entrySet()) {
            double idleTime = allFiresExtinguishedTime - entry.getValue();
            droneTotalIdleTime.merge(entry.getKey(), idleTime, Double::sum);
        }

        double total = droneTotalIdleTime.values().stream().mapToDouble(Double::doubleValue).sum();
        double avg = droneTotalIdleTime.isEmpty() ? 0.0 : total / droneTotalIdleTime.size();
        return "\n- Average Drone Idle Time: " + String.format("%.2f", avg) + "s\n";
    }

    public String analyzeDroneFlightTime() throws IOException {
        Map<Integer, Double> droneLastDepartTime = new HashMap<>();
        Map<Integer, Double> droneTotalFlightTime = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("DRONE_DEPARTED")) {
                    String[] parts = line.split(", ");
                    int drone = Integer.parseInt(parts[2].split("=")[1]);
                    double timestamp = Double.parseDouble(parts[3].replace("]", ""));

                    droneLastDepartTime.put(drone, timestamp);
                } else if (line.contains("DRONE_ARRIVED_AT_FIRE")) {
                    String[] parts = line.split(", ");
                    int drone = Integer.parseInt(parts[2].split("=")[1]);
                    double timestamp = Double.parseDouble(parts[4].replace("]", ""));

                    if (droneLastDepartTime.containsKey(drone)) {
                        double flightTime = timestamp - droneLastDepartTime.get(drone);
                        droneTotalFlightTime.merge(drone, flightTime, Double::sum);
                        droneLastDepartTime.remove(drone);
                    }
                }
            }
        }

        String result = "\n- Drone Flight Times:\n";
        for (Map.Entry<Integer, Double> entry : droneTotalFlightTime.entrySet()) {
            result += "Drone " + entry.getKey() + ": " + String.format("%.2f", entry.getValue()) + "s\n";
        }
        return result;
    }

    public String analyzeTotalSimulationTime() throws IOException {
        double firstFireTime = 0.0;
        double allExtinguishedTime = 0.0;

        try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("FIRST_FIRE_EVENT")) {
                    String[] parts = line.split(", ");
                    firstFireTime = Double.parseDouble(parts[2].replace("]", ""));
                } else if (line.contains("ALL_FIRES_EXTINGUISHED")) {
                    String[] parts = line.split(", ");
                    allExtinguishedTime = Double.parseDouble(parts[2].replace("]", ""));
                }
            }
        }

        double total = allExtinguishedTime - firstFireTime;
        return "\n- Total Time to Extinguish All Fires: " + String.format("%.2f", total) + "s\n";
    }
}
