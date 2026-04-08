enum DroneEvent {
    DRONE_BACK_ONLINE, //This is purely so if faulted it will go back to the task it was doing
    REQUEST_STATUS, // This event doesn't create a transition just the scheduler asking for the drone status again
    FIRE_ASSIGNED, //request going to fire
    ARRIVAL, // I've arrived at destination
    EXTINGUISH_REQUEST,// I've been allowed to extinguish
    FIRE_EXTINGUISHED, // finished extinguishing
    RETURN_BASE_REQUEST, // I'm going home or I'm doing a task
    REPAIRED, // Drone is fixed
    FAILURE // Drone is broken
}

enum DroneState {
    IDLE, //waiting for a task at base
    EN_ROUTE_FIRE, //flying to fire
    ARRIVED_AT_FIRE, //arrival at fire
    FIRE_HANDLED, //fire has been handled
    EN_ROUTE_BASE, //back to base emptystate
    DROPPING_AGENT, //release the substance
    FAULTED
}

public class DroneStateMachine {

    public DroneState state;

    public DroneStateMachine() {//Default Constructor
        this.state = DroneState.IDLE;
    }

    private void transitionTo(DroneState next, DroneEvent cause) {
        System.out.println("[DSM] " + state + " --(" + cause + ")--> " + next);
        state = next;
    }

    public synchronized void handleEvent(DroneEvent ev, String payload, DroneSubsystem drone) {
        System.out.println("Handling Event");
        switch (state) {
            case IDLE:
                if (ev == DroneEvent.FIRE_ASSIGNED) {
                    transitionTo(DroneState.EN_ROUTE_FIRE, ev);
                }
                break;

            case EN_ROUTE_FIRE:
                if (ev == DroneEvent.FAILURE) {
                    transitionTo(DroneState.FAULTED, ev);
                    drone.handleFault();
                } else if (ev == DroneEvent.ARRIVAL) {
                    transitionTo(DroneState.ARRIVED_AT_FIRE, ev);
                }
                break;

            case ARRIVED_AT_FIRE:
                if (ev == DroneEvent.EXTINGUISH_REQUEST) {
                    if(drone.openNozzle(payload)) { //checks to see if it opens
                        transitionTo(DroneState.DROPPING_AGENT, ev);
                    }else{ //doesn't open fault it
                        transitionTo(DroneState.FAULTED,ev );
                    }
                }
                break;

            case DROPPING_AGENT:
                if (ev == DroneEvent.FAILURE) {
                    transitionTo(DroneState.FAULTED, ev);
                    drone.closeNozzle(payload);
                    drone.handleFault();
                } else if (ev == DroneEvent.FIRE_EXTINGUISHED) {
                    transitionTo(DroneState.FIRE_HANDLED, ev);
                    drone.closeNozzle(payload);
                }
                break;

            case FIRE_HANDLED:
                if (ev == DroneEvent.RETURN_BASE_REQUEST) {
                    transitionTo(DroneState.EN_ROUTE_BASE, ev);
                    // drone.returnToBase(payload);
                } else if (ev == DroneEvent.FIRE_ASSIGNED) {
                        System.out.println("Have we made it into here?");
                        transitionTo(DroneState.EN_ROUTE_FIRE, ev);

                }
                break;

            case EN_ROUTE_BASE:
                if (ev == DroneEvent.FAILURE) {
                    transitionTo(DroneState.FAULTED, ev);
                    drone.handleFault();
                } else if (ev == DroneEvent.ARRIVAL) {
                    transitionTo(DroneState.IDLE, ev);
                    drone.restore();
                } else if (ev == DroneEvent.FIRE_ASSIGNED) {
                    if (drone.hasBattery() && drone.hasAgent()) {
                        transitionTo(DroneState.EN_ROUTE_FIRE, ev);
                        //  drone.flyToFire(payload);
                    }
                }
                break;

            case FAULTED:
                if (ev == DroneEvent.REPAIRED) {
                    transitionTo(DroneState.IDLE, ev);
                    drone.restore();
                }
                break;

            default:
                System.out.println("[DSM][Error]: Drone is in an unknown state: " + state);
        }
    }

    // --- Getters and Setters ---
    public DroneState getCurrentState() {
        return state;
    }

    public void setCurrentState(DroneState state) {
        this.state = state;
    }
}
