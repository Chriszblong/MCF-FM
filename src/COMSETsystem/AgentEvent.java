package COMSETsystem;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TijanaKlimovic
 * <p>
 * The AgentEvent class represents a moment an agent is going to perform an
 * action in the simmulation, such as becoming empty and picking up a
 * resource, or driving to some other Intersection.
 * <p>
 * An AgentEvent is triggered in either of the following cases:
 * <p>
 * 1. The agent reaches an intersection.
 * 2. The agent drops off a resource.
 * <p>
 * In the case that the agent reaches an intersection, the AgentEvent invokes agent.nextIntersection()
 * to let the agent determine which of the neighboring intersections to go to. The AgentEvent is triggered
 * again when the agent reaches the next intersection, and so on. This is how the agent's search route
 * is executed. The searching ends when the agent is assigned to a resource, in which case the AgentEvent
 * is set to be triggered at the time when the agent drops off the resource to its destination.
 * <p>
 * In the case that the agent drops off a resource, the AgentEvent checks if there are waiting resources. If so,
 * the AgentEvent assigns the agent to the closest waiting resource if the travel time from the agent's current location
 * to the resource is smaller than the resource's remaining life time. Otherwise the AgentEvent moves the agent to
 * the end intersection of the current road.
 */
public class AgentEvent extends Event {

	enum State {
		INITIAL,
		INTERSECTION_REACHED,
		PICKING_UP,
		DROPPING_OFF,
	}

	// The location at which the event is triggered.
	LocationOnRoad loc;


	boolean isPickup = false;

	State state = State.INITIAL;

	/*
	 * The time at which the agent started to search for a resource. This is also the
	 * time at which the agent drops off a resource.
	 */
	long startSearchTime;

	ResourceEvent assignedResource;
	long assignTime;
	LocationOnRoad assignLocation;

	long lastAppearTime;
	LocationOnRoad lastAppearLocation;

	/**
	 * Constructor for class AgentEvent.
	 *
	 * @param loc this agent's location when it becomes empty.
	 */
	public AgentEvent(LocationOnRoad loc, long startedSearch, Simulator simulator, FleetManager fleetManager) {
		super(startedSearch, simulator, fleetManager);
		this.loc = loc;
		this.startSearchTime = startedSearch;
		this.lastAppearTime = startedSearch;
		this.lastAppearLocation = loc;
	}

	@Override
	Event trigger() throws Exception {
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "******** AgentEvent id = " + id+
				" triggered at time " + getTime(), this);
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Loc = " + loc, this);

		switch (state) {
			case INITIAL:
				navigateToNearestIntersection();
				break;
			case INTERSECTION_REACHED:
				navigate();
				break;
			case PICKING_UP:
				if (assignedResource == null) {
					moveToEndIntersection();
				} else {
					pickup();
				}
				break;
			case DROPPING_OFF:
				dropOff();
				break;
		}
		return this;
	}

	boolean hasResPickup() {
		return isPickup;
	}

	void assignTo(ResourceEvent resourceEvent, long assignTime) throws UnsupportedOperationException {
		long elapseTime = assignTime - lastAppearTime;

		LocationOnRoad currentLocation = simulator.trafficPattern.travelRoadForTime(lastAppearTime, lastAppearLocation, elapseTime);
		this.assignLocation = currentLocation;
		this.assignTime = assignTime;
		assignResource(resourceEvent);

		if (isOnSameRoad(loc, assignedResource.pickupLoc)) {
			// check if loc is closer to the start of the road than pickupLoc
			if (currentLocation.upstreamTo(assignedResource.pickupLoc)) {
				long nextEventTime = assignTime + simulator.trafficPattern.roadForwardTravelTime(assignTime, currentLocation, assignedResource.pickupLoc);
				simulator.removeEvent(this);
				update(nextEventTime, assignedResource.pickupLoc, State.PICKING_UP, assignTime, currentLocation);

				simulator.addEvent(this);
			}
		}
	}

	void abortResource() {
		// Since we were on the event queue, we need to remove ourselves before rescheduling ourselves.
		simulator.removeEvent(this);
		unassignResource();
		isPickup = false;
		if (state == State.PICKING_UP) {
			moveToEndIntersection();
		}
		simulator.addEvent(this);
	}

	void navigate() throws Exception {
		assert loc.atEndIntersection() : "Agent not at an intersection.";

		if (isArrivingPickupLoc()) {
			long travelTime = fleetManager.trafficPattern.roadTravelTimeFromStartIntersection(getTime(), assignedResource.pickupLoc);
			long nextEventTime = getTime() + travelTime;
			update(nextEventTime, assignedResource.pickupLoc, State.PICKING_UP, getTime(), loc);
			return;
		}

		if (isArrivingDropOffLoc()) {
			long travelTime = fleetManager.trafficPattern.roadTravelTimeFromStartIntersection(getTime(), assignedResource.dropoffLoc);
			long nextEventTime = getTime() + travelTime;
			update(nextEventTime, assignedResource.dropoffLoc, State.DROPPING_OFF, getTime(), loc);
			return;
		}


		Intersection nextIntersection;
		if (isPickup && assignedResource != null) {
			nextIntersection = fleetManager.onReachIntersectionWithResource(id, getTime(), simulator.agentCopy(loc),
					assignedResource.copyResource());
		} else {
			nextIntersection = fleetManager.onReachIntersection(id, getTime(), simulator.agentCopy(loc));
		}

		if (nextIntersection == null) {
			throw new Exception("FleetManager did not return a next location");
		}

		if (!loc.road.to.isAdjacent(nextIntersection)) {
			throw new Exception("move not made to an adjacent location");
		}

		// set location and time of the next trigger
		Road nextRoad = loc.road.to.roadTo(nextIntersection);
		LocationOnRoad nextLocation = LocationOnRoad.createFromRoadEnd(nextRoad);
		long travelTime = fleetManager.trafficPattern.roadTravelTimeFromStartIntersection(getTime(), nextLocation);
		update(getTime() + travelTime, nextLocation, State.INTERSECTION_REACHED, getTime(), LocationOnRoad.createFromRoadStart(nextRoad));

		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Move to " + nextRoad.to, this);
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Next trigger time = " + getTime(), this);
	}

	private void navigateToNearestIntersection() {
		startSearchTime = getTime();

		fleetManager.onAgentIntroduced(id, simulator.agentCopy(loc), getTime());

		moveToEndIntersection();
	}

	private boolean isArrivingPickupLoc() {
		return !isPickup && assignedResource != null && assignedResource.pickupLoc.road.from.equals(loc.road.to);
	}

	private boolean isArrivingDropOffLoc() {
		return isPickup && assignedResource != null && assignedResource.dropoffLoc.road.from.equals(loc.road.to);
	}

	/*
	 * The handler of a pick up event.
	 */
	private void pickup() throws UnsupportedOperationException {
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Pickup at " + loc, this);

		isPickup = true;
		long searchTime = getTime() - startSearchTime;
		long approachTime = getTime() - assignTime;
		long staticApproachTime = simulator.map.travelTimeBetween(assignLocation, loc);

		simulator.score.recordApproachTime(getTime(), startSearchTime, assignTime, assignedResource.availableTime,
				staticApproachTime);

		assignedResource.pickup(this, getTime());

		AgentAction action = fleetManager.onResourceAvailabilityChange(assignedResource.copyResource(), FleetManager.ResourceState.PICKED_UP, simulator.agentCopy(loc), getTime());

		if (isValidAssignmentAction(action)) {
			ResourceEvent resourceEvent = simulator.resMap.get(action.resId);
			AgentEvent agentEvent = simulator.agentMap.get(action.agentId);
			agentEvent.assignTo(resourceEvent, getTime());
			resourceEvent.assignTo(agentEvent);
		}

		if (isOnSameRoad(assignedResource.dropoffLoc, loc) && loc.upstreamTo(assignedResource.dropoffLoc)) {
			long travelTime = fleetManager.trafficPattern.roadForwardTravelTime(getTime(), loc, assignedResource.dropoffLoc);
			long nextEventTime = getTime() + travelTime;
			update(nextEventTime, assignedResource.dropoffLoc, State.DROPPING_OFF, getTime(), loc);
		} else {
			moveToEndIntersection();
		}
	}

	/*
	 * The handler of a drop off event.
	 */
	private void dropOff() throws UnsupportedOperationException {
		startSearchTime = getTime();

		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Dropoff at " + loc, this);

		isPickup = false;
		assignedResource.dropOff(getTime());

		AgentAction action = fleetManager.onResourceAvailabilityChange(assignedResource.copyResource(), FleetManager.ResourceState.DROPPED_OFF, simulator.agentCopy(loc), getTime());

		if (!isValidAssignmentAction(action)) {
			unassignResource();
			simulator.markAgentEmpty(this);
			moveToEndIntersection();
			return;
		}


		ResourceEvent resourceEvent = simulator.resMap.get(action.resId);
		if (action.agentId == id) {
			assignResource(resourceEvent);
			assignedResource.assignTo(this);
			assignTime = getTime();
			assignLocation = loc;

			if (isOnSameRoad(loc, assignedResource.pickupLoc) && loc.upstreamTo(assignedResource.pickupLoc)) {
				// Reach resource pickup location before reach the end intersection
				long travelTime = fleetManager.trafficPattern.roadForwardTravelTime(getTime(), loc, assignedResource.pickupLoc);
				long nextEventTime = getTime() + travelTime;
				update(nextEventTime, assignedResource.pickupLoc, State.PICKING_UP, getTime(), loc);
			} else {
				moveToEndIntersection();
			}
		} else {
			AgentEvent agentEvent = simulator.agentMap.get(action.agentId);
			agentEvent.assignTo(resourceEvent, getTime());
			resourceEvent.assignTo(agentEvent);

			unassignResource();
			moveToEndIntersection();
		}
	}

	protected void moveToEndIntersection() {
		long travelTime = fleetManager.trafficPattern.roadTravelTimeToEndIntersection(getTime(), loc);
		long nextEventTime = getTime() + travelTime;
		LocationOnRoad nextLoc = LocationOnRoad.createFromRoadEnd(loc.road);
		update(nextEventTime, nextLoc, State.INTERSECTION_REACHED, getTime(), loc);
	}

	private void update(long time, LocationOnRoad loc, State state, long lastAppearTime, LocationOnRoad lastAppearLocation) {
		assert time >= simulator.simulationTime : "trying to update event to the past time";
		this.setTime(time);
		this.loc = loc;
		this.state = state;
		this.lastAppearTime = lastAppearTime;
		this.lastAppearLocation = lastAppearLocation;
	}

	private boolean isValidAssignmentAction(AgentAction agentAction) {
		if (agentAction == null) return false;

        long agentId = agentAction.agentId;
        long resId = agentAction.resId;

        AgentEvent agentEvent = simulator.agentMap.get(agentId);
		ResourceEvent resEvent = simulator.resMap.get(resId);

		return agentEvent != null && resEvent != null && !agentEvent.hasResPickup() && agentAction.getType() == AgentAction.Type.ASSIGN;
	}

	private boolean isOnSameRoad(LocationOnRoad loc1, LocationOnRoad loc2) {
		return loc1.road.equals(loc2.road);
	}

	/**
	 * @param resourceEvent Assign this rosource to this agent
	 */
	private void assignResource(ResourceEvent resourceEvent) {
		assert assignedResource == null;
		assignedResource = resourceEvent;
		simulator.markAgentServing(this);
	}

	/**
	 * Unassign the resource current assigned to this agent.
	 */
	private void unassignResource() {
		assert assignedResource != null;
		assignedResource = null;
		simulator.markAgentEmpty(this);
	}
}
