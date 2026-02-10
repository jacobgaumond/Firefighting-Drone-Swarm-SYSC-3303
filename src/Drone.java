
enum DroneEvent{

}

enum DroneState{

}
public class Drone {
    private static int nextIdValue = 1;//self ID creation

    private int drone_ID;
    private int x_coord;
    private int y_coord;
    private int fluidAmount;



    public Drone() {//Null Constructor should be must initialization placements
        this.drone_ID = nextIdValue++; // Assign current value, then increment
        this.x_coord = 0;
        this.y_coord = 0;
        this.fluidAmount = 15;
    }

    //
    public Drone(int x_coord, int y_coord, int fluidAmount) {
        this.drone_ID = nextIdValue++;
        this.x_coord = x_coord;
        this.y_coord = y_coord;
        this.fluidAmount = fluidAmount;
    }

    public void transitionTo(DroneState next, DroneEvent cause){

    }
    public synchronized void handleEvent(DroneEvent ev, String payload ){

    }


    // --- Getters and Setters ---

    public int getDrone_ID() {return drone_ID;}

    public int getX_coord() { return x_coord; }
    public void setX_coord(int x_coord) { this.x_coord = x_coord; }

    public int getY_coord() { return y_coord; }
    public void setY_coord(int y_coord) { this.y_coord = y_coord; }

    public int getFluidAmount() { return fluidAmount; }
    public void setFluidAmount(int fluidAmount) { this.fluidAmount = fluidAmount; }

    // Static method to check how many drones have been created
    public static int getTotalDronesCreated() {
        return nextIdValue - 1;
    }
}