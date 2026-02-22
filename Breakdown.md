# Responsibility Breakdown
Breakdown of responsibilities of each team member for this iteration

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
