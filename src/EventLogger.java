
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
            // Convert from milliseconds to simulation seconds
            long simulationSeconds = time / 1000;
            int hours = (int) (simulationSeconds / 3600);
            int minutes = (int) ((simulationSeconds % 3600) / 60);
            int seconds = (int) (simulationSeconds % 60);
            String timestamp = String.format("%02d:%02d:%02d", hours, minutes, seconds);

            String log = "[" + timestamp + ", " + code;

            // Add data fields
            if (data != null) {
                for (String d : data) {
                    log += ", " + d;
                }
            }

            // Add simulation time as the last field (for metrics)
            log += ", " + simulationSeconds;

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
    /*public void log(String eventCode, String... data) {

        queue.add(new Event(
                SimulationEnvironment.getCurrentTimeSeconds(),
                eventCode,
                data
        ));
    }*/
    public void log(String eventCode, long simulationTimeSeconds, String... data) {
        // Convert simulation seconds to milliseconds for consistency
        long simulationTimeMs = simulationTimeSeconds * 1000;

        queue.add(new Event(
                simulationTimeMs,
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
    public String analyzeAverageEventResponseTime() throws IOException {
        // Use a Map of Queues to handle multiple fires in the same zone
        Map<Integer, Queue<Double>> fireCreatedTimes = new HashMap<>();
        Map<Integer, Queue<Double>> arrivalTimes = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("FIRE_EVENT_CREATED")) {
                    String[] parts = line.split(", ");
                    int zoneId = Integer.parseInt(parts[2].split("=")[1]);
                    double timestamp = Double.parseDouble(parts[parts.length - 1].replace("]", ""));

                    // Add this fire creation to the queue for this zone
                    fireCreatedTimes.computeIfAbsent(zoneId, k -> new LinkedList<>()).add(timestamp);

                } else if (line.contains("DRONE_ARRIVED_AT_FIRE")) {
                    String[] parts = line.split(", ");
                    // Note: Indexing for DRONE_ARRIVED_AT_FIRE is different (parts[3] is zone)
                    int zoneId = Integer.parseInt(parts[3].split("=")[1]);
                    double timestamp = Double.parseDouble(parts[parts.length - 1].replace("]", ""));

                    // Add this arrival to the queue for this zone
                    arrivalTimes.computeIfAbsent(zoneId, k -> new LinkedList<>()).add(timestamp);
                }
            }
        }

        double totalResponseTime = 0.0;
        int firesHandled = 0;

        // Iterate through all zones where a fire was created
        for (Integer zone : fireCreatedTimes.keySet()) {
            Queue<Double> createdQueue = fireCreatedTimes.get(zone);
            Queue<Double> arrivedQueue = arrivalTimes.get(zone);

            // While we have a matching pair of (Created, Arrived) for this specific zone
            while (createdQueue != null && arrivedQueue != null &&
                    !createdQueue.isEmpty() && !arrivedQueue.isEmpty()) {

                double createdTime = createdQueue.poll(); // Get oldest creation
                double arrivalTime = arrivedQueue.poll(); // Get oldest arrival

                double responseTime = arrivalTime - createdTime;
                totalResponseTime += responseTime;
                firesHandled++;
            }
        }

        double averageResponseTime = firesHandled > 0 ? totalResponseTime / firesHandled : 0.0;
        return "\n- Average Event Response Time: " + String.format("%.2f", averageResponseTime) + " seconds\n";
    }

    public String analyzeMaximumEventResponseTime() throws IOException {
        Map<Integer, Double> fireCreatedTimes = new HashMap<>();
        Map<Integer, Double> firstArrivalTimes = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath))) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.contains("FIRE_EVENT_CREATED")) {
                    String[] parts = line.split(", ");
                    int zoneId = Integer.parseInt(parts[2].split("=")[1]);
                    String lastPart = parts[parts.length - 1];
                    double timestamp = Double.parseDouble(lastPart.replace("]", ""));
                    fireCreatedTimes.put(zoneId, timestamp);

                } else if (line.contains("DRONE_ARRIVED_AT_FIRE")) {
                    String[] parts = line.split(", ");
                    int zoneId = Integer.parseInt(parts[3].split("=")[1]);
                    String lastPart = parts[parts.length - 1];
                    double timestamp = Double.parseDouble(lastPart.replace("]", ""));

                    if (!firstArrivalTimes.containsKey(zoneId)) {
                        firstArrivalTimes.put(zoneId, timestamp);
                    }
                }
            }
        }

        double maxResponseTime = 0.0;
        for (Integer zoneId : fireCreatedTimes.keySet()) {
            if (firstArrivalTimes.containsKey(zoneId)) {
                double responseTime = firstArrivalTimes.get(zoneId) - fireCreatedTimes.get(zoneId);
                if (responseTime > maxResponseTime) {
                    maxResponseTime = responseTime;
                }
            }
        }

        return "\n- Maximum Response Time: " + String.format("%.2f", maxResponseTime) + " seconds\n";
    }

    public String analyzeAverageEventCompletionTime() throws IOException {
        Map<Integer, Double> fireCreatedTimes = new HashMap<>();
        Map<Integer, Double> fireExtinguishedTimes = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath))) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.contains("FIRE_EVENT_CREATED")) {
                    String[] parts = line.split(", ");
                    int zoneId = Integer.parseInt(parts[2].split("=")[1]);
                    String lastPart = parts[parts.length - 1];
                    double timestamp = Double.parseDouble(lastPart.replace("]", ""));
                    fireCreatedTimes.put(zoneId, timestamp);

                } else if (line.contains("FIRE_EXTINGUISHED")) {
                    String[] parts = line.split(", ");
                    int zoneId = Integer.parseInt(parts[2].split("=")[1]);
                    String lastPart = parts[parts.length - 1];
                    double timestamp = Double.parseDouble(lastPart.replace("]", ""));
                    fireExtinguishedTimes.put(zoneId, timestamp);
                }
            }
        }

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
        return "\n- Average Event Completion Time: " + String.format("%.2f", averageCompletionTime) + " seconds\n";
    }

    public String analyzeMaximumEventCompletionTime() throws IOException {
        Map<Integer, Double> fireCreatedTimes = new HashMap<>();
        Map<Integer, Double> fireExtinguishedTimes = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath))) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.contains("FIRE_EVENT_CREATED")) {
                    String[] parts = line.split(", ");
                    int zoneId = Integer.parseInt(parts[2].split("=")[1]);
                    String lastPart = parts[parts.length - 1];
                    double timestamp = Double.parseDouble(lastPart.replace("]", ""));
                    fireCreatedTimes.put(zoneId, timestamp);

                } else if (line.contains("FIRE_EXTINGUISHED")) {
                    String[] parts = line.split(", ");
                    int zoneId = Integer.parseInt(parts[2].split("=")[1]);
                    String lastPart = parts[parts.length - 1];
                    double timestamp = Double.parseDouble(lastPart.replace("]", ""));
                    fireExtinguishedTimes.put(zoneId, timestamp);
                }
            }
        }

        double maxCompletionTime = 0.0;
        for (Integer zoneId : fireCreatedTimes.keySet()) {
            if (fireExtinguishedTimes.containsKey(zoneId)) {
                double completionTime = fireExtinguishedTimes.get(zoneId) - fireCreatedTimes.get(zoneId);
                if (completionTime > maxCompletionTime) {
                    maxCompletionTime = completionTime;
                }
            }
        }

        return "\n- Maximum Event Completion Time: " + String.format("%.2f", maxCompletionTime) + " seconds\n";
    }

    public String analyzeDroneUtilization() throws IOException {
        double firstFireEventTime = 0.0;
        double lastEventTime = 0.0;
        Map<Integer, Double> droneActiveTime = new HashMap<>();
        Map<Integer, Double> droneLastAssignedTime = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath))) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.contains("FIRST_FIRE_EVENT")) {
                    String[] parts = line.split(", ");
                    String lastPart = parts[parts.length - 1];
                    firstFireEventTime = Double.parseDouble(lastPart.replace("]", ""));

                } else if (line.contains("DRONE_ASSIGNED")) {
                    String[] parts = line.split(", ");
                    int drone = Integer.parseInt(parts[2].split("=")[1]);
                    String lastPart = parts[parts.length - 1];
                    double timestamp = Double.parseDouble(lastPart.replace("]", ""));
                    droneLastAssignedTime.put(drone, timestamp);
                    lastEventTime = Math.max(lastEventTime, timestamp);

                } else if (line.contains("DRONE_IDLE")) {
                    String[] parts = line.split(", ");
                    int drone = Integer.parseInt(parts[2].split("=")[1]);
                    String lastPart = parts[parts.length - 1];
                    double timestamp = Double.parseDouble(lastPart.replace("]", ""));

                    if (droneLastAssignedTime.containsKey(drone)) {
                        double activeTime = timestamp - droneLastAssignedTime.get(drone);
                        droneActiveTime.put(drone, droneActiveTime.getOrDefault(drone, 0.0) + activeTime);
                        droneLastAssignedTime.remove(drone);
                    }
                    lastEventTime = Math.max(lastEventTime, timestamp);
                }
            }
        }

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
    }}
