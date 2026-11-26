package simu.model;

import controller.IControllerMtoV;
import eduni.distributions.Negexp;
import eduni.distributions.Normal;
import simu.framework.ArrivalProcess;
import simu.framework.Clock;
import simu.framework.Engine;
import simu.framework.Event;
import simu.framework.Trace;
import java.util.Map;
import eduni.distributions.Uniform;
import java.util.List;
import java.util.ArrayList;

public class MyEngine extends Engine {

	private ArrivalProcess arrivalProcess;
	private final Uniform uniformGenerator = new Uniform(0.0, 1.0);
	private final int maxActivities;
	private final Map<RouteOrigin, Map<VisitorState, Integer>> routingWeights;
	private final Stage stage;
	private final double firstShowStart;
	private double showDuration;
	private double breakDuration;

	private record StageRequest(Customer visitor, RouteOrigin origin) {}
	private final List<StageRequest> stageRequests = new ArrayList<>();

	public MyEngine(IControllerMtoV controller, FestivalConfig config, double duration, int expectedVisitors){ // NEW
		super(controller); // NEW
		maxActivities = config.getPositiveInt("visitor.maxActivities");
		stage = new Stage(config.getPositiveInt("stage.capacity"));
		firstShowStart = config.getPositiveDouble("stage.firstShowStart");
		showDuration = config.getPositiveDouble("stage.showDuration");
		breakDuration = config.getPositiveDouble("stage.breakDuration");

		routingWeights = Map.of(
			RouteOrigin.ENTRANCE, Map.of(
				VisitorState.BAR, config.getNonNegativeInt("routing.entrance.bar"),
				VisitorState.TOILET, config.getNonNegativeInt("routing.entrance.toilet"),
				VisitorState.EXITED, config.getNonNegativeInt("routing.entrance.exited"),
				VisitorState.STAGE, config.getNonNegativeInt("routing.entrance.stage")
			),
			RouteOrigin.BAR, Map.of(
				VisitorState.BAR, config.getNonNegativeInt("routing.bar.bar"),
				VisitorState.TOILET, config.getNonNegativeInt("routing.bar.toilet"),
				VisitorState.EXITED, config.getNonNegativeInt("routing.bar.exited"),
				VisitorState.STAGE, config.getNonNegativeInt("routing.bar.stage")
			),
			RouteOrigin.TOILET, Map.of(
				VisitorState.BAR, config.getNonNegativeInt("routing.toilet.bar"),
				VisitorState.TOILET, config.getNonNegativeInt("routing.toilet.toilet"),
				VisitorState.EXITED, config.getNonNegativeInt("routing.toilet.exited"),
				VisitorState.STAGE, config.getNonNegativeInt("routing.toilet.stage")
			),
			RouteOrigin.STAGE, Map.of(
				VisitorState.BAR, config.getNonNegativeInt("routing.stage.bar"),
				VisitorState.TOILET, config.getNonNegativeInt("routing.stage.toilet"),
				VisitorState.EXITED, config.getNonNegativeInt("routing.stage.exited"),
				VisitorState.STAGE, config.getNonNegativeInt("routing.stage.stage")
			)
		);
		validateRoutingWeights();

		long arrivalSeed = config.getLong("arrival.seed");

		if (expectedVisitors > 0) {
			double meanInterarrival = duration / expectedVisitors;
			arrivalProcess = new ArrivalProcess(new Negexp(meanInterarrival, arrivalSeed), eventList, EventType.VISITOR_ARRIVAL);
		}
		servicePoints = new ServicePoint[3];
	
		servicePoints[0]=new ServicePoint(new Normal(config.getPositiveDouble("service.entrance.mean"), config.getPositiveDouble("service.entrance.variance")), eventList, EventType.ENTRANCE_COMPLETE);
		servicePoints[1]=new ServicePoint(new Normal(config.getPositiveDouble("service.bar.mean"), config.getPositiveDouble("service.bar.variance")), eventList, EventType.BAR_COMPLETE);
		servicePoints[2]=new ServicePoint(new Normal(config.getPositiveDouble("service.toilet.mean"), config.getPositiveDouble("service.toilet.variance")), eventList, EventType.TOILET_COMPLETE);
	}

	private void scheduleNextArrival() {
		if (arrivalProcess != null) {
			arrivalProcess.generateNext();
		}
	}

	private void routeVisitor(Customer visitor, RouteOrigin origin) {
		routeVisitor(visitor, origin, stage.isPerformanceActive());
	}

	private void routeVisitor(Customer visitor, RouteOrigin origin, boolean allowStage) {
		visitor.setState(VisitorState.DECISION);
		
		if (visitor.getCompletedActivities() >= maxActivities) {
			finishVisit(visitor);
			return;
		}
		VisitorState destination = chooseDestination(origin, allowStage);
		if (destination == VisitorState.STAGE) {
			stageRequests.add(new StageRequest(visitor, origin));
			return;
		}

		visitor.setState(destination);

		switch (destination) {
			case BAR:
				servicePoints[1].addQueue(visitor);
				break;
			case TOILET:
				servicePoints[2].addQueue(visitor);
				break;
			case EXITED:
				finishVisit(visitor);
				break;
			default:
				throw new IllegalArgumentException("Invalid destination: " + destination);
		}
	}
	private void finishVisit(Customer visitor) {
		visitor.setState(VisitorState.EXITED);
		visitor.setRemovalTime(Clock.getInstance().getTime());
		visitor.reportResults();
	}

	@Override
	protected void initialization() {
		Customer.resetForNewRun();
		stage.reset();
		stageRequests.clear();

		eventList.add(new Event(EventType.PERFORMANCE_START, firstShowStart));
		scheduleNextArrival();	 // First arrival in the system
	}

	@Override
	protected void runEvent(Event t) {  // B phase events

		switch ((EventType)t.getType()){
		case VISITOR_ARRIVAL:
			Customer visitor = new Customer(); // Starts in DECISION
			servicePoints[0].addQueue(visitor); // Entrance
			scheduleNextArrival();
			controller.visualiseCustomer(); // NEW
			break;

		case ENTRANCE_COMPLETE: // Entrance service completed
			Customer admitted = servicePoints[0].removeQueue();
			routeVisitor(admitted, RouteOrigin.ENTRANCE);
			break;

		case BAR_COMPLETE:
			Customer fromBar = servicePoints[1].removeQueue();
			fromBar.recordCompletedActivity();
			routeVisitor(fromBar, RouteOrigin.BAR);
			break;

		case TOILET_COMPLETE:
			Customer fromToilet = servicePoints[2].removeQueue();
			fromToilet.recordCompletedActivity();
			routeVisitor(fromToilet, RouteOrigin.TOILET);
			break;
		
		case PERFORMANCE_START:
			stage.startPerformance();
			eventList.add(new Event(EventType.PERFORMANCE_END, Clock.getInstance().getTime() + showDuration));
			break;

		case PERFORMANCE_END:
			List<Customer> departingVisitors = stage.endPerformance();
			for (Customer departingVisitor : departingVisitors) {
				departingVisitor.recordCompletedActivity();

				Trace.out(Trace.Level.INFO, "Customer #" + 
					departingVisitor.getId() + 
					" left the stage at " + Clock.getInstance().getTime() +
					" (attendance: " + departingVisitors.size() + "/" + stage.getCapacity() + ")");
					
				routeVisitor(departingVisitor, RouteOrigin.STAGE);
			}
			eventList.add(new Event(EventType.PERFORMANCE_START, Clock.getInstance().getTime() + breakDuration));
			break;
		}	
	}

	private VisitorState chooseDestination(RouteOrigin origin, boolean allowStage) {
		Map<VisitorState, Integer> weights = routingWeights.get(origin);

		if (weights == null || weights.isEmpty()) {
			throw new IllegalArgumentException("No routing weights defined for origin: " + origin);
		}
		long totalWeight = 0;
		for (Map.Entry<VisitorState, Integer> entry : weights.entrySet()) {
			if (!allowStage && entry.getKey() == VisitorState.STAGE) {
				continue;
			}
			int weight = entry.getValue();
			if (weight < 0) {
				throw new IllegalArgumentException("Negative weight found for origin: " + origin + ", destination: " + entry.getKey());
			}
			totalWeight += weight;
		}
		if (totalWeight <= 0) {
			throw new IllegalArgumentException("Total weight must be positive for origin: " + origin);
		}

		double ticket = uniformGenerator.sample() * totalWeight;
		VisitorState lastPositiveDestination = null;
		
		for (Map.Entry<VisitorState, Integer> entry : weights.entrySet()) {
			if (!allowStage && entry.getKey() == VisitorState.STAGE) {
				continue;
			}
			int weight = entry.getValue();
			if (weight == 0) {
				continue;
			}
			lastPositiveDestination = entry.getKey();
			ticket -= weight;
			if (ticket < 0) {
				return entry.getKey();
			}
		}

		if (lastPositiveDestination == null) {
			throw new IllegalStateException("No valid destination found for origin: " + origin);
		}
		return lastPositiveDestination;
	}

	private void validateRoutingWeights() {
		for (Map.Entry<RouteOrigin, Map<VisitorState, Integer>> row : routingWeights.entrySet()) {
			long total = 0;
			for (int weight : row.getValue().values()) {
				total += weight;
			}
			if (total == 0) {
				throw new IllegalArgumentException("Total routing weight for " + row.getKey() + " must be positive");
			}
		}
	}
	
	@Override
	protected boolean tryAdditionalCEvents() {
		if (stageRequests.isEmpty()) {
			return false;
		}
		List <StageRequest> requests = new ArrayList<>(stageRequests);
		stageRequests.clear();
		for (StageRequest request : requests) {
			Customer visitor = request.visitor();

			if (stage.admit(visitor)) {
				Trace.out(Trace.Level.INFO, "Customer #" + 
					visitor.getId() + 
					" entered the stage at " + Clock.getInstance().getTime() +
					" (attendance: " + stage.getAttendance() + "/" + stage.getCapacity() + ")");
			} else {
				Trace.out(Trace.Level.INFO, "Customer #" + 
					visitor.getId() +
					" could not enter the stage at " + Clock.getInstance().getTime() +
					"; rerouting");
				
				routeVisitor(visitor, request.origin(), false);
			}
		}
		return true;
}

	@Override
	protected void results() {
		// OLD text UI
		//System.out.println("Simulation ended at " + Clock.getInstance().getClock());
		//System.out.println("Results ... are currently missing");

		// NEW GUI
		controller.showEndTime(Clock.getInstance().getTime());
	}
}
