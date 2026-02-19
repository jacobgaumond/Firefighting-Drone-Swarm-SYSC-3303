
enum DroneEvent{
    MISSION_ASSIGNED,
    FIRE_REACHED,
    BASE_REACHED,
    TANK_EMPTY,
    FIRE_EXTINGUISHED,
    REFILL_COMPLETE,//? do we need this as refill's are instant
    REPAIRED,

    FAILURE
}

enum DroneState{
    IDLE, //waiting for a task
    EN_ROUTE_FIRE, //flyting to fire

    EN_ROUTE,
    EN_ROUTE_BASE,

    DROPPING_AGENT, //release the substance
    REFILLING,//? do we need this as refill's are instant?
    FAULTED
}
public class Drone {
    private static int nextIdValue = 1;//self ID creation

    private int drone_ID;
    private int x_coord;
    private int y_coord;
    private int fluidAmount;


    public DroneState state;

    public Drone() {//Null Constructor
        this.drone_ID = nextIdValue++; // Assign current value, then increment
        this.x_coord = 0;
        this.y_coord = 0;
        this.fluidAmount = 15;
        this.state = DroneState.IDLE;
    }

    //
    public Drone(int x_coord, int y_coord, int fluidAmount) {
        this.drone_ID = nextIdValue++;
        this.x_coord = x_coord;
        this.y_coord = y_coord;
        this.fluidAmount = fluidAmount;
        this.state = DroneState.IDLE;
    }

    private void transitionTo(DroneState next, DroneEvent cause) {
        System.out.println("[FSM] " + state + " --(" + cause + ")--> " + next);
        state = next;
    }
    public synchronized void handleEvent(DroneEvent ev, String payload ){

        switch(state){
            case IDLE:
                if(ev == DroneEvent.MISSION_ASSIGNED)
                    transitionTo(DroneState.EN_ROUTE_FIRE, ev);
            break;

            case EN_ROUTE_FIRE:
                if( ev == DroneEvent.FAILURE)  transitionTo(DroneState.FAULTED, ev);

                else if( ev == DroneEvent.FIRE_REACHED) transitionTo(DroneState.DROPPING_AGENT,ev);

            break;

            case EN_ROUTE_BASE:
                if( ev == DroneEvent.FAILURE)  transitionTo(DroneState.FAULTED, ev);

                else  if(ev == DroneEvent.MISSION_ASSIGNED && fluidAmount!=0) transitionTo (DroneState.EN_ROUTE_FIRE, ev);

                else if (ev == DroneEvent.BASE_REACHED) {
                    transitionTo(DroneState.IDLE, ev);
                    this.fluidAmount = 15; //refills the drone
                }

            break;

            case DROPPING_AGENT:
                if( ev == DroneEvent.FAILURE)  transitionTo(DroneState.FAULTED, ev);

                else if (ev == DroneEvent.TANK_EMPTY && fluidAmount == 0) transitionTo(DroneState.EN_ROUTE_BASE, ev);

                else if (ev == DroneEvent.FIRE_EXTINGUISHED) transitionTo(DroneState.IDLE, ev); //this logic might be changed to route back to base
            break;

            case FAULTED:

                if (ev == DroneEvent.REPAIRED) transitionTo (DroneState.IDLE, ev);

            break;

            default:
                System.out.println("Error Drone is is an unknown state:"+state);

        }
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

    public DroneState  getCurrentState() {
        return state;
    }
    public void setCurrentState(DroneState state) { this.state = state; }
}