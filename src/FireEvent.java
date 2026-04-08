public class FireEvent extends SimulationEvent {

    private String time; // 14:03:15
    private int zoneId; // 1
    private String eventType; // fire detected
    private String severity; // high or moderate
    private int targetX; // x coordinate of fire
    private int targetY; // y coordinate of fire
    private String faultType; // "stuck"," jammed"," packet_loss"," corrupted", or empty string

    public FireEvent(String time, int zoneId, String eventType, String severity, int targetX, int targetY) {
        this.time = time;
        this.zoneId = zoneId;
        this.eventType = eventType;
        this.severity = severity;
        this.targetX = targetX;
        this.targetY = targetY;
        this.faultType = "";
    }

    public FireEvent(String time, int zoneId, String eventType, String severity, int targetX, int targetY, String faultType) {
        this.time = time;
        this.zoneId = zoneId;
        this.eventType = eventType;
        this.severity = severity;
        this.targetX = targetX;
        this.targetY = targetY;
        this.faultType = faultType;
    }

    /**
     * Constructor that creates a FireEvent object from the output of the
     * FireEvent class's serialize() method. In other words, it deserializes
     * Strings to create FireEvent objects.
     *
     * @param serializedFireEvent A String containing a serialized FireEvent
     * object.
     */
    public FireEvent(String serializedFireEvent) {
        String[] parts = serializedFireEvent.split(DELIMITER);
        this.time = parts[0];
        this.zoneId = Integer.parseInt(parts[1]);
        this.eventType = parts[2];
        this.severity = parts[3];
        this.targetX = Integer.parseInt(parts[4]);
        this.targetY = Integer.parseInt(parts[5]);
        this.faultType = parts.length > 6 ? parts[6] : "";
    }

    // getters
    public String getTime() {
        return time;
    }

    public int getZoneId() {
        return zoneId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getSeverity() {
        return severity;
    }

    public int getTargetX() {
        return targetX;
    }

    public int getTargetY() {
        return targetY;
    }

    public String getFaultType() {
        return faultType;
    }

    public long getTimeInSeconds() {
        String[] parts = time.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        int seconds = Integer.parseInt(parts[2]);
        return (hours * 3600) + (minutes * 60) + seconds;
    }
    public void setFaultType(String s){
        this.faultType = s;
    }

    public static FireEvent parseFromCsv(String line) {
        String[] parts = line.split(",");
        if (parts.length < 4) {
            System.err.println("Error: FireEvent: Invalid CSV line");
            return null;
        }

        String time = parts[0];
        int zoneId;
        try {
            zoneId = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            System.err.println("Error: FireEvent: Invalid zoneId");
            return null;
        }
        String eventType = parts[2];
        String severity = parts[3];
        String faultType = parts.length > 4 ? parts[4].trim() : "";

        int targetX = ZoneMap.getX(zoneId);
        int targetY = ZoneMap.getY(zoneId);

        return new FireEvent(time, zoneId, eventType, severity, targetX, targetY, faultType);
    }

    @Override
    public String toString() {
        return "FireTask[Time=" + time + ", Zone="
                + zoneId + ", eventType=" + eventType + ", Severity="
                + severity + ", TargetX="
                + targetX + ", TargetY=" + targetY + ", FaultType=" + faultType + "]";
    }

    /**
     * Serializes the FireEvent object into a String.
     *
     * @return String representation of the FireEvent object.
     */
    @Override
    public String serialize() {
        String serializedFireEvent = getTime() + DELIMITER
                + getZoneId() + DELIMITER
                + getEventType() + DELIMITER
                + getSeverity() + DELIMITER
                + getTargetX() + DELIMITER
                + getTargetY() + DELIMITER
                + getFaultType();
        return serializedFireEvent;
    }
}
