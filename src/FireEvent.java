/**
 * Iteration #1: Fire Incident System
 *
 * Uses Scheduler as a table
 * FireIncident signals a fire to the table
 * Table dispatches to dronesystem
 * dronesystem handles fire and returns resolution to scheduler
 *
 * sleeps are put in to keep readability however will be able to happen asynchronously as
 * nothing is waiting to receive a messaage
 */

class FireEvent {
    private String time;
    private int zoneId;
    private String type;
    private String severity;
    private boolean firehandled = false;

    public FireEvent(String time, int zoneId, String type, String severity) {
        this.time = time;
        this.zoneId = zoneId;
        this.type = type;
        this.severity = severity;
    }

    public void fireHandled(boolean status) { this.firehandled = status; }
    public int getZoneId() { return zoneId; }
    public String getTime(){ return time; }

    @Override
    public String toString() {
        return String.format("[%s] Zone: %d | Type: %s | Severity: %s | Status: %s",
                time, zoneId, type, severity, firehandled ? "EXTINGUISHED" : "PENDING");
    }
}