import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;

public class TestDroneFSM {
    private DroneStateMachine fsm;

    @BeforeEach
    public void setup() {
        fsm = new DroneStateMachine();
    }


    @Test
    void testFireAssignmentFlow(){
        //initial is IDLE
        assertEquals(DroneState.IDLE, fsm.getCurrentState());

        //assign fire
        fsm.handleEvent(DroneEvent.FIRE_ASSIGNED, null);
        assertEquals(DroneState.EN_ROUTE_FIRE, fsm.getCurrentState());

        //reach fire
        fsm.handleEvent(DroneEvent.FIRE_REACHED, null);
        assertEquals(DroneState.DROPPING_AGENT, fsm.getCurrentState());

        //tank empty
        fsm.handleEvent(DroneEvent.TANK_EMPTY, null);
        assertEquals(DroneState.EN_ROUTE_BASE, fsm.getCurrentState());

        //reach base
        fsm.handleEvent(DroneEvent.BASE_REACHED, null);
        assertEquals(DroneState.IDLE, fsm.getCurrentState());
    }

    @Test
    void testFailureFromEnRouteFire(){
        fsm.handleEvent(DroneEvent.FIRE_ASSIGNED, null);
        fsm.handleEvent(DroneEvent.FAILURE, null);

        assertEquals(DroneState.FAULTED, fsm.getCurrentState());
    }

    @Test
    void testRepairFromFaulted(){
        fsm.handleEvent(DroneEvent.FIRE_ASSIGNED, null);
        fsm.handleEvent(DroneEvent.FAILURE, null);

        fsm.handleEvent(DroneEvent.REPAIRED, null);
        assertEquals(DroneState.IDLE, fsm.getCurrentState());
    }

    @Test
    void testInvalidEventDoesNothing(){
        fsm.handleEvent(DroneEvent.TANK_EMPTY, null);
        assertEquals(DroneState.IDLE, fsm.getCurrentState());
    }

    @Test
    void testRedirectFromEnRouteBase(){
        fsm.handleEvent(DroneEvent.FIRE_ASSIGNED, null);
        fsm.handleEvent(DroneEvent.FIRE_REACHED, null);
        fsm.handleEvent(DroneEvent.TANK_EMPTY, null);

        assertEquals(DroneState.EN_ROUTE_BASE, fsm.getCurrentState());
        fsm.handleEvent(DroneEvent.FIRE_REACHED, null);
        assertEquals(DroneState.EN_ROUTE_BASE, fsm.getCurrentState());
    }






}
