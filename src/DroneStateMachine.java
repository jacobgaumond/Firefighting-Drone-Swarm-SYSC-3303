
enum DroneEvent{
    FIRE_ASSIGNED, // I'm going to a fire
    FIRE_REACHED, // I've arrived at a fire
    BASE_REACHED, //I've arrived at home base
    TANK_EMPTY, //my tanks is empty
    FIRE_EXTINGUISHED, // the fire is extinguished (I've dropped enough)
    REFILL_COMPLETE,// TODO; do we need this as refill's are instant
    REPAIRED, // Drone is fixed
    FAILURE // Drone is broken
}

enum DroneState{
    IDLE, //waiting for a task at base
    EN_ROUTE_FIRE, //flying to fire
    EN_ROUTE, // TODO; remove based on scheduler logic
    EN_ROUTE_BASE, //back to base emptystate
    DROPPING_AGENT, //release the substance
    REFILLING,// TODO; discuss if this is needed
    FAULTED
}
public class DroneStateMachine {

    public DroneState state;
    public DroneStateMachine() {//Null Constructor

        this.state = DroneState.IDLE;
    }
    public DroneStateMachine(int x_coord, int y_coord, int fluidAmount) {

        this.state = DroneState.IDLE;
    }
    private void transitionTo(DroneState next, DroneEvent cause) {
        System.out.println("[FSM] " + state + " --(" + cause + ")--> " + next);
        state = next;
    }
    public synchronized void handleEvent(DroneEvent ev, String payload ){ //this is all valid

        switch(state){
            case IDLE:
                if(ev == DroneEvent.FIRE_ASSIGNED)
                    transitionTo(DroneState.EN_ROUTE_FIRE, ev);
            break;

            case EN_ROUTE_FIRE:
                if( ev == DroneEvent.FAILURE)  transitionTo(DroneState.FAULTED, ev);

                else if( ev == DroneEvent.FIRE_REACHED) transitionTo(DroneState.DROPPING_AGENT,ev);

            break;

            case EN_ROUTE_BASE:
                if( ev == DroneEvent.FAILURE)  transitionTo(DroneState.FAULTED, ev);

                else  if(ev == DroneEvent.FIRE_ASSIGNED) transitionTo (DroneState.EN_ROUTE_FIRE, ev);

                else if (ev == DroneEvent.BASE_REACHED) transitionTo(DroneState.IDLE, ev);

            break;

            case DROPPING_AGENT:
                if( ev == DroneEvent.FAILURE)  transitionTo(DroneState.FAULTED, ev);

                else if (ev == DroneEvent.TANK_EMPTY) transitionTo(DroneState.EN_ROUTE_BASE, ev);

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
    public DroneState  getCurrentState() {
        return state;
    }
    public void setCurrentState(DroneState state) { this.state = state; }
}