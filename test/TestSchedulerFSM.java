
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;

public class TestSchedulerFSM {

    private SchedulerStateMachine fsm;
    private Scheduler scheduler;

    @BeforeEach
    public void setup() {
        fsm = new SchedulerStateMachine();
        scheduler = new Scheduler();
    }

    @AfterEach
    public void teardown() {
        scheduler.closeBox();
    }

    @Test
    void testStartNoFires() {
        assertEquals(SchedulerState.NO_FIRES, fsm.getCurrentState());
    }

    @Test
    void testFireAssignedFlow() {
        assertEquals(SchedulerState.NO_FIRES, fsm.getCurrentState());

        fsm.handleEvent(SchedulerEvent.FIRE_EVENT, scheduler);
        assertEquals(SchedulerState.DISPATCHING, fsm.getCurrentState());

        fsm.handleEvent(SchedulerEvent.DRONES_AVAILABLE, scheduler);
        assertEquals(SchedulerState.ALL_FIRES_ATTENDED_TO, fsm.getCurrentState());

        fsm.handleEvent(SchedulerEvent.ALL_FIRES_EXTINGUISHED, scheduler);
        assertEquals(SchedulerState.NO_FIRES, fsm.getCurrentState());
    }

    @Test
    void testNotEnoughDronesFlow() {
        assertEquals(SchedulerState.NO_FIRES, fsm.getCurrentState());

        fsm.handleEvent(SchedulerEvent.FIRE_EVENT, scheduler);
        assertEquals(SchedulerState.DISPATCHING, fsm.getCurrentState());

        fsm.handleEvent(SchedulerEvent.NOT_ENOUGH_DRONES_AVAILABLE, scheduler);
        assertEquals(SchedulerState.UNATTENDED_FIRES, fsm.getCurrentState());

        fsm.handleEvent(SchedulerEvent.DRONE_UPDATED, scheduler);
        assertEquals(SchedulerState.DISPATCHING, fsm.getCurrentState());

        fsm.handleEvent(SchedulerEvent.DRONES_AVAILABLE, scheduler);
        assertEquals(SchedulerState.ALL_FIRES_ATTENDED_TO, fsm.getCurrentState());

        fsm.handleEvent(SchedulerEvent.ALL_FIRES_EXTINGUISHED, scheduler);
        assertEquals(SchedulerState.NO_FIRES, fsm.getCurrentState());
    }

    @Test
    void testInvalidEventDoesNothing() {
        fsm.handleEvent(SchedulerEvent.ALL_FIRES_EXTINGUISHED, scheduler);
        assertEquals(SchedulerState.NO_FIRES, fsm.getCurrentState());
    }

    @Test
    void testNewFireWhileDispatching() {
        fsm.handleEvent(SchedulerEvent.FIRE_EVENT, scheduler);
        assertEquals(SchedulerState.DISPATCHING, fsm.getCurrentState());

        // another fire while dispatching should stay in DISPATCHING
        fsm.handleEvent(SchedulerEvent.FIRE_EVENT, scheduler);
        assertEquals(SchedulerState.DISPATCHING, fsm.getCurrentState());
    }

    @Test
    void testNewFireWhileAllFiresAttendedTo() {
        fsm.handleEvent(SchedulerEvent.FIRE_EVENT, scheduler);
        fsm.handleEvent(SchedulerEvent.DRONES_AVAILABLE, scheduler);
        assertEquals(SchedulerState.ALL_FIRES_ATTENDED_TO, fsm.getCurrentState());

        fsm.handleEvent(SchedulerEvent.FIRE_EVENT, scheduler);
        assertEquals(SchedulerState.DISPATCHING, fsm.getCurrentState());
    }

    @Test
    void testInvalidEventInUnattendedFires() {
        fsm.handleEvent(SchedulerEvent.FIRE_EVENT, scheduler);
        fsm.handleEvent(SchedulerEvent.NOT_ENOUGH_DRONES_AVAILABLE, scheduler);
        assertEquals(SchedulerState.UNATTENDED_FIRES, fsm.getCurrentState());

        // invalid event shouldn't change state
        fsm.handleEvent(SchedulerEvent.ALL_FIRES_EXTINGUISHED, scheduler);
        assertEquals(SchedulerState.UNATTENDED_FIRES, fsm.getCurrentState());
    }

    @Test
    void testNewFireFromUnattendedFires() {
        fsm.handleEvent(SchedulerEvent.FIRE_EVENT, scheduler);
        fsm.handleEvent(SchedulerEvent.NOT_ENOUGH_DRONES_AVAILABLE, scheduler);
        assertEquals(SchedulerState.UNATTENDED_FIRES, fsm.getCurrentState());

        fsm.handleEvent(SchedulerEvent.FIRE_EVENT, scheduler);
        assertEquals(SchedulerState.DISPATCHING, fsm.getCurrentState());
    }
}
