package COMSETsystem;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.checkerframework.checker.nullness.qual.NonNull;

import me.tongfei.progressbar.*;


import DataParsing.*;

/**
 * The Simulator class defines the major steps of the simulation. It is
 * responsible for loading the map, creating the necessary number of agents,
 * creating a respective AgentEvent for each of them such that they are added
 * to the events PriorityQueue. Furthermore it is also responsible for dealing 
 * with the arrival of resources, map matching them to the map, and assigning  
 * them to agents. This produces the score according to the scoring rules.
 * <p>
 * The time is simulated by the events having a variable time, this time
 * corresponds to when something will be empty and thus needs some
 * interaction (triggering). There's an event corresponding to every existent
 * Agent and for every resource that hasn't arrived yet. All of this events are
 * in a PriorityQueue called events which is ordered by their time in an
 * increasing way.
 */
public class Simulator {

	// Class the contains parameters to configure system
	final Configuration configuration;

	// The map that everything will happen on.
	protected CityMap map;

	// A deep copy of map to be passed to agents. 
	// This is a way to make map unmodifiable.
	public CityMap mapForAgents;

	// The event queue.
	private PriorityQueue<Event> events = new PriorityQueue<>();

	// The set of empty agents.
	protected TreeSet<AgentEvent> emptyAgents = new TreeSet<>(new AgentEventComparator());

	// The set of agents serving resources
	protected TreeSet<AgentEvent> servingAgents = new TreeSet<>(new AgentEventComparator());

	// The beginning time of the simulation
	protected long simulationStartTime;

	// The current Simulation Time
	protected long simulationTime;

	// The simulation end time is the expiration time of the last resource.
	protected long simulationEndTime;

	protected ScoreInfo score;

	// A list of all the agents in the system. Not really used in COMSET, but maintained for
	// a user's debugging purposes.
	ArrayList<BaseAgent> agents;

	protected FleetManager fleetManager;

	// Traffic pattern
	protected TrafficPattern trafficPattern;

	public final Map<Long, AgentEvent> agentMap = new HashMap<>();
	public final Map<Long, ResourceEvent> resMap = new HashMap<>();


	/**
	 * Constructor of the class Main. This is made such that the type of
	 * agent/resourceAnalyzer used is not hardcoded and the users can choose
	 * whichever they wants.
	 **/
	public Simulator(Configuration configuration) {
		this.configuration = configuration;
		configure();
	}

	public void removeEvent(Event e) {
		events.remove(e);
	}

	/**
	 * Configure the simulation system including:
	 * 
	 * 1. Create a map from the map file and the bounding polygon KML file.
	 * 2. Load the resource data set and map match.
	 * 3. Create the event queue. 
	 *
	 * See COMSETsystem.Configuration and Main.java for detailed description of the parameters.
	 *
	 */
	private void configure() {
		// Configuration Properties for this simulation.

		map = configuration.map;


		// Make a map copy for agents to use so that an agent cannot modify the map used by
		// the simulator
		mapForAgents = map.makeCopy();

		MapWithData mapWD = new MapWithData(map, configuration.resourceFile, configuration.agentPlacementRandomSeed);

		// map match resources
		System.out.println("Loading and map-matching resources...");

		fleetManager = createFleetManager(configuration);

		// The simulation end time is the expiration time of the last resource.
		// which is return by createMapWithData
		this.simulationEndTime = mapWD.createMapWithData(configuration, this, fleetManager);
		trafficPattern = mapWD.getTrafficPattern(configuration.trafficPatternEpoch, configuration.trafficPatternStep,
				configuration.dynamicTrafficEnabled);
		fleetManager.setTrafficPattern(trafficPattern);

		// Deploy agents at random locations of the map.
		System.out.println("Randomly placing " + configuration.numberOfAgents + " agents on the map...");
		mapWD.placeAgentsRandomly(this, fleetManager, configuration.numberOfAgents);

		// Initialize the event queue.
		events = mapWD.getEvents();

		mappingEventId();
	}

	/**
	 * This method corresponds to running the simulation. An object of ScoreInfo
	 * is created in order to keep track of performance in the current
	 * simulation. Go through every event until the simulation is over.
	 *
	 */
	public void run() {
		System.out.println("Running the simulation...");

		long eventCount = 0;

		score = new ScoreInfo(configuration, this);
		if (map == null) {
			System.out.println("map is null at beginning of run");
		}

		try (ProgressBar pb = new ProgressBar("Progress:", 100, ProgressBarStyle.ASCII)) {
			assert events.peek() != null;
			simulationStartTime = simulationTime = events.peek().getTime();
			long totalSimulationTime = simulationEndTime - simulationStartTime;

			while (!events.isEmpty()) {
				assert events.peek() != null;
				long nextTime = events.peek().getTime();
				assert (nextTime >= simulationTime);
				simulationTime = nextTime;

				// Extend total simulation time for agent which is still delivering resource
				totalSimulationTime = Math.max(totalSimulationTime, simulationTime - simulationStartTime);

				eventCount++;
				Event toTrigger = events.poll();
				assert toTrigger != null;
				pb.stepTo((long)(((float)(toTrigger.getTime() - simulationStartTime))
						/ totalSimulationTime * 100.0));
				if (simulationTime <= simulationEndTime || servingAgents.size() > 0) {
					Event e = toTrigger.trigger();
					if (e != null) {
						addEvent(e);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("Simulation finished.");

		score.end();
	}

	protected static class PickUp {
		private final ResourceEvent resource;
		private final long time;

		public PickUp(ResourceEvent resource, long time) {
			this.resource = resource;
			this.time = time;
		}

		public ResourceEvent getResource() {
			return resource;
		}

		public long getTime() {
			return time;
		}

	}

	/**
	 * Compares agent events
	 */
	static class AgentEventComparator implements Comparator<AgentEvent> {

		/**
		 * Checks if two agentEvents are the same by checking their ids.
		 * 
		 * @param a1 The first agent event
		 * @param a2 The second agent event
		 * @return returns 0 if the two agent events are the same, 1 if the id of
		 * the first agent event is bigger than the id of the second agent event,
		 * -1 otherwise
		 */
		public int compare(AgentEvent a1, AgentEvent a2) {
			return Long.compare(a1.id, a2.id);
		}
	}

	/**
	 * Compares resource events
	 */
	static class ResourceEventComparator implements Comparator<ResourceEvent> {
		/**
		 * Checks if two resourceEvents are the same by checking their ids.
		 * 
		 * @param a1 The first resource event
		 * @param a2 The second resource event
		 * @return returns 0 if the two resource events are the same, 1 if the id of
		 * the resource event is bigger than the id of the second resource event,
		 * -1 otherwise
		 */
		public int compare(ResourceEvent a1, ResourceEvent a2) {
			return Long.compare(a1.id, a2.id);
		}
	}

	public boolean hasEvent(Event event) {
		return events.contains(event);
	}

	public void addEvent(Event event) {
		assert(event.getTime() >= simulationTime);
		events.add(event);
	}

	/**
	 * @param agent Add this agent to the set of empty agents
	 */
	public void markAgentEmpty(AgentEvent agent) {
		servingAgents.remove(agent);
		emptyAgents.add(agent);
	}

	/**
	 * @param agent Add this agent to the set of serving agents. A serving has been assigned a resource and is either
	 *              on its way to pickup or dropoff
	 */
	public void markAgentServing(AgentEvent agent) {
		emptyAgents.remove(agent);
		servingAgents.add(agent);
	}

	/**
	 * Make an agent copy of locationOnRoad so that an agent cannot modify the attributes of the road.
	 * 
	 * @param locationOnRoad the location to make a copy for
	 * @return an agent copy of the location 
	 */
	public LocationOnRoad agentCopy(LocationOnRoad locationOnRoad) {
		Intersection from = mapForAgents.intersections().get(locationOnRoad.road.from.id);
		Intersection to = mapForAgents.intersections().get(locationOnRoad.road.to.id);
		Road roadAgentCopy = from.roadsMapFrom.get(to);
		return LocationOnRoad.copyWithReplacedRoad(roadAgentCopy, locationOnRoad);
	}

	public FleetManager createFleetManager(Configuration configuration) {
		try {
			Constructor<? extends FleetManager> cons =
					configuration.fleetManagerClass.getConstructor(CityMap.class);
			return cons.newInstance(this.mapForAgents);
		} catch (NoSuchMethodException | IllegalAccessException | InstantiationException |
				InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void mappingEventId() {
		for (Event event : events) {
			if (event instanceof AgentEvent) {
				agentMap.put(event.id, (AgentEvent) event);
			} else if (event instanceof ResourceEvent) {
				resMap.put(event.id, (ResourceEvent) event);
			}
		}
	}
}
