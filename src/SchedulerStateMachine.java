enum SchedulerState {
    NO_FIRES,
    DISPATCHING,
    UNATTENDED_FIRES,
    ALL_FIRES_ATTENDED_TO
}

enum SchedulerEvent {
    FIRE_EVENT,
    DRONES_AVAILABLE,
    DRONE_UNAVAILABLE,
    NOT_ENOUGH_DRONES_AVAILABLE,
    POTENTIALLY_AVAILABLE_DRONE_FOUND,
    DRONE_UPDATED,
    ALL_FIRES_EXTINGUISHED
}

public class SchedulerStateMachine {

    private SchedulerState state;

    public SchedulerStateMachine() {
        this.state = SchedulerState.NO_FIRES;
    }

    private void transitionTo(SchedulerState next, SchedulerEvent cause) {
        System.out.println("[SSM] " + state + " --(" + cause + ")--> " + next);
        state = next;
    }

    public synchronized void handleEvent(SchedulerEvent event, Scheduler scheduler) {
        switch (state) {
            case NO_FIRES:
                if (event == SchedulerEvent.FIRE_EVENT) {
                    transitionTo(SchedulerState.DISPATCHING, event);
                    scheduler.tryAssignTask();
                }
                break;

            case DISPATCHING:
                if (event == SchedulerEvent.FIRE_EVENT) {
                    scheduler.tryAssignTask();
                } else if (event == SchedulerEvent.DRONES_AVAILABLE) {
                    transitionTo(SchedulerState.ALL_FIRES_ATTENDED_TO, event);
                } else if (event == SchedulerEvent.NOT_ENOUGH_DRONES_AVAILABLE) {
                    transitionTo(SchedulerState.UNATTENDED_FIRES, event);
                }
                break;

            case UNATTENDED_FIRES:
                if (event == SchedulerEvent.FIRE_EVENT) {
                    transitionTo(SchedulerState.DISPATCHING, event);
                    scheduler.tryAssignTask();
                } else if (event == SchedulerEvent.DRONE_UPDATED) {
                    transitionTo(SchedulerState.DISPATCHING, event);
                    scheduler.tryAssignTask();
                } else if (event == SchedulerEvent.DRONES_AVAILABLE) {
                    transitionTo(SchedulerState.ALL_FIRES_ATTENDED_TO, event);
                }
                break;

            case ALL_FIRES_ATTENDED_TO:
                if (event == SchedulerEvent.FIRE_EVENT) {
                    transitionTo(SchedulerState.DISPATCHING, event);
                    scheduler.tryAssignTask();
                } else if (event == SchedulerEvent.ALL_FIRES_EXTINGUISHED) {
                    transitionTo(SchedulerState.NO_FIRES, event);
                }
                break;

            default:
                System.out.println("[SSM] Unknown state: " + state);
        }
    }

    public SchedulerState getCurrentState() {
        return state;
    }
}
