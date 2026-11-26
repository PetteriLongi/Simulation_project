package simu.model;

import simu.framework.IEventType;

// TODO:
// Event types are defined by the requirements of the simulation model
public enum EventType implements IEventType {
	VISITOR_ARRIVAL,
	ENTRANCE_COMPLETE,
	BAR_COMPLETE,
	TOILET_COMPLETE,
	PERFORMANCE_START,
	PERFORMANCE_END
}
