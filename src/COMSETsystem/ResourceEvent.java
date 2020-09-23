package COMSETsystem;

import java.util.logging.Level;
import java.util.logging.Logger;
import COMSETsystem.FleetManager.ResourceState;

/**
 *
 * @author TijanaKlimovic
 *
 * The ResourceEvent class represents the moment a resource becomes available or expired in
 * the simulation. Therefore there are two cases in which a ResourceEvent object is triggered:
 * 
 * 1. When the resource is introduced to the system and becomes available. In this case, the 
 * resource is assigned (if there are agents available and within reach) to an agent.
 * 2. When the resource gets expired.
 */
public class ResourceEvent extends Event {

	enum State {
		AVAILABLE,
		EXPIRED
	}

	// The location at which the resource is introduced.
	public final LocationOnRoad pickupLoc;
	// The destination of the resource.
	public final LocationOnRoad dropoffLoc;

	// The time at which the resource is introduced
	final long availableTime;

	// The time at which the resource will be expired
	final long expirationTime;

	long pickupTime;

	AgentEvent agentEvent = null;

	State state;

	// The shortest travel time from pickupLoc to dropoffLoc
	public final long staticTripTime;

	/**
	 * Constructor for class ResourceEvent.
	 *
	 * @param availableTime time when this agent is introduced to the system.
	 * @param pickupLoc this resource's location when it becomes available.
	 * @param dropoffLoc this resource's destination location.
	 * @param simulator the simulator object.
	 * @param fleetManager the fleet manager object.
	 * @param resourceMaximumLifeTime time interval that resource waits and expires after that.
	 */
	public ResourceEvent(LocationOnRoad pickupLoc, LocationOnRoad dropoffLoc, long availableTime, long staticTripTime,
						 Simulator simulator, FleetManager fleetManager, long resourceMaximumLifeTime) {
		super(availableTime, simulator, fleetManager);
		this.pickupLoc = pickupLoc;
		this.dropoffLoc = dropoffLoc;
		this.availableTime = availableTime;
		this.expirationTime = availableTime + resourceMaximumLifeTime;
		this.staticTripTime = staticTripTime;
		this.pickupTime = -1;
		this.state = State.AVAILABLE;
	}

	/**
	 * Constructor for ResourceEvent that overrides tripTime. Makes it easier to test.
	 *
	 * @param availableTime time when this agent is introduced to the system.
	 * @param pickupLoc this resource's location when it becomes available.
	 * @param dropoffLoc this resource's destination location.
	 * @param staticTripTime the time it takes to go from pickUpLoc and dropoffLoc under static traffic condition
	 * @param simulator the simulator object.
	 * @param resourceMaximumLifeTime time interval that resource waits and expires after that.
	 */
	protected ResourceEvent(LocationOnRoad pickupLoc, LocationOnRoad dropoffLoc, long availableTime,
							long staticTripTime, Simulator simulator, long resourceMaximumLifeTime) {
		super(availableTime);
		this.pickupLoc = pickupLoc;
		this.dropoffLoc = dropoffLoc;
		this.availableTime = availableTime;
		this.expirationTime = availableTime + resourceMaximumLifeTime;
		this.staticTripTime = staticTripTime;
		this.pickupTime = -1;
	}

	/**
	 * Whenever a resource arrives/becomes available an event corresponding to
	 * it gets triggered. When it triggers it checks for all the active agents
	 * which agent can get to the resource the fastest. The closest agent is
	 * saved in the variable bestAgent. If there are no active agents or no
	 * agent can get in time to the resource, the current resource gets added to
	 * waitingResources such that once an agent gets available it will check if
	 * it can get to the resource in time. Furthermore, calculate the score of
	 * this assignment according to the scoring rules. Also remove the assigned
	 * agent from the PriorityQueue and from activeAgents.
	 */
	@Override
	Event trigger() throws UnsupportedOperationException {
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "******** ResourceEvent id = "+ id +
				" triggered at time " + getTime(), this);
		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Loc = " + this.pickupLoc + "," + this.dropoffLoc, this);

		if (pickupLoc == null) {
			System.out.println("intersection is null");
		}

		switch (state) {
			case AVAILABLE:
				available();
				return this;
			case EXPIRED:
			default:
				expire();
				return null;
		}
	}

	void assignTo(AgentEvent event) {
		this.agentEvent = event;
	}

	Resource copyResource() {
		return new Resource(id, expirationTime, agentEvent == null ? -1 : agentEvent.id, simulator.agentCopy(pickupLoc), simulator.agentCopy(dropoffLoc));
	}

	void pickup(AgentEvent agentEvent, long pickupTime) {
		this.pickupTime = pickupTime;
		simulator.removeEvent(this);
	}

	boolean isPickedup() {
		return pickupTime > 0;
	}

	void dropOff(long dropOffTime) {
		long staticTripTime = simulator.map.travelTimeBetween(pickupLoc, dropoffLoc);
		simulator.score.recordCompletedTrip(dropOffTime, pickupTime, staticTripTime);
	}

	private void available() throws UnsupportedOperationException {
		++simulator.score.totalResources;

		AgentAction action = fleetManager.onResourceAvailabilityChange(copyResource(), ResourceState.AVAILABLE, simulator.agentCopy(pickupLoc), getTime());
		processAgentAction(action);
		setTime(expirationTime);
		state = State.EXPIRED;
	}

	private void expire() throws UnsupportedOperationException {
		// Expiration can only happen if the resource has not been picked up.
		assert !isPickedup() : "Resource expiring after having been picked up";

		AgentAction action = fleetManager.onResourceAvailabilityChange(copyResource(), ResourceState.EXPIRED,
				simulator.agentCopy(pickupLoc), getTime());
		processAgentAction(action);
		if (agentEvent != null) {
			// We're assigned but hasn't been picked up, so the trip is being aborted.
			agentEvent.abortResource();
			simulator.score.recordAbortion();
		}

		simulator.score.recordExpiration();

		Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Expired.", this);
	}

	private void processAgentAction(AgentAction agentAction) throws UnsupportedOperationException {
		if (agentAction == null) {
			return;
		}

		AgentEvent agentEvent = simulator.agentMap.get(agentAction.agentId);
		ResourceEvent resourceEvent = simulator.resMap.get(agentAction.resId);

		if (agentEvent != null && resourceEvent != null && !agentEvent.hasResPickup()) {
			agentEvent.assignTo(resourceEvent, getTime());
			resourceEvent.assignTo(agentEvent);
		}
	}
}
