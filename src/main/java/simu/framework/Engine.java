package simu.framework;

import controller.IControllerMtoV;
import simu.model.ServicePoint;

public abstract class Engine extends Thread implements IEngine {  // NEW DEFINITIONS
	private double simulationTime = 0;	// time when the simulation will be stopped
	private long delay = 0;
	private Clock clock;				// in order to simplify the code (clock.getClock() instead Clock.getInstance().getClock())
	
	protected EventList eventList;
	protected ServicePoint[] servicePoints;
	protected IControllerMtoV controller; // NEW

	public Engine(IControllerMtoV controller) {	// NEW
		this.controller = controller;  			// NEW
		clock = Clock.getInstance();
		eventList = new EventList();
		/* Service Points are created in simu.model-package's class who is inheriting the Engine class */
	}

	@Override
	public void setSimulationTime(double time) {
		simulationTime = time;
	}
	
	@Override // NEW
	public void setDelay(long time) {
		this.delay = time;
	}
	
	@Override // NEW
	public long getDelay() {
		return delay;
	}
	
	@Override
	public void run() {
		clock.reset();
		eventList.clear();
		initialization(); // creating, e.g., the first event

		while (!eventList.isEmpty()) {
			Event nextEvent = eventList.peek();

			if (nextEvent.getTime() > simulationTime) {
				break;
			}

			delay(); // NEW
			clock.setTime(eventList.getNextTime());
			runBEvents();
			tryCEvents();
		}
		clock.setTime(simulationTime);
		results();
	}
	
	private void runBEvents() {
		while (!eventList.isEmpty() && eventList.getNextTime() == clock.getTime()){
			runEvent(eventList.remove());
		}
	}

	private void tryCEvents() {    // define protected, if you want to overwrite
		boolean eventExecuted;
		do {
			eventExecuted = false;
			for (ServicePoint point: servicePoints){
				if (!point.isReserved() && point.isOnQueue()){
					point.beginService();
					eventExecuted = true;
				}
			}
			if (tryAdditionalCEvents()) {
				eventExecuted = true;
			}
		} while (eventExecuted);
	}

	private void delay() { // NEW
		Trace.out(Trace.Level.INFO, "Delay " + delay);
		try {
			sleep(delay);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	protected boolean tryAdditionalCEvents() { // define protected, if you want to overwrite
		return false;
	}

	protected abstract void initialization(); 	// Defined in simu.model-package's class who is inheriting the Engine class
	protected abstract void runEvent(Event t);	// Defined in simu.model-package's class who is inheriting the Engine class
	protected abstract void results(); 			// Defined in simu.model-package's class who is inheriting the Engine class
}