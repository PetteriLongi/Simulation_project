package simu.model;

import eduni.distributions.ContinuousGenerator;
import simu.framework.Clock;
import simu.framework.Event;
import simu.framework.EventList;

import java.util.LinkedList;

// TODO:
// Service Point functionalities & calculations (+ variables needed) and reporting to be implemented
public class ServicePoint {
	private LinkedList<Customer> jono = new LinkedList<Customer>(); // Data Structure used
	private ContinuousGenerator generator;
	private EventList eventList;
	private EventType eventTypeScheduled;
	//Queuestrategy strategy; // option: ordering of the customer
	private boolean reserved = false;

	public ServicePoint(ContinuousGenerator generator, EventList tapahtumalista, EventType tyyppi){
		this.eventList = tapahtumalista;
		this.generator = generator;
		this.eventTypeScheduled = tyyppi;
	}
	
	private double samplePositiveServiceTime() {
		for (int attempt = 0; attempt < 1000; attempt++) {
			double sample = generator.sample();
			if (sample > 0) {
				return sample;
			}
		}
		throw new IllegalStateException("Failed to generate a positive service time after 1000 attempts");
	}

	public void addQueue(Customer a){   // First customer at the queue is always on the service
		jono.add(a);
	}

	public Customer removeQueue(){		// Remove serviced customer
		reserved = false;
		return jono.poll();
	}

	public void beginService() {  		// Begins a new service, customer is on the queue during the service
		reserved = true;
		double serviceTime = samplePositiveServiceTime();
		eventList.add(new Event(eventTypeScheduled, Clock.getInstance().getTime()+serviceTime));
	}

	public boolean isReserved(){
		return reserved;
	}

	public boolean isOnQueue(){
		return jono.size() != 0;
	}
}
