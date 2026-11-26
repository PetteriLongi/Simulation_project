package view;

public interface ISimulatorUI {
	// The Controller needs input which is passed to the Engine
	public double getTime();
	public long getDelay();
	int getExpectedVisitors();
	
	// Controller gives Engine produced results to the UI
	public void setEndingTime(double time);
	
	// Controller requires
	public IVisualisation getVisualisation();

	void showError(String message);
}
