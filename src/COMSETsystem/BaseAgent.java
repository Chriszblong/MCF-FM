package COMSETsystem;

/**
 * The BaseAgent class is the class the participants should extend in order to create an Agent that will be used in
 * Simulator to run the simulation. The participants should extend the BaseAgent class to implement their agent search
 * strategies.
 */
public abstract class BaseAgent {

	// Every agent gets a reference to the map
	protected final CityMap map;

	// An id that is unique across all agents and resources 
	protected final long id;

	/**
	 * BaseAgent constructor. 
	 *
	 * @param id An id that is unique among all agents and resources
	 * @param map The map
	 */
	public BaseAgent (long id, CityMap map) {
		this.id = id;
		this.map = map;
	}

	/**
	 * This is a callback method called when the agent drops off a resource or when the previous route is finished. 
	 * The agent uses this method to plan a route which defines what is the intersection to return for each of 
	 * the subsequent nextIntersection calls. 
	 * 
	 * See UserExamples.AgentRandomDestination and UserExamples.AgentRandomWalk for two examples how planSearchRoute works.
	 * 
	 * This method must be overridden in every Agent implementation.
	 */
	public abstract void planSearchRoute(LocationOnRoad currentLocation, long currentTime);

	/**
	 * This method must be overridden in every Agent implementation in order to return an Intersection that the
	 * Simulator can use to move the Agent.
	 * 
	 * This method is a callback method which is called when the agent reaches an intersection. The agent decides which
	 * of the neighboring intersections to go to. The Simulator will move the agent to the returned intersection and then 
	 * call this method again, and so on. This is how a planned route is executed by the Simulator. 
	 *
	 * @param currentLocation The agent's location at the time when the method is called 
	 * @param currentTime The time at which the method is invoked
	 * @return Intersection that the Agent is going to move to
	 */
	public abstract Intersection nextIntersection(LocationOnRoad currentLocation, long currentTime);

	/**
	 * This method is to inform the agent that it is assigned to a resource. No action is necessary from the agent. The agent
	 * will be automatically moved to the destination of the resource. The method is provided in case that the agent wants to use
	 * the assignment information to assist its future routing strategy.
	 * 
	 * This method must be overridden in every Agent implementation.
	 *   
	 * @param currentLocation The agent's location at the time when the method is called
	 * @param currentTime The time at which the assignment occurs
	 * @param resourceId The id of the resource to which the agent is assigned
	 * @param resourcePickupLocation The pickup location of the resource to which the agent is assigned
	 * @param resourceDropoffLocation The dropoff location of the resource to which the agent is assigned
	 */
	public abstract void assignedTo(LocationOnRoad currentLocation, long currentTime, long resourceId, LocationOnRoad resourcePickupLocation, LocationOnRoad resourceDropoffLocation);
}