public class FireEvent extends SimulationEvent {
    private String time; //14:03:15
    private int zoneId; //1
    private String eventType; //fire detected
    private String severity; //high or moderate
    private int targetX; //x coordinate of fire
    private int targetY; //y coordinate of fire

    public FireEvent(String time, int zoneId, String eventType, String severity, int targetX, int targetY){
        this.time = time;
        this.zoneId = zoneId;
        this.eventType = eventType;
        this.severity = severity;
        this.targetX = targetX;
        this.targetY = targetY;
    }

    // TODO: Implement the String constructor that deserializes the output of FireEvent.serialize()
//    /**
//     * Constructor that creates a FireEvent object from the output of the FireEvent class's serialize() method. In
//     * other words, it deserializes Strings to create FireEvent objects.
//     *
//     * @param serializedFireEvent A String containing a serialized FireEvent object.
//     */
    public FireEvent(String serializedFireEvent){
        String[] parts = serializedFireEvent.split(DELIMITER);
        this.time = parts[0];
        this.zoneId = Integer.parseInt(parts[1]);
        this.eventType = parts[2];
        this.severity = parts[3];
        this.targetX = Integer.parseInt(parts[4]);
        this.targetY = Integer.parseInt(parts[5]);
    }


    //getters
    public String getTime(){ return time; }
    public int getZoneId(){
        return zoneId;
    }
    public String getEventType(){
        return eventType;
    }
    public String getSeverity(){ return severity; }
    public int  getTargetX(){
        return targetX;
    }
    public int getTargetY(){
        return targetY;
    }


    @Override
    public String toString(){
        return "FireTask[Time=" + time + ", Zone=" +
                zoneId + ", eventType=" + eventType + ", Severity=" +
                severity + ", TargetX=" +
                targetX + ", TargetY=" + targetY + "]";
    }

    /**
     * Serializes the FireEvent object into a String.
     *
     * @return String representation of the FireEvent object.
     */
    @Override
    public String serialize() {
        String serializedFireEvent =    getTime() + DELIMITER +
                                        getZoneId() + DELIMITER +
                                        getEventType() + DELIMITER +
                                        getSeverity() + DELIMITER +
                                        getTargetX() + DELIMITER +
                                        getTargetY();
        return serializedFireEvent;
    }
}
