package simu.model;

import simu.framework.Clock;
import simu.framework.Trace;


// TODO:
// Customer to be implemented according to the requirements of the simulation model (data!)
public class Customer {
	private double arrivalTime;
	private double removalTime;
	private int id;
	private static int i = 1;
	private static double sum = 0;
	private static int completedCustomerCount = 0;
	private VisitorState state = VisitorState.DECISION;
	private int completedActivities = 0;
	
	public Customer() {
	    id = i++;
	    
		arrivalTime = Clock.getInstance().getTime();
		Trace.out(Trace.Level.INFO, "New customer #" + id + " arrived at  " + arrivalTime);
	}

	public double getRemovalTime() {
		return removalTime;
	}

	public void setRemovalTime(double removalTime) {
		this.removalTime = removalTime;
	}

	public double getArrivalTime() {
		return arrivalTime;
	}

	public void setArrivalTime(double arrivalTime) {
		this.arrivalTime = arrivalTime;
	}
	public static void resetForNewRun() {
		i = 1;
		sum = 0;
		completedCustomerCount = 0;
	}
	public int getId() {
		return id;
	}
	public VisitorState getState() {
		return state;
	}
	public void setState(VisitorState state) {
		this.state = state;
	}
	public int getCompletedActivities() {
		return completedActivities;
	}
	public void recordCompletedActivity() {
		completedActivities++;
	}
	
	public void reportResults() {
		Trace.out(Trace.Level.INFO, "\nCustomer " + id + " ready! ");
		Trace.out(Trace.Level.INFO, "Customer "   + id + " arrived: " + arrivalTime);
		Trace.out(Trace.Level.INFO,"Customer "    + id + " removed: " + removalTime);
		Trace.out(Trace.Level.INFO,"Customer "    + id + " stayed: "  + (removalTime - arrivalTime));

		sum += removalTime - arrivalTime;
		completedCustomerCount++;

		double mean = sum / completedCustomerCount;
		System.out.println("Average time inside festival for completed visitors: " + mean);
	}

}
