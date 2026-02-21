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
        fsm.handleEvent(DroneEvent.FIRE_ASSIGNED, null,null);
        assertEquals(DroneState.EN_ROUTE_FIRE, fsm.getCurrentState());

        //reach fire
        fsm.handleEvent(DroneEvent.ARRIVAL, null,null);
        assertEquals(DroneState.DROPPING_AGENT, fsm.getCurrentState());

        //tank empty
        fsm.handleEvent(DroneEvent.FIRE_EXTINGUISHED, null,null);
        assertEquals(DroneState.EN_ROUTE_BASE, fsm.getCurrentState());

        //reach base
        fsm.handleEvent(DroneEvent.ARRIVAL, null,null);
        assertEquals(DroneState.IDLE, fsm.getCurrentState());
    }

    @Test
    void testFailureFromEnRouteFire(){
        fsm.handleEvent(DroneEvent.FIRE_ASSIGNED, null,null);
        fsm.handleEvent(DroneEvent.FAILURE, null,null);

        assertEquals(DroneState.FAULTED, fsm.getCurrentState());
    }

    @Test
    void testRepairFromFaulted(){
        fsm.handleEvent(DroneEvent.FIRE_ASSIGNED, null,null);
        fsm.handleEvent(DroneEvent.FAILURE, null,null);

        fsm.handleEvent(DroneEvent.REPAIRED, null,null);
        assertEquals(DroneState.IDLE, fsm.getCurrentState());
    }

    @Test
    void testInvalidEventDoesNothing(){
        fsm.handleEvent(DroneEvent.FIRE_EXTINGUISHED, null,null);
        assertEquals(DroneState.IDLE, fsm.getCurrentState());
    }

    @Test
    void testRedirectFromEnRouteBase(){
        fsm.handleEvent(DroneEvent.FIRE_ASSIGNED, null,null);
        fsm.handleEvent(DroneEvent.ARRIVAL, null,null);
        fsm.handleEvent(DroneEvent.FIRE_EXTINGUISHED, null,null);

        assertEquals(DroneState.EN_ROUTE_BASE, fsm.getCurrentState());
        fsm.handleEvent(DroneEvent.ARRIVAL, null,null);
        assertEquals(DroneState.EN_ROUTE_BASE, fsm.getCurrentState());
    }






}
