
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;

public class TestDroneFSM {

    private DroneStateMachine fsm;
    private DroneSubsystem dsub;

    @BeforeEach
    public void setup() {
        fsm = new DroneStateMachine();
        dsub = new DroneSubsystem();
    }

    @AfterEach
    void breakdown() {
        dsub.closeBox();
    }

    @Test
    void testStartIdle() {
        assertEquals(DroneState.IDLE, fsm.getCurrentState());
    }

    @Test
    void testFireAssignmentFlow() {
        assertEquals(DroneState.IDLE, fsm.getCurrentState());

        fsm.handleEvent(DroneEvent.FIRE_ASSIGNED, "", dsub);
        assertEquals(DroneState.EN_ROUTE_FIRE, fsm.getCurrentState());

        fsm.handleEvent(DroneEvent.ARRIVAL, "", dsub);
        assertEquals(DroneState.ARRIVED_AT_FIRE, fsm.getCurrentState());

        fsm.handleEvent(DroneEvent.EXTINGUISH_REQUEST, "", dsub);
        assertEquals(DroneState.DROPPING_AGENT, fsm.getCurrentState());

        fsm.handleEvent(DroneEvent.FIRE_EXTINGUISHED, "", dsub);
        assertEquals(DroneState.FIRE_HANDLED, fsm.getCurrentState());

        fsm.handleEvent(DroneEvent.RETURN_BASE_REQUEST, "", dsub);
        assertEquals(DroneState.EN_ROUTE_BASE, fsm.getCurrentState());

        fsm.handleEvent(DroneEvent.ARRIVAL, "", dsub);
        assertEquals(DroneState.IDLE, fsm.getCurrentState());
    }

    @Test
    void testInvalidEventDoesNothing() {
        fsm.handleEvent(DroneEvent.FIRE_EXTINGUISHED, "", dsub);
        assertEquals(DroneState.IDLE, fsm.getCurrentState());
    }

    @Test
    void testRedirectFromEnRouteBase() {
        fsm.handleEvent(DroneEvent.FIRE_ASSIGNED, "", dsub);
        fsm.handleEvent(DroneEvent.ARRIVAL, "", dsub);
        fsm.handleEvent(DroneEvent.EXTINGUISH_REQUEST, "", dsub);
        fsm.handleEvent(DroneEvent.FIRE_EXTINGUISHED, "", dsub);
        fsm.handleEvent(DroneEvent.RETURN_BASE_REQUEST, "", dsub);
        assertEquals(DroneState.EN_ROUTE_BASE, fsm.getCurrentState());

        // invalid event shouldn't change state
        fsm.handleEvent(DroneEvent.FIRE_EXTINGUISHED, "", dsub);
        assertEquals(DroneState.EN_ROUTE_BASE, fsm.getCurrentState());
    }
}
