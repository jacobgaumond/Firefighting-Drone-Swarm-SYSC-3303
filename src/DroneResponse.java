public class DroneResponse extends SimulationEvent {

    private int droneId;
    private String state;
    private double x, y;
    private double fluidAmount;
    private int battery;
    private double fluidDropped;

    private String faultType;

    public DroneResponse(int droneId, String state, double x, double y, double fluidAmount, int battery, double fluidDropped) {
        this.droneId = droneId;
        this.state = state;
        this.x = x;
        this.y = y;
        this.fluidAmount = fluidAmount;
        this.battery = battery;
        this.fluidDropped = fluidDropped;
        this.faultType="";
    }
    public DroneResponse(int droneId, String state, double x, double y, double fluidAmount, int battery, double fluidDropped, String faultType) {
        this.droneId = droneId;
        this.state = state;
        this.x = x;
        this.y = y;
        this.fluidAmount = fluidAmount;
        this.battery = battery;
        this.fluidDropped = fluidDropped;
        this.faultType = faultType;
    }

    // Deserialize constructor
    public DroneResponse(String serialized) {
        String[] parts = serialized.split(DELIMITER);
        this.droneId = Integer.parseInt(parts[0]);
        this.state = parts[1];
        this.x = Double.parseDouble(parts[2]);
        this.y = Double.parseDouble(parts[3]);
        this.fluidAmount = Double.parseDouble(parts[4]);
        this.battery = Integer.parseInt(parts[5]);
        this.fluidDropped = Double.parseDouble(parts[6]);
        this.faultType = parts.length > 7 ? parts[7] : "";
    }

    @Override
    public String serialize() {
        return droneId + DELIMITER + state + DELIMITER + x + DELIMITER + y + DELIMITER
                + fluidAmount + DELIMITER + battery + DELIMITER + fluidDropped+ DELIMITER+faultType;
    }

    public String toString() {
        return "DroneResponse[DroneId=" + droneId + ", State=" + state
                + ", Pos=(" + x + "," + y + "), Fluid=" + fluidAmount
                + ", Battery=" + battery + ", FluidDropped=" + fluidDropped + "]";
    }

    // Getters
    public int getDroneID() {
        return droneId;
    }

    public String getState() {
        return state;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getFluidAmount() {
        return fluidAmount;
    }

    public int getBattery() {
        return battery;
    }

    public double getFluidDropped() {
        return fluidDropped;
    }

    public String getFaultType() {
        return faultType;
    }
}
