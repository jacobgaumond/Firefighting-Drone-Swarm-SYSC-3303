# Firefighting Drone Swarm

## Overall Project Description
This is an iterative project that aims to create a simulation of a swarm of drones that put out fires on a map.

This iteration focuses on demonstrating safe communication between multiple threads.

The project is multithreaded and uses the Client-Server model, such that a Server thread is interacting with multiple
Client threads.

## File/Class Descriptions

It should be noted that not all of the code in the repository is being used in this iterable. Some functions -- like
`MessageBox.closeBox()` -- were implemented but will not be used. Some classes -- like SocketWrapper -- were implemented
before it was realized that they would only be usable in future iterations.

### Main Code

- `src/Message.java`
  A class used to encode data into an object, along with the source and desired destination of the object. In future
  iterations this class will be able to describe interactions even more precisely.

- `src/MessageBox.java`
  A class used as a shared resource to pass Message objects between threads. The Server and Client classes each have
  a MessageBox used to receive incoming Message objects.

- `src/Scheduler.java`
  This class represents the Server in the Client-Server model. It has an incoming MessageBox, and has access to the
  MessageBox objects of both the FireIncidentSubsystem and DroneSubsystem. Currently, it processes messages sent to
  its incoming MessageBox to decide if it needs to forward the message to one of its clients.

- `src/FireIncidentSubsystem.java`
  This class represents one of the Clients in the Client-Server model. It has an incoming MessageBox, and has access to
  the MessageBox object of the Scheduler; it does **not** have access to any other client's MessageBox. Currently, it
  parses an input file for events to raise, and passes them as Message objects to the DroneSubsystem. Once it has
  finished with the input file, it processes messages sent to its incoming MessageBox. If it receives a message from the
  DroneSubsystem, it sends an acknowledgement back (unless the message was an acknowledgement itself).

- `src/DroneSubsystem.java`
  This class represents one of the Clients in the Client-Server model. It has an incoming MessageBox, and has access to
  the MessageBox object of the Scheduler; it does **not** have access to any other client's MessageBox. Currently, it
  parses an input file for events to raise, and passes them as Message objects to the FireIncidentSubsystem. Once it has
  finished with the input file, it processes messages sent to its incoming MessageBox. If it receives a message from the
  FireIncidentSubsystem, it sends an acknowledgement back (unless the message was an acknowledgement itself).

- `src/Main.java`
  This class is exclusively used to start the program. It creates threads to execute code for the Scheduler,
  FireIncidentSubsystem, and DroneSubsystem classes.

- `src/DroneGUI.java`
  This class is a standalone GUI using the `java.swing` library to be later connected to the DroneSystem classes.
  Currently, it sets up the static pieces of the legend, the zone labels and gridlines. It also creates the zone fires
  in the middle of each zone and has a status change class to demonstrate switching between fire states.

- `src/data/Sample_event_file.csv`
  This csv file is used as the event input file for the FireIncidentSubsystem class.

### Testing Code

Tests verify inter-thread communication, with a focus verifying passing Message objects between threads.

- `test/MessageBoxTest.java`
  Tests and verifies the functionality of the public functions of the MessageBox class. This may not include functions
  which were not actually used in this iterable of the project.

- `test/FireIncidentSubsystemTest.java`, `test/DroneSubsystemTest.java`, and `test/SchedulerTest.java`
  Tests the FireIncidentSubsystem and DroneSubsystem classes, with a focus on testing passing messages through outgoing
  MessageBox objects. Note that `test/SchedulerTest.java` explicitly tests both of its Client MessageBox objects.
  Each threaded component is tested by creating the thread (and providing it with references to MessageBox object(s)).
  Then, the test functions inject messages into a MessageBox object and assert that they are correctly consumed and
  redistributed to the appropriate recipients.

### UML Diagrams

The `png` files under `uml/` contain UML class and sequence diagrams for the system.

## Project Setup Instructions

### Downloading the git repository

For university assessment purposes, this project is specifically concerned with its ability to run on the IntelliJ IDE.

To download this project, clone the repository from GitHub into a new project on the IntelliJ IDE.

For evaluation purposes, iterables will be submitted to the university as compressed ZIP files. To install the project
from a ZIP file, extract the file and open the resulting directory using the IntelliJ IDE.

### Usage

To run the project, run the main function of the Main class in `src/Main.java` (e.g., by right-clicking the file and
selecting "Run Main.main()"). This will start 3 threads (i.e., one for the Scheduler (Server), one for the
FireIncidentSubsystem (Client), and one for the DroneSubsystem (Client).

Once running, the project will not stop until it is manually stopped by the user (i.e., by pressing stop in IntelliJ).

To run the GUI aspect of the project, run the main function of the DroneGUI class in `src/DroneGUI.java`. This will
create a resizable interface. Once running, close the GUI window to end that program.

### Testing

To test the project, run all of the tests in the `test/` directory (e.g., by right-clicking the directory and selecting
"Run 'All Tests'").
