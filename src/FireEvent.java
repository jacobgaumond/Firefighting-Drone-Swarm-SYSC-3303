public class FireEvent {
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





}




