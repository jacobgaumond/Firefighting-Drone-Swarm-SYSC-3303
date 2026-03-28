
public class DroneRequest extends SimulationEvent {

    private DroneEvent droneEvent;
    private String time;
    private int zoneId;
    private String eventType;
    private String severity;
    private int targetX;
    private int targetY;
    private int amountToDrop;
    private int droneId;

    private String faultType; // "stuck"," jammed"," packet_loss"," corrupted", or empty string

    public DroneRequest(DroneEvent droneEvent, String time, int zoneId, String eventType, String severity, int targetX, int targetY, int amountToDrop, int droneId) {
        this.droneEvent = droneEvent;
        this.time = time;
        this.zoneId = zoneId;
        this.eventType = eventType;
        this.severity = severity;
        this.targetX = targetX;
        this.targetY = targetY;
        this.amountToDrop = amountToDrop;
        this.droneId = droneId;
        this.faultType = "";
    }
    public DroneRequest(DroneEvent droneEvent, String time, int zoneId, String eventType, String severity, int targetX, int targetY, int amountToDrop, int droneId,String faultType) {
        this.droneEvent = droneEvent;
        this.time = time;
        this.zoneId = zoneId;
        this.eventType = eventType;
        this.severity = severity;
        this.targetX = targetX;
        this.targetY = targetY;
        this.amountToDrop = amountToDrop;
        this.droneId = droneId;
        this.faultType = faultType;
    }

    // Deserialize constructor
    public DroneRequest(String serialized) {
        String[] parts = serialized.split(DELIMITER);
        this.droneEvent = DroneEvent.valueOf(parts[0]);
        this.time = parts[1];
        this.zoneId = Integer.parseInt(parts[2]);
        this.eventType = parts[3];
        this.severity = parts[4];
        this.targetX = Integer.parseInt(parts[5]);
        this.targetY = Integer.parseInt(parts[6]);
        this.amountToDrop = Integer.parseInt(parts[7]);
        this.droneId = Integer.parseInt(parts[8]);
        this.faultType = parts.length > 9 ? parts[9] : "";
    }

    @Override
    public String serialize() {
        String fault = (faultType == null || faultType.isEmpty()) ? "NONE":faultType;
        return droneEvent + DELIMITER + time + DELIMITER + zoneId + DELIMITER + eventType + DELIMITER + severity + DELIMITER + targetX + DELIMITER + targetY + DELIMITER + amountToDrop + DELIMITER + droneId+ DELIMITER+ fault;
    }

    @Override
    public String toString() {
        return "DroneSendEvent[Event=" + droneEvent + ", Time=" + time + ", Zone=" + zoneId + ", Type=" + eventType + ", Severity=" + severity + ", TargetX=" + targetX + ", TargetY=" + targetY + ", AmountToDrop=" + amountToDrop + ", DroneId=" + droneId + "]";
    }

    // Getters
    public DroneEvent getDroneEvent() {
        return droneEvent;
    }

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

    public int getAmountToDrop() {
        return amountToDrop;
    }

    public int getDroneId() {
        return droneId;
    }
    public String getFaultType(){return faultType;}
    public void setFaultType(String faultType){this.faultType=faultType;}
}
