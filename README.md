# Festival Simulator

This project starts from the course simulation framework. I have adapted it
into a foundation for our festival simulator, adding support for our ideas
about service points (entrance, bar, toilet, and stage), visitor routing, and
scheduled stage attendance. It is a framework for the team to build on, not a
finished simulator. This is a placeholder document so everyone can navigate the changes easier.

## How a run works

1. In the JavaFX screen, enter the run duration in minutes and the expected
   number of visitors. The default duration is 1,440 minutes (one day). Enter
   a delay in milliseconds to control animation speed; the speed buttons adjust
   it while the run is active.
   See
   [SimulatorGUI](src/main/java/view/SimulatorGUI.java),
   [ISimulatorUI](src/main/java/view/ISimulatorUI.java), and
   [Controller](src/main/java/controller/Controller.java).

2. The [Controller](src/main/java/controller/Controller.java) validates the
   inputs and creates the model. The expected count and duration determine the
   mean arrival interval: `duration / expected visitors`. Arrivals are
   generated randomly over the run, so the actual count can differ. A count of
   zero means no visitors arrive.
   See
   [MyEngine](src/main/java/simu/model/MyEngine.java) and
   [ArrivalProcess](src/main/java/simu/framework/ArrivalProcess.java).

3. Each arriving visitor joins the entrance queue. After the entrance, visitors
   choose the bar, toilet, stage, or exit using configurable routing weights.
   They can repeat activities up to the configured limit.
   See
   [Customer](src/main/java/simu/model/Customer.java),
   [VisitorState](src/main/java/simu/model/VisitorState.java), and
   [RouteOrigin](src/main/java/simu/model/RouteOrigin.java).

4. Performances start and end on a repeating schedule. Visitors can attend only
   during a performance and only while capacity is available. At show end,
   attendees leave and choose their next destination. If the stage is inactive
   or full, selected visitors are rerouted.
   See
   [Stage](src/main/java/simu/model/Stage.java) and
   [EventType](src/main/java/simu/model/EventType.java).

## Simulation engine

The engine follows the course's three phases:

- **A:** advance the simulation clock to the next event.
- **B:** process scheduled events, such as arrivals and service completions.
- **C:** start services or admit stage visitors when their conditions are met;
  repeat C while an eligible action occurs.

The engine resets the event list and clock for each run. The festival model also
resets stage attendance, pending admissions, visitor IDs, and visitor averages.
See
[Engine](src/main/java/simu/framework/Engine.java),
[EventList](src/main/java/simu/framework/EventList.java), and
[Clock](src/main/java/simu/framework/Clock.java),
[MyEngine](src/main/java/simu/model/MyEngine.java), and
[Customer](src/main/java/simu/model/Customer.java).

## Settings and tests

Edit [festival.properties](config/festival.properties) to change service-time
distributions, routing weights, visitor activity limit, and stage capacity and
schedule. For example, the current stage capacity is 100, with a 45-minute show
starting at minute 15 and a 15-minute break. Routing weights are relative
values, not percentages. The stage is a separate capacity-limited model;
entrance, bar, and toilet each currently have one queue and one service server
([ServicePoint](src/main/java/simu/model/ServicePoint.java)).

[EngineTest](src/test/java/simu/framework/EngineTest.java) checks a deterministic
arrival-to-exit scenario twice, checks reset behavior and that an arrival after
closing is ignored, and checks that an empty event list ends cleanly. Run it
from VS Code's Testing view using the Java Test Runner (JUnit 5). The JUnit
dependency is declared in [pom.xml](pom.xml); using Maven from the command line
is optional.

The visualization currently counts arrivals; it does not animate movement
between stations. The console logs arrivals, stage entry/exit, and completed
visitor times and average stay ([Customer](src/main/java/simu/model/Customer.java),
[MyEngine](src/main/java/simu/model/MyEngine.java)). Service-point performance
statistics and final aggregate results are not implemented yet.
