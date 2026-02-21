public class DroneResponse extends SimulationEvent {
    private int droneId;
    private String state;
    private int x, y;
    private int fluidAmount;
    private int battery;
    private int fluidDropped;

    public DroneResponse(int droneId, String state, int x, int y, int fluidAmount, int battery, int fluidDropped) {
        this.droneId = droneId;
        this.state = state;
        this.x = x;
        this.y = y;
        this.fluidAmount = fluidAmount;
        this.battery = battery;
        this.fluidDropped = fluidDropped;
    }

    // Deserialize constructor
    public DroneResponse(String serialized) {
        String[] parts = serialized.split(DELIMITER);
        this.droneId = Integer.parseInt(parts[0]);
        this.state = parts[1];
        this.x = Integer.parseInt(parts[2]);
        this.y = Integer.parseInt(parts[3]);
        this.fluidAmount = Integer.parseInt(parts[4]);
        this.battery = Integer.parseInt(parts[5]);
        this.fluidDropped = Integer.parseInt(parts[6]);
    }

    @Override
    public String serialize() {
        return droneId + DELIMITER + state + DELIMITER + x + DELIMITER + y + DELIMITER
                + fluidAmount + DELIMITER + battery + DELIMITER + fluidDropped;
    }

    public String toString() {
        return "DroneResponse[DroneId=" + droneId + ", State=" + state +
                ", Pos=(" + x + "," + y + "), Fluid=" + fluidAmount +
                ", Battery=" + battery + ", FluidDropped=" + fluidDropped + "]";
    }

    // Getters
    public int getDroneID() { return droneId; }
    public String getState() { return state; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getFluidAmount() { return fluidAmount; }
    public int getBattery() { return battery; }
    public int getFluidDropped() { return fluidDropped; }
}