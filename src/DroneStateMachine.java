
enum DroneEvent{
    FIRE_ASSIGNED, //request going to fire
    ARRIVAL, // I've arrived at destination
    EXTINGUISH_REQUEST,// I've been allowed to extinguish
    FIRE_EXTINGUISHED, // finished extinguishing
    RETURN_BASE_REQUEST, // I'm going home or I'm doing a task
    REPAIRED, // Drone is fixed
    FAILURE // Drone is broken
}

enum DroneState{
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
    public DroneStateMachine() {//Null Constructor

        this.state = DroneState.IDLE;
    }
    private void transitionTo(DroneState next, DroneEvent cause) {
        System.out.println("[DSM] " + state + " --(" + cause + ")--> " + next);
        state = next;
    }
    public synchronized void handleEvent(DroneEvent ev, String payload, DroneSubsystem drone) {
        switch (state) {
            case IDLE:
                if (ev == DroneEvent.FIRE_ASSIGNED) {
                    transitionTo(DroneState.EN_ROUTE_FIRE, ev);
                    drone.flyToFire(payload);
                }
                break;

            case EN_ROUTE_FIRE:
                if (ev == DroneEvent.FAILURE) {
                    transitionTo(DroneState.FAULTED, ev);
                    drone.handleFault();
                }
                else if (ev == DroneEvent.ARRIVAL) {
                    transitionTo(DroneState.ARRIVED_AT_FIRE, ev);
                }
                break;

            case ARRIVED_AT_FIRE:
                if (ev == DroneEvent.EXTINGUISH_REQUEST) {
                    transitionTo(DroneState.DROPPING_AGENT, ev);
                    drone.openNozzle();
                }
                break;

            case DROPPING_AGENT:
                if (ev == DroneEvent.FAILURE) {
                    drone.closeNozzle(payload);
                    transitionTo(DroneState.FAULTED, ev);
                    drone.handleFault();
                }
                else if (ev == DroneEvent.FIRE_EXTINGUISHED) {
                    drone.closeNozzle(payload);
                    transitionTo(DroneState.FIRE_HANDLED, ev);
                }
                break;

            case FIRE_HANDLED:
                if (ev == DroneEvent.RETURN_BASE_REQUEST) {
                    transitionTo(DroneState.EN_ROUTE_BASE, ev);
                    drone.returnToBase(payload);
                }
                else if (ev == DroneEvent.FIRE_ASSIGNED) {
                    if (drone.hasBattery() && drone.hasAgent()) {
                        transitionTo(DroneState.EN_ROUTE_FIRE, ev);
                        drone.flyToFire(payload);
                    }
                }
                break;

            case EN_ROUTE_BASE:
                if (ev == DroneEvent.FAILURE) {
                    transitionTo(DroneState.FAULTED, ev);
                    drone.handleFault();
                }
                else if (ev == DroneEvent.ARRIVAL) {
                    transitionTo(DroneState.IDLE, ev);
                    drone.restore();
                }
                else if (ev == DroneEvent.FIRE_ASSIGNED) {
                    if (drone.hasBattery() && drone.hasAgent()) {
                        transitionTo(DroneState.EN_ROUTE_FIRE, ev);
                        drone.flyToFire(payload);
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
                System.out.println("Error: Drone is in an unknown state: " + state);
        }

        drone.sendStatus(); //notifies the scheduler after each state change
    }

    // --- Getters and Setters ---
    public DroneState  getCurrentState() {
        return state;
    }
    public void setCurrentState(DroneState state) { this.state = state; }
}