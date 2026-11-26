package simu.framework;

import controller.IControllerMtoV;
import eduni.distributions.ContinuousGenerator;
import org.junit.jupiter.api.Test;
import simu.model.Customer;
import simu.model.EventType;
import simu.model.ServicePoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EngineTest {

    @Test
    void twoRunsStartCleanAndProcessArrivalToExit() {
        RunSummary first = runScenario();
        RunSummary second = runScenario();

        assertEquals(new RunSummary(1, 1, 1, 10.0), first);
        assertEquals(first, second);
    }

    private RunSummary runScenario() {
        Trace.setTraceLevel(Trace.Level.ERR);
        TestEngine engine = new TestEngine();
        engine.setSimulationTime(10.0);
        engine.setDelay(0);

        // Calling run() directly runs synchronously; it does not start a thread or JavaFX.
        engine.run();

        return new RunSummary(
                engine.arrivals,
                engine.exits,
                engine.firstVisitorId,
                Clock.getInstance().getTime()
        );
    }

    private record RunSummary(
            int arrivals,
            int exits,
            int firstVisitorId,
            double endTime
    ) {}

    private static class TestEngine extends Engine {
        private final ArrivalProcess arrivalsProcess;
        private int arrivals;
        private int exits;
        private int firstVisitorId;

        TestEngine() {
            super(new NoOpController());

            servicePoints = new ServicePoint[] {
                    new ServicePoint(new SequenceGenerator(1.0), eventList, EventType.ENTRANCE_COMPLETE),
                    new ServicePoint(new SequenceGenerator(1.0), eventList, EventType.BAR_COMPLETE)
            };

            // First arrival is at time 1; the next is at time 101, after closing.
            arrivalsProcess = new ArrivalProcess(
                    new SequenceGenerator(1.0, 100.0),
                    eventList,
                    EventType.VISITOR_ARRIVAL
            );
        }

        @Override
        protected void initialization() {
            Customer.resetForNewRun();
            arrivalsProcess.generateNext();
        }

        @Override
        protected void runEvent(Event event) {
            switch ((EventType) event.getType()) {
                case VISITOR_ARRIVAL -> {
                    Customer visitor = new Customer();
                    if (arrivals == 0) {
                        firstVisitorId = visitor.getId();
                    }
                    arrivals++;
                    servicePoints[0].addQueue(visitor);
                    arrivalsProcess.generateNext();
                }
                case ENTRANCE_COMPLETE -> {
                    Customer visitor = servicePoints[0].removeQueue();
                    servicePoints[1].addQueue(visitor);
                }
                case BAR_COMPLETE -> {
                    servicePoints[1].removeQueue();
                    exits++;
                }
                default -> throw new IllegalStateException(
                        "Unexpected event in test scenario: " + event.getType()
                );
            }
        }

        @Override
        protected void results() {
            // The test reads the summary from this engine; no UI update is needed.
        }
    }

    private static class NoOpController implements IControllerMtoV {
        @Override
        public void showEndTime(double time) {}

        @Override
        public void visualiseCustomer() {}
    }

    private static class SequenceGenerator implements ContinuousGenerator {
        private final double[] values;
        private int index;
        private long seed;

        SequenceGenerator(double... values) {
            this.values = values;
        }

        @Override
        public double sample() {
            int current = Math.min(index, values.length - 1);
            index++;
            return values[current];
        }

        @Override
        public void setSeed(long seed) {
            this.seed = seed;
        }

        @Override
        public long getSeed() {
            return seed;
        }

        @Override
        public void reseed() {
            index = 0;
        }
    }
    @Test
    void emptyEventListFinishesAtConfiguredEndTime() {
        EmptyEventEngine engine = new EmptyEventEngine();
        engine.setSimulationTime(10.0);
        engine.setDelay(0);

        engine.run();

        assertEquals(10.0, Clock.getInstance().getTime(), 0.0001);
        assertTrue(engine.resultsCalled);
    }

    private static class EmptyEventEngine extends Engine {
        private boolean resultsCalled;

        EmptyEventEngine() {
            super(new NoOpController());
            servicePoints = new ServicePoint[0];
        }

        @Override
        protected void initialization() {
            // Intentionally schedule no events.
        }

        @Override
        protected void runEvent(Event event) {
            throw new AssertionError("No event should be processed");
        }

        @Override
        protected void results() {
            resultsCalled = true;
        }
    }
}