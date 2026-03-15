# Responsibility Breakdown
Breakdown of responsibilities of each team member for each iteration

## Iteration #1

- Olivia: GUI, Code
- Jacob: Code, Refactoring, README
- Ulan: UML, README
- Peter: Code, README

## Iteration #2

- Olivia:
  - DroneStateMachine
  - Construction of State Machine
  - DroneSubsystem
    - Drone logic and state tracking
  - Scheduler
    - Drone registry and task assignment
    - Fire tracking and multi-run extinguishing
  - Minor Parsing Classes
    - DroneRequest, DroneResponse, FireEvent

- Jacob:
  - UML
    - Scheduler & Drone Subsystem State Machine Diagrams
      - Designed state machines
    - Updated UML Class Diagram

- Ulan:
  - Fire event message parsing
  - Parts of FireEvent class
  - Updates on communication layer
  - Csv zone loading
  - Map to store zones
  - Few tests
  - Zone class implementation
    
- Peter:
  - GUI
    - Refactor
    - Build UI Zones using ZoneMap (csv data)
    - Update fire status
    - Move drones
    - Threaded/Interruptable animations
  - Scheduler
    - Update fire severity
  - GUI calls throughout app where needed

## Iteration #3
- Olivia:
  -Code
    - Scheduler routing for specific drone port calling
    - GUI logic in the Scheduler subsystem
    - Sequence Diagram
    - State Machine Test Cases
    - Refactoring of Test Cases
- Jacob:
  - Code
    - Created `src/SocketWrapper.java` in a previous iteration (unused until now). Updated/Polished in this iteration for UDP use.
    - Created `src/UDPMessageBox.java`.
    - Converted project to use UDP instead of local message passing (adapted FireIncidentSubsystem/Scheduler/DroneSubsystem to use UDPMessageBox instead of MessageBox).
    - Contributed towards dividing the `src/Main.java` code amongst the (newly made) main methods of the 4 main classes (i.e., FireIncidentSubsystem, Scheduler, DroneSubsystem, DroneGUI).
    - Documented changes in the `README.md`.

- Ulan:

- Peter:
  - GUI
    - Get current drone position
  - Scheduler
    - Drone dispatching priority logic
    - Checks number of times specific drone has been dispatched
    - Between least dispatched, select available. 
    - If none available, compare severities for potentially reroute
  - Main
    - Add multiple drone threads
  - UDPMessageBox
    - fix port assignment
    - fix sending messages over UDP
  - Tests
    - UDPMessageBoxTest
