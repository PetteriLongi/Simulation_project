package simu.model;

import java.util.ArrayList;
import java.util.List;

public class Stage {
    private final int capacity;
    private final List<Customer> attendees = new ArrayList<>();
    private boolean performanceActive;

    public Stage(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Stage capacity must be greater than zero.");
        }
        this.capacity = capacity;
    }
    public void startPerformance() { 
        if (performanceActive) {
            throw new IllegalStateException("Performance is already active.");
        }
        performanceActive = true;
    }
    public List<Customer> endPerformance() {
        if (!performanceActive) {
            throw new IllegalStateException("No performance is active");
        }

        performanceActive = false;
        List<Customer> departingVisitors = new ArrayList<>(attendees);
        attendees.clear();
        return departingVisitors;
    }

    public boolean admit(Customer visitor) {
        if (!performanceActive || attendees.size() >= capacity) {
            return false;
        }

        attendees.add(visitor);
        visitor.setState(VisitorState.STAGE);
        return true;
    }

    public boolean isPerformanceActive() {
        return performanceActive;
    }

    public int getAttendance() {
        return attendees.size();
    }

    public int getCapacity() {
        return capacity;
    }

    public void reset() {
        attendees.clear();
        performanceActive = false;
    }
}