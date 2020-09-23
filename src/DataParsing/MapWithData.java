package DataParsing;

import COMSETsystem.*;

import java.time.ZoneId;
import java.util.*;

/**
 * The MapWithData class is responsible for loading a resource dataset file,
 * map matching resources, and create a list of resource events.  
 */
public class MapWithData {

	// Map without any data added to it
	public CityMap map;     

	// Full path of the file containing the resources to be loaded to the simulator
	private final String resourceFile;

	// Priority queue of events
	public PriorityQueue<Event> events;

	// The earliest resource introduction time. The time is used to determine the time at which
	// agents are to be deployed. The agents are to be deployed at time earliestResourceTime - 1.
	public long earliestResourceTime = Long.MAX_VALUE;

	// The latest possible time that a resource is dropped off. The time is used to determine the time at which
	// the simulation is to stop.
	public long latestResourceTime = -1; 
	
	// Seed for the random number generator when placing agents.
	public long agentPlacementRandomSeed;
	
	// Time Zone ID of the map; for conversion from the time stamps in a resource dataset file to Linux epochs.
	protected ZoneId zoneId;

	private ArrayList<Resource> resourcesParsed;

	/**
	 * Constructor of MapWithData
	 * @param map reference to the map
	 * @param resourceFile full path to the resource file
	 * @param agentPlacementRandomSeed Seed for randome number that generates agent placements
	 */
	public MapWithData(CityMap map, String resourceFile, long agentPlacementRandomSeed) {
		this.map = map;
		this.resourceFile = resourceFile;
		this.agentPlacementRandomSeed = agentPlacementRandomSeed;
		events = new PriorityQueue<>();
		zoneId = map.computeZoneId();
	}

	/**
	 * Maps each agent and each resource onto the nearest location on the map
	 * according to the agent/resource's longitude and latitude. Creates resource events 
	 * for each passenger record obtained from the resource file and adds them to the events
	 * priority queue.
	 *
	 * @param configuration configuration object containing run-time parameters
	 * @param simulator Simulator object with whose methods agent and resource events can
	 * be created.
	 * @return long the latest resource time
	 */
	// FIXME: Pass in configuration here too instead of accessing it with the singleton.
	public long createMapWithData(Configuration configuration, Simulator simulator, FleetManager fleetManager) {


		CSVNewYorkParser parser = new CSVNewYorkParser(resourceFile, zoneId);
		resourcesParsed = parser.parse(Configuration.timeResolution);
		try {
            for (Resource resource : resourcesParsed) {
				// map matching
				LocationOnRoad pickupMatch = mapMatch(resource.getPickupLon(), resource.getPickupLat());
				LocationOnRoad dropoffMatch = mapMatch(resource.getDropoffLon(), resource.getDropoffLat());

				// TODO: won't need trip time
				long staticTripTime = simulator.mapForAgents.travelTimeBetween(pickupMatch, dropoffMatch);

				resource.setPickupLocation(pickupMatch);
				resource.setDropoffLocation(dropoffMatch);

				ResourceEvent ev = new ResourceEvent(pickupMatch, dropoffMatch, resource.getTime(), staticTripTime,
						simulator, fleetManager, configuration.resourceMaximumLifeTime);
				events.add(ev);

				//  track earliestResourceTime and latestResourceTime
				if (resource.getTime() < earliestResourceTime) {
					earliestResourceTime = resource.getTime();
				}


				final long resourceMaximumLifeTime = configuration.resourceMaximumLifeTime;
				if (resource.getTime() + resourceMaximumLifeTime +ev.staticTripTime > latestResourceTime) {
					latestResourceTime = resource.getTime() + resourceMaximumLifeTime + ev.staticTripTime;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return latestResourceTime;
	}

	public TrafficPattern getTrafficPattern(long trafficPatternEpoch, long trafficPatternStep,
											boolean dynamicTrafficEnabled) {
		System.out.println("Building traffic patterns...");
		return buildSlidingTrafficPattern(resourcesParsed, trafficPatternEpoch, trafficPatternStep,
				dynamicTrafficEnabled);

	}

	/**
	 * Match a point to the closest location on the map
	 */
	public LocationOnRoad mapMatch(double longitude, double latitude) {
		Link link = map.getNearestLink(longitude, latitude);
		double [] xy = map.projector().fromLatLon(latitude, longitude);
		double [] snapResult = snap(link.from.getX(), link.from.getY(), link.to.getX(), link.to.getY(), xy[0], xy[1]);
		double distanceFromStartVertex = this.distance(snapResult[0], snapResult[1], link.from.getX(), link.from.getY());

		// find the begin distance of link
		double distanceFromStartIntersection = 0;
		for (Link aLink : link.road.links) {
			if (aLink.id == link.id) {
				distanceFromStartIntersection += distanceFromStartVertex;
				break;
			} else {
				distanceFromStartIntersection += aLink.length;
			}
		}
		return new LocationOnRoad(link.road, distanceFromStartIntersection);
	}

	/**
	 * Find the closest point on a line segment with end points (x1, y1) and
	 * (x2, y2) to a point (x ,y), a procedure called snap.
	 *
	 * @param x1 x-coordinate of an end point of the line segment
	 * @param y1 y-coordinate of an end point of the line segment
	 * @param x2 x-coordinate of another end point of the line segment
	 * @param y2 y-coordinate of another end point of the line segment
	 * @param x x-coordinate of the point to snap
	 * @param y y-coordinate of the point to snap
	 *
	 * @return the closest point on the line segment.
	 */
	public double[] snap(double x1, double y1, double x2, double y2, double x, double y) {
		double[] snapResult = new double[3];
		double dist;
		double length = (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);

		if (length == 0.0) {
			dist = this.distance(x1, y1, x, y);
			snapResult[0] = x1;
			snapResult[1] = y1;
			snapResult[2] = dist;
		} else {
			double t = ((x - x1) * (x2 - x1) + (y - y1) * (y2 - y1)) / length;
			if (t < 0.0) {
				dist = distance(x1, y1, x, y);
				snapResult[0] = x1;
				snapResult[1] = y1;
				snapResult[2] = dist;
			} else if (t > 1.0) {
				dist = distance(x2, y2, x, y);
				snapResult[0] = x2;
				snapResult[1] = y2;
				snapResult[2] = dist;
			} else {
				double proj_x = x1 + t * (x2 - x1);
				double proj_y = y1 + t * (y2 - y1);
				dist = distance(proj_x, proj_y, x, y);
				snapResult[0] = proj_x;
				snapResult[1] = proj_y;
				snapResult[2] = dist;
			}
		}
		return snapResult;
	}

	/**
	 * Compute the Euclidean distance between point (x1, y1) and point (x2, y2).
	 */
	public double distance(double x1, double y1, double x2, double y2) {
		return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
	}

	/**
	 * Creates agent events that are randomly placed on map.
	 *
	 * @param numberOfAgents number of agents to be simulated
	 * @param simulator a reference to the simulator object
	 */
	public void placeAgentsRandomly(Simulator simulator, FleetManager fleetManager, long numberOfAgents) {
		long deployTime = earliestResourceTime - 1;

		Random generator = new Random(agentPlacementRandomSeed);
		for (int i = 0; i < numberOfAgents; i++) {
			int road_id = generator.nextInt(map.roads().size());
			Road road = map.roads().get(road_id);
			double distanceFromStartIntersection = generator.nextDouble() * road.length;
			LocationOnRoad locationOnRoad = new LocationOnRoad(road, distanceFromStartIntersection);

			AgentEvent ev = new AgentEvent(locationOnRoad, deployTime, simulator, fleetManager);
			simulator.markAgentEmpty(ev);
			events.add(ev);
		}
	}

	/**
	 * 
	 * @return events
	 */
	public PriorityQueue<Event> getEvents() {
		return events;
	}

	/**
	 * Build a traffic pattern to adjust travel speed at each road over the time of a day.
	 * The travel speed is computed based on the road segment's speed limit and the TLC Trip Record data to
	 * reflect the traffic pattern over the time of a day. The calibration goes as follows.
	 *
	 * 1. For every step (e.g., minute) of a day, compute the average trip duration of all trips recorded in the
	 * TLC Trip Record data that fall into a epoch time window (e.g., 15 minutes) starting at the current minute;
	 * call it the TLC_average_trip_duration.
	 * 2. For each trip, compute the shortest travel time from the pickup location of the trip to the
	 * dropoff location using speed limits.
	 * 3. Compute the average shortest travel time of all trips; call it the map_average_trip_duration.
	 * 4. For each road segment, travel_speed_of_current_minute = speed_limit * ((map_average_trip_duration)/(TLC_average_trip_duration)).
	 *
	 * In other words, we adjust the travel speeds so that the average trip time produced by COMSET is consistent with that of the real data.
	 *
	 * @param resources set of resources that will be used to compute speed factors.
	 * @param epoch the window of time that determines the speed factor
	 * @param step the resolution of the time-of-day speed depenence.
	 * @param dynamicTraffic true if we will be simulated with time-of-day dependent traffic.
	 * @return traffic pattern
	 */
	public TrafficPattern buildSlidingTrafficPattern(ArrayList<Resource> resources, long epoch, long step,
													 boolean dynamicTraffic) {
		// sort resources by pickup
		resources.sort(Comparator.comparingLong(TimestampAbstract::getTime));
		TrafficPattern trafficPattern = new TrafficPattern(step);
		long epochBeginTime = resources.get(0).getPickupTime();
		int beginResourceIndex = 0;
		double lastKnownSpeedFactor = 0.3; // default to 0.3 if no trip data available
		while (true) {
			ArrayList<Resource> epochResources = new ArrayList<>();
			long epochEndTime = epochBeginTime + epoch;
			int resourceIndex = beginResourceIndex;
			while (resourceIndex < resources.size() && resources.get(resourceIndex).getPickupTime() < epochEndTime) {
				if (resources.get(resourceIndex).getDropoffTime() < epochEndTime) {
					epochResources.add(resources.get(resourceIndex));
				}
				resourceIndex += 1;
			}

			if (dynamicTraffic) {
				if (epochResources.size() == 0) {
					// use the previous epoch if available
					trafficPattern.addTrafficPatternItem(epochBeginTime, lastKnownSpeedFactor);
				} else {
					double speedFactor = getSpeedFactor(epochResources);
					if (speedFactor < 0.0) { // didn't get a valid speed factor
						trafficPattern.addTrafficPatternItem(epochBeginTime, lastKnownSpeedFactor);
					} else {
						if (speedFactor > 1.0) { // cap speed factor to 1
							speedFactor = 1.0;
						}
						trafficPattern.addTrafficPatternItem(epochBeginTime, speedFactor);
						lastKnownSpeedFactor = speedFactor;
					}
				}
			} else {
				trafficPattern.addTrafficPatternItem(epochBeginTime, 1.0);
			}

			epochBeginTime += step;
			while (beginResourceIndex < resources.size() && resources.get(beginResourceIndex).getPickupTime() < epochBeginTime) {
				beginResourceIndex += 1;
			}

			if (resourceIndex == resources.size()) {
				break;
			}
		}
		return trafficPattern;
	}

	/**
	 * Compute speed factor from a set of resources. Speed factor is based on actual travel times
	 * compared to ideal travel time between picku and location based on distance.
	 * @param resources the set of resources that will determine the speed factor.
	 * @return speed factor
	 */
	public double getSpeedFactor(ArrayList<Resource> resources) {
		long totalActualTravelTime = 0;
		long totalSimulatedTravelTime = 0;
		for (Resource r : resources) {
			long pickupTime = r.getPickupTime();
			long dropoffTime = r.getDropoffTime();
			long actualTravelTime = dropoffTime - pickupTime;

			long simulatedTravelTime = map.travelTimeBetween(r.getPickupLocation(), r.getDropoffLocation());

			totalActualTravelTime += actualTravelTime;
			totalSimulatedTravelTime += simulatedTravelTime;
		}
		if (totalActualTravelTime == 0) {
			return -1.0;
		} else {
			return ((double) totalSimulatedTravelTime) / totalActualTravelTime;
		}
	}
}
