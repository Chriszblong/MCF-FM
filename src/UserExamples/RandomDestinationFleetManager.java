package UserExamples;

import COMSETsystem.*;

import java.util.*;

public class RandomDestinationFleetManager extends FleetManager {
    private final Map<Long, Long> agentLastAppearTime = new HashMap<>();
    private final Map<Long, LocationOnRoad> agentLastLocation = new HashMap<>();
    private final Map<Long, Resource> resourceAssignment = new HashMap<>();
    private final Set<Resource> waitingResources = new TreeSet<>(Comparator.comparingLong((Resource r) -> r.id));
    private final Set<Long> availableAgent = new TreeSet<>(Comparator.comparingLong((Long id) -> id));
    private final Map<Long, Random> agentRnd = new HashMap<>();


    Map<Long, LinkedList<Intersection>> agentRoutes = new HashMap<>();

    /**
     * The simulation calls onAgentIntroduced to notify the **FleetManager** that a new agent has been randomly
     * placed and is available for assignment.
     * @param agentId a unique id for each agent and can be used to associated information with agents.
     * @param currentLoc the current location of the agent.
     * @param time the simulation time.
     */
    @Override
    public void onAgentIntroduced(long agentId, LocationOnRoad currentLoc, long time) {
        agentLastAppearTime.put(agentId, time);
        agentLastLocation.put(agentId, currentLoc);
        availableAgent.add(agentId);
    }

    /**
     * The simulation calls this method to notify the **FleetManager** that the resource's state has changed:
     * + resource becomes available for pickup
     * + resource expired
     * + resource has been dropped off by its assigned agent
     * + resource has been picked up by an agent.
     * @param resource This object contains information about the Resource useful to the fleet manager
     * @param state the new state of the resource
     * @param currentLoc current location of the resources
     * @param time the simulation time
     * @return AgentAction that tells the agents what to do.
     */
    @Override
    public AgentAction onResourceAvailabilityChange(Resource resource,
                                                    ResourceState state,
                                                    LocationOnRoad currentLoc,
                                                    long time) {

        AgentAction action = AgentAction.doNothing();

        if (state == ResourceState.AVAILABLE) {
            Long assignedAgent = getNearestAvailableAgent(resource, time);
            if (assignedAgent != null) {
                resourceAssignment.put(assignedAgent, resource);
                agentRoutes.put(assignedAgent, new LinkedList<>());
                availableAgent.remove(assignedAgent);
                action = AgentAction.assignTo(assignedAgent, resource.id);
            } else {
                waitingResources.add(resource);
            }
        } else if (state == ResourceState.DROPPED_OFF) {
            Resource bestResource =  null;
            long earliest = Long.MAX_VALUE;
            for (Resource res : waitingResources) {
                // If res is in waitingResources, then it must have not expired yet
                // testing null pointer exception
                // Warning: map.travelTimeBetween returns the travel time based on speed limits, not
                // the dynamic travel time. Thus the travel time returned by map.travelTimeBetween may be different
                // than the actual travel time.
                long travelTime = map.travelTimeBetween(currentLoc, res.pickupLoc);

                // if the resource is reachable before expiration
                long arriveTime = time + travelTime;
                if (arriveTime <= res.expirationTime && arriveTime < earliest) {
                    earliest = arriveTime;
                    bestResource = res;
                }

            }

            if (bestResource != null) {
                waitingResources.remove(bestResource);
                action = AgentAction.assignTo(resource.assignedAgentId, bestResource.id);
            } else {
                availableAgent.add(resource.assignedAgentId);
                action = AgentAction.doNothing();
            }
            resourceAssignment.put(resource.assignedAgentId, bestResource);
            agentLastLocation.put(resource.assignedAgentId, currentLoc);
            agentLastAppearTime.put(resource.assignedAgentId, time);
        } else if (state == ResourceState.EXPIRED) {
            waitingResources.remove(resource);
            if (resource.assignedAgentId != -1) {
                agentRoutes.put(resource.assignedAgentId, new LinkedList<>());
                availableAgent.add(resource.assignedAgentId);
                resourceAssignment.remove(resource.assignedAgentId);
            }
        } else if (state == ResourceState.PICKED_UP) {
            agentRoutes.put(resource.assignedAgentId, new LinkedList<>());
        }

        return action;
    }

    /**
     * Calls to this method notifies that an agent has reach an intersection and is ready for new travel directions.
     * This is called whenever any agent without an assigned resources reaches an intersection. This method allows
     * the **FleetManager** to plan any agent's cruising path, the path it takes when it has no assigned resource.
     * The intention is that the **FleetManager** will plan the cruising, to minimize the time it takes to
     * reach resources for pickup.
     * @param agentId unique id of the agent
     * @param time current simulation time.
     * @param currentLoc current location of the agent.
     * @return the next intersection for the agent to navigate to.
     */
    @Override
    public Intersection onReachIntersection(long agentId, long time, LocationOnRoad currentLoc) {
        if (agentId == 240902L && time == 1464800008L) {
            System.out.println("here");
        }
        agentLastAppearTime.put(agentId, time);

        LinkedList<Intersection> route = agentRoutes.getOrDefault(agentId, new LinkedList<>());

        if (route.isEmpty()) {
            route = planRoute(agentId, currentLoc);
            agentRoutes.put(agentId, route);
        }

        Intersection nextLocation = route.poll();
        Road nextRoad = currentLoc.road.to.roadTo(nextLocation);
        LocationOnRoad locationOnRoad = LocationOnRoad.createFromRoadStart(nextRoad);
        agentLastLocation.put(agentId, locationOnRoad);
        return nextLocation;
    }

    /**
     * Calls to this method notifies that an agent with an picked up resource reaches an intersection.
     * This method allows the **FleetMangaer** to plan the route of the agent to the resource's dropoff point.
     * @param agentId the unique id of the agent
     * @param time current simulation time
     * @param currentLoc current location of agent
     * @param resource information of the resource associated with the agent.
     * @return the next intersection for the agent to navigate to.
     */
    @Override
    public Intersection onReachIntersectionWithResource(long agentId, long time, LocationOnRoad currentLoc,
                                                        Resource resource) {
        agentLastAppearTime.put(agentId, time);

        LinkedList<Intersection> route = agentRoutes.getOrDefault(agentId, new LinkedList<>());

        if (route.isEmpty()) {
            route = planRouteToTarget(resource.pickupLoc, resource.dropOffLoc);
            agentRoutes.put(agentId, route);
        }

        Intersection nextLocation = route.poll();
        Road nextRoad = currentLoc.road.to.roadTo(nextLocation);
        LocationOnRoad locationOnRoad = LocationOnRoad.createFromRoadStart(nextRoad);
        agentLastLocation.put(agentId, locationOnRoad);
        return nextLocation;
    }

    Long getNearestAvailableAgent(Resource resource, long currentTime) {
        long earliest = Long.MAX_VALUE;
        Long bestAgent = null;
        for (Long id : availableAgent) {
            if (!agentLastLocation.containsKey(id)) continue;

            LocationOnRoad curLoc = getCurrentLocation(
                    agentLastAppearTime.get(id),
                    agentLastLocation.get(id),
                    currentTime);
            // Warning: map.travelTimeBetween returns the travel time based on speed limits, not
            // the dynamic travel time. Thus the travel time returned by map.travelTimeBetween may be different
            // than the actual travel time.
            long travelTime = map.travelTimeBetween(curLoc, resource.pickupLoc);
            long arriveTime = travelTime + currentTime;
            if (arriveTime < earliest) {
                bestAgent = id;
                earliest = arriveTime;
            }
        }

        if (earliest <= resource.expirationTime) {
            return bestAgent;
        } else {
            return null;
        }
    }

    LinkedList<Intersection> planRoute(long agentId, LocationOnRoad currentLocation) {
        Resource assignedRes = resourceAssignment.get(agentId);

        if (assignedRes != null) {
            Intersection sourceIntersection = currentLocation.road.to;
            Intersection destinationIntersection = assignedRes.pickupLoc.road.from;
            LinkedList<Intersection> shortestTravelTimePath = map.shortestTravelTimePath(sourceIntersection,
                    destinationIntersection);
            shortestTravelTimePath.poll(); // Ensure that route.get(0) != currentLocation.road.to.
            return shortestTravelTimePath;
        } else {
            return getRandomRoute(agentId, currentLocation);
        }
    }

    LinkedList<Intersection> planRouteToTarget(LocationOnRoad source, LocationOnRoad destination) {
        Intersection sourceIntersection = source.road.to;
        Intersection destinationIntersection = destination.road.from;
        LinkedList<Intersection> shortestTravelTimePath = map.shortestTravelTimePath(sourceIntersection,
                destinationIntersection);
        shortestTravelTimePath.poll(); // Ensure that route.get(0) != currentLocation.road.to.
        return shortestTravelTimePath;
    }

    LinkedList<Intersection> getRandomRoute(long agentId, LocationOnRoad currentLocation) {
        Random random = agentRnd.getOrDefault(agentId, new Random(agentId));
        agentRnd.put(agentId, random);

        Intersection sourceIntersection = currentLocation.road.to;
        int destinationIndex = random.nextInt(map.intersections().size());
        Intersection[] intersectionArray =
                map.intersections().values().toArray(new Intersection[0]);
       Intersection destinationIntersection = intersectionArray[destinationIndex];
        if (destinationIntersection == sourceIntersection) {
            // destination cannot be the source
            // if destination is the source, choose a neighbor to be the destination
            Road[] roadsFrom =
                    sourceIntersection.roadsMapFrom.values().toArray(new Road[0]);
            destinationIntersection = roadsFrom[0].to;
        }
        LinkedList<Intersection> shortestTravelTimePath = map.shortestTravelTimePath(sourceIntersection,
                destinationIntersection);
        shortestTravelTimePath.poll(); // Ensure that route.get(0) != currentLocation.road.to.
        return shortestTravelTimePath;
    }

    public RandomDestinationFleetManager(CityMap map) {
        super(map);
    }
}
