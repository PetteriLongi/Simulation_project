package controller;

import javafx.application.Platform;
import simu.framework.IEngine;
import simu.model.MyEngine;
import view.ISimulatorUI;
import simu.model.FestivalConfig;
import java.io.IOException;

public class Controller implements IControllerVtoM, IControllerMtoV {   // NEW
	private IEngine engine;
	private ISimulatorUI ui;
	
	public Controller(ISimulatorUI ui) {
		this.ui = ui;
	}

	/* Engine control: */
	@Override
	public boolean startSimulation() {
		try {
			FestivalConfig config = FestivalConfig.load();
			
			double duration = ui.getTime();
			int expectedVisitors = ui.getExpectedVisitors();

			if (!Double.isFinite(duration) || duration <= 0) {
				throw new IllegalArgumentException("Simulation time must be a positive number.");
			}
			if (expectedVisitors < 0) {
				throw new IllegalArgumentException("Expected visitors cant be a negative number.");
			}
			
			engine = new MyEngine(this, config, duration, expectedVisitors); // NEW
			engine .setSimulationTime(duration);
			engine.setDelay(ui.getDelay());
			ui.getVisualisation().clearDisplay();
			((Thread) engine).start();
			return true;
		} catch (IOException | IllegalArgumentException e) {
			ui.showError(e.getMessage());
			return false;
		}
	}
	
	@Override
	public void decreaseSpeed() { // hidastetaan moottorisäiettä
		engine.setDelay((long)(engine.getDelay()*1.10));
	}

	@Override
	public void increaseSpeed() { // nopeutetaan moottorisäiettä
		engine.setDelay((long)(engine.getDelay()*0.9));
	}


	/* Simulation results passing to the UI
	 * Because FX-UI updates come from engine thread, they need to be directed to the JavaFX thread
	 */
	@Override
	public void showEndTime(double time) {
		Platform.runLater(()->ui.setEndingTime(time));
	}

	@Override
	public void visualiseCustomer() {
		Platform.runLater(() -> ui.getVisualisation().newCustomer());
	}
}
