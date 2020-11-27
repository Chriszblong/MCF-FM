package UserExamples;

import COMSETsystem.*;
import com.google.ortools.graph.MinCostFlow;
import com.uber.h3core.H3Core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class MCFFleetManager extends FleetManager {
    private final Map<Long, Long> agentLastAppearTime = new HashMap<>();
    private final Map<Long, LocationOnRoad> agentLastLocation = new HashMap<>();
    private final Map<Long, Resource> resourceAssignment = new HashMap<>();
    private final Set<Resource> waitingResources = new TreeSet<>(Comparator.comparingLong((Resource r) -> r.id));
    private final Set<Long> availableAgent = new TreeSet<>(Comparator.comparingLong((Long id) -> id));
    private final Set<Long> occupiedAgent = new TreeSet<>(Comparator.comparingLong((Long id) -> id));
    private final Map<Long, Random> agentRnd = new HashMap<>();
    private final Map<Long, Resource> assignmentForOccupied = new HashMap<>();
    Map<Long, LinkedList<Intersection>> agentRoutes = new HashMap<>();

    TemporalUtils temporalUtils;
    TrafficPatternPred trafficPatternPred;
    H3Core h3;
    Map<Long, Long> agentStartSearchTime = new HashMap<>();
    List<Region> regionList = new ArrayList<>();
    Map<String, Integer> hexAddr2Region = new HashMap<>();
    // Candidate agents for repositioning task
    Set<Long> candidateAgents = new TreeSet<>(Comparator.comparingLong((Long id) -> id));
    boolean[] hasRepositioned;
    Map<Long, List<Integer>> intersectionResourceMap = new HashMap<>();

    static {
        System.loadLibrary("jniortools");
    }

    @Override
    public void onAgentIntroduced(long agentId, LocationOnRoad currentLoc, long time) {
        agentLastAppearTime.put(agentId, time);
        agentLastLocation.put(agentId, currentLoc);
        availableAgent.add(agentId);
        agentStartSearchTime.put(agentId, time);

        addAgentToRegion(agentId, currentLoc);
    }


    @Override
    public AgentAction onResourceAvailabilityChange(Resource resource, ResourceState state, LocationOnRoad currentLoc, long time) {
        AgentAction action = AgentAction.doNothing();

        if (state == ResourceState.AVAILABLE) {
            Long assignedAgent = getNearestAgent(resource, time);
//            Long assignedAgent = getNearestAvailableAgent(resource, time);
            if (assignedAgent != null) {
                resourceAssignment.put(assignedAgent, resource);
                agentRoutes.put(assignedAgent, new LinkedList<>());
                availableAgent.remove(assignedAgent);
                candidateAgents.remove(assignedAgent);
                action = AgentAction.assignTo(assignedAgent, resource.id);

                // remove from region
                removeAgentFromRegion(assignedAgent);
            } else {
                waitingResources.add(resource);
                addResourceToRegion(resource);
            }
        } else if (state == ResourceState.DROPPED_OFF) {
            Resource bestResource =  null;
            long earliest = Long.MAX_VALUE;
            if(assignmentForOccupied.containsKey(resource.assignedAgentId)){
                bestResource = assignmentForOccupied.get(resource.assignedAgentId);
            }
            else {
                for (Resource res : waitingResources) {
                    // If res is in waitingResources, then it must have not expired yet
                    // testing null pointer exception

                    if(assignmentForOccupied.values().contains(res))
                        continue;

                    long travelTime = map.travelTimeBetween(currentLoc, res.pickupLoc);

                    double speedFactor = getSpeedFactor(time);
                    long adjustedTravelTime = (long) (travelTime / speedFactor);

                    // if the resource is reachable before expiration
                    long arriveTime = time + adjustedTravelTime;
                    if (arriveTime <= res.expirationTime && arriveTime < earliest) {
                        earliest = arriveTime;
                        bestResource = res;
                    }
                }
            }


            if (bestResource != null) {
                waitingResources.remove(bestResource);
                removeResourceFromRegion(bestResource);
                action = AgentAction.assignTo(resource.assignedAgentId, bestResource.id);
            } else {
                availableAgent.add(resource.assignedAgentId);
                addAgentToRegion(resource.assignedAgentId, currentLoc);
                agentStartSearchTime.put(resource.assignedAgentId, time);
                action = AgentAction.doNothing();
            }
            assignmentForOccupied.remove(resource.assignedAgentId);
            occupiedAgent.remove(resource.assignedAgentId);
            resourceAssignment.put(resource.assignedAgentId, bestResource);
            agentLastLocation.put(resource.assignedAgentId, currentLoc);
            agentLastAppearTime.put(resource.assignedAgentId, time);
        } else if (state == ResourceState.EXPIRED) {
            waitingResources.remove(resource);
            removeResourceFromRegion(resource);
            if (resource.assignedAgentId != -1) {
                agentRoutes.put(resource.assignedAgentId, new LinkedList<>());
                availableAgent.add(resource.assignedAgentId);
                addAgentToRegion(resource.assignedAgentId, currentLoc);
                agentStartSearchTime.put(resource.assignedAgentId, time);
                resourceAssignment.remove(resource.assignedAgentId);
            }
        } else if (state == ResourceState.PICKED_UP) {
            agentRoutes.put(resource.assignedAgentId, new LinkedList<>());
            occupiedAgent.add(resource.assignedAgentId);
        }

        return action;
    }

    @Override
    public Intersection onReachIntersection(long agentId, long time, LocationOnRoad currentLoc) {
        if (agentId == 240902L && time == 1464800008L) {
            System.out.println("here");
        }

        LocationOnRoad lastLoc = agentLastLocation.get(agentId);
        String lastAddr = h3.geoToH3Address(getLocationLatLon(lastLoc)[0], getLocationLatLon(lastLoc)[1], 8);
        String currentAddr = h3.geoToH3Address(getLocationLatLon(currentLoc)[0], getLocationLatLon(currentLoc)[1], 8);
        if(!lastAddr.equals(currentAddr) && availableAgent.contains(agentId)){
            removeAgentFromRegion(agentId);
            if (availableAgent.contains(agentId)) {
                addAgentToRegion(agentId, currentLoc);
            }
        }
        agentLastAppearTime.put(agentId, time);

        int timeIndex = temporalUtils.findTimeIntervalIndex(time);
        int searchedTime = (int)((time - agentStartSearchTime.get(agentId)) / Configuration.timeResolution);
        if(searchedTime > GlobalParameters.cruising_threshold && availableAgent.contains(agentId))
            candidateAgents.add(agentId);
        if(candidateAgents.size() > 0 && !hasRepositioned[timeIndex]){
            driverReposition(time);
            hasRepositioned[timeIndex] = true;
        }

        LinkedList<Intersection> route = agentRoutes.getOrDefault(agentId, new LinkedList<>());
        if (route.isEmpty()) {
            route = planRoute(agentId, currentLoc, time);
            agentRoutes.put(agentId, route);
        }

        Intersection nextLocation = route.poll();
        Road nextRoad = currentLoc.road.to.roadTo(nextLocation);
        LocationOnRoad locationOnRoad = LocationOnRoad.createFromRoadStart(nextRoad);
        agentLastLocation.put(agentId, locationOnRoad);
        return nextLocation;
    }

    @Override
    public Intersection onReachIntersectionWithResource(long agentId, long time, LocationOnRoad currentLoc, Resource resource) {
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

    public MCFFleetManager(CityMap map) {
        super(map);

        temporalUtils = new TemporalUtils(map.computeZoneId());
        trafficPatternPred = new TrafficPatternPred(GlobalParameters.traffic_pattern_pred_file);
        hasRepositioned = new boolean[temporalUtils.numOfTimeInterval];
        try{
            h3 = H3Core.newInstance();
        }catch (IOException ex){
            ex.printStackTrace();
        }

        readRegionFile(GlobalParameters.region_file);
        readPickupMatrix(GlobalParameters.pickup_pred_file);
        readDropoffMatrix(GlobalParameters.dropoff_pred_file);
        readIntersectionResourceFile(GlobalParameters.intersectionResourceFile);
    }

    LinkedList<Intersection> planRoute(long agentId, LocationOnRoad currentLocation, long time) {
        Resource assignedRes = resourceAssignment.get(agentId);

        if (assignedRes != null) {
            Intersection sourceIntersection = currentLocation.road.to;
            Intersection destinationIntersection = assignedRes.pickupLoc.road.from;
            LinkedList<Intersection> shortestTravelTimePath = map.shortestTravelTimePath(sourceIntersection,
                    destinationIntersection);
            shortestTravelTimePath.poll(); // Ensure that route.get(0) != currentLocation.road.to.
            return shortestTravelTimePath;
        } else {
            return getSTPRoute(agentId, currentLocation, time);
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

    /**
     * Call this method to find a search route for an idle agent
     * @param agentId the unique id of the agent
     * @param currentLocation current location of agent
     * @param time current simulation time
     * @return the search route for idle agent
     */
    LinkedList<Intersection> getSTPRoute(long agentId, LocationOnRoad currentLocation, long time) {
        Intersection sourceIntersection = currentLocation.road.to;

        Random rnd = agentRnd.getOrDefault(agentId, new Random(agentId));
        agentRnd.put(agentId, rnd);

        Set<Region> candidateRegion = new HashSet<>();
        List<Region> kNeigbors = getKNeighborRegions(getRegion(sourceIntersection), GlobalParameters.k);

        while (candidateRegion.size() < GlobalParameters.n){
            Region region = sampleARegion(kNeigbors, time, rnd);
            candidateRegion.add(region);
            kNeigbors.remove(region);
        }

        double speedFactor = getSpeedFactor(time);
        Region destinationRegion = sampleByDistance(candidateRegion, currentLocation, speedFactor,
                GlobalParameters.gamma,
                rnd);

        Intersection destinationIntersection = getDestination(destinationRegion, time);
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

    // Convert location to latitude and longtitude
    private double[] getLocationLatLon(LocationOnRoad location){
        double[] latLon = new double[2];
        double proportion = ((double) location.getStaticTravelTimeOnRoad()) / location.road.travelTime;

        if(proportion < 0)
            proportion = 0;
        if(proportion > 1)
            proportion = 1;

        latLon[0] =
                location.road.from.latitude + (location.road.to.latitude - location.road.from.latitude) * proportion;
        latLon[1] =
                location.road.from.longitude + (location.road.to.longitude - location.road.from.longitude) * proportion;
        return latLon;
    }

    private void removeResourceFromRegion(Resource resource){
        double[] latLon = getLocationLatLon(resource.pickupLoc);
        String hexAddr = h3.geoToH3Address(latLon[0], latLon[1], 8);
        if(hexAddr2Region.containsKey(hexAddr)){
            regionList.get(hexAddr2Region.get(hexAddr)).waitingResources.remove(resource);
        }
    }

    private void addAgentToRegion(Long agentId, LocationOnRoad currentLoc){
        double[] latLon = getLocationLatLon(currentLoc);
        String hexAddr = h3.geoToH3Address(latLon[0],
                latLon[1], 8);
        if(hexAddr2Region.containsKey(hexAddr)){
            regionList.get(hexAddr2Region.get(hexAddr)).availableAgents.add(agentId);
        }
    }


    private void removeAgentFromRegion(Long agentId){
        double[] latLon = getLocationLatLon(agentLastLocation.get(agentId));
        String hexAddr = h3.geoToH3Address(latLon[0], latLon[1], 8);
        if(hexAddr2Region.containsKey(hexAddr)){
            regionList.get(hexAddr2Region.get(hexAddr)).availableAgents.remove(agentId);
        }
    }

    private void addResourceToRegion(Resource resource){
        double[] latLon = getLocationLatLon(resource.pickupLoc);
        String hexAddr = h3.geoToH3Address(latLon[0], latLon[1], 8);
        if(hexAddr2Region.containsKey(hexAddr)){
            regionList.get(hexAddr2Region.get(hexAddr)).waitingResources.add(resource);
        }
    }

    private void readRegionFile(String fileName){
        try{
            File file = new File(fileName);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String tempString = null;
            while((tempString = reader.readLine()) != null){
                Region region = new Region(tempString);
                regionList.add(region);
                hexAddr2Region.put(tempString, regionList.size()-1);
            }
            reader.close();
        }catch (Exception ex){
            ex.printStackTrace();
        }

        for(Intersection i : map.intersections().values()){
            double lat = i.latitude;
            double lng = i.longitude;
            String hexAddr = h3.geoToH3Address(lat, lng, 8);
            regionList.get(hexAddr2Region.get(hexAddr)).intersectionList.add(i);
            List<Integer> intersectionResource = new ArrayList<>();
            intersectionResourceMap.put(i.id, intersectionResource);
        }
    }

    private void readPickupMatrix(String fileName){
        try{
            File file = new File(fileName);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String tmp = null;
            while ((tmp = reader.readLine()) != null){
                String[] regionData = tmp.split(",");
                for(int i=0; i<regionData.length; i++){
                    regionList.get(i).resourceQuantity.add((int)Double.parseDouble(regionData[i]));
                }
            }
            reader.close();
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    private void readDropoffMatrix(String fileName){
        try{
            File file = new File(fileName);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String tmp = null;
            while ((tmp = reader.readLine()) != null){
                String[] regionData = tmp.split(",");
                for(int i=0; i<regionData.length; i++){
                    regionList.get(i).destinationQuantity.add((int)Double.parseDouble(regionData[i]));
                }
            }
            reader.close();
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    private void readIntersectionResourceFile(String fileName){
        try{
            File file = new File(fileName);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String tmp = null;
            List<Intersection> intersectionList = new ArrayList<>(map.intersections().values());
            while ((tmp = reader.readLine()) != null){
                String[] regionData = tmp.split(",");
                for(int i=0; i<regionData.length; i++){
                    intersectionResourceMap.get(intersectionList.get(i).id).add((int)Double.parseDouble(regionData[i]));
                }
            }
            reader.close();
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    /**
     * Call this method to get the predicted speed factor of current time
     * @param time current simulation time
     * @return the predicted speed factor
     */
    private double getSpeedFactor(long time){
        int index = temporalUtils.findTimeIntervalIndex(time);
        return trafficPatternPred.getSpeedFactor(index);
    }

    // To get the estimated travel time using the predicted speed factor
    private long getTravelTimeBetween(LocationOnRoad source, LocationOnRoad destination, long time){
        long travelTime = map.travelTimeBetween(source, destination);
        return (long) (travelTime / getSpeedFactor(time));
    }

    private long getTravelTimeBetween(Intersection source, Intersection destination, long time){
        double travelTime = map.travelTimeBetween(source, destination);
        return (long) (travelTime / getSpeedFactor(time));
    }

    private long getTravelTimeBetween(LocationOnRoad source, Intersection destination, long time){
        double travelTime =
                map.travelTimeBetween(source.road.to, destination) + source.road.travelTime - source.getStaticTravelTimeOnRoad();
        return (long) (travelTime / getSpeedFactor(time));
    }

    // return the region of intersection
    public Region getRegion(Intersection intersection){
        String hexAddr = h3.geoToH3Address(intersection.latitude, intersection.longitude, 8);
        if(hexAddr2Region.containsKey(hexAddr))
            return regionList.get(hexAddr2Region.get(hexAddr));
        return null;
    }

    // Reposition all candidate agents together
    private void driverReposition(long time){
        //Solve the minimum flow problem to get the optimal assignments
        Map<Long, Integer> agentNodeMap = new HashMap<>();
        Map<Region, Integer> regionNodeMap = new HashMap<>();
        Map<Integer, Long> nodeAgentMap = new HashMap<>();
        Map<Integer, Region> nodeRegionMap = new HashMap<>();
        Map<Long, Set<Region>> agentDestinations = new HashMap<>();
        Set<Region> candidateRegions = new HashSet<>();

        for(Long agent : candidateAgents){
            List<Region> regions = new ArrayList<>(regionList);
            Set<Region> regionSet = new HashSet<>();
            Random rnd = agentRnd.get(agent);
            while (regionSet.size() < GlobalParameters.n){
                Region region = sampleARegion(regions, time, rnd);
                regionSet.add(region);
                candidateRegions.add(region);
                regions.remove(region);
            }
            agentDestinations.put(agent, regionSet);
        }

        List<Integer> start_nodes = new ArrayList<>();
        List<Integer> end_nodes = new ArrayList<>();
        List<Long> capacities = new ArrayList<>();
        List<Long> costs = new ArrayList<>();
        int source = 0;
        int sink = candidateAgents.size() + candidateRegions.size() + 1;
        long[] supplies = new long[sink + 1];


        int k = 1;
        for(Long agent : candidateAgents){
            agentNodeMap.put(agent, k);
            nodeAgentMap.put(k, agent);
            k++;
        }
        for(Region region : candidateRegions){
            regionNodeMap.put(region, k);
            nodeRegionMap.put(k,region);
            k++;
        }

        for(Long agent : candidateAgents){
            start_nodes.add(source);
            end_nodes.add(agentNodeMap.get(agent));
            capacities.add(1L);
            costs.add(0L);
        }

        for(Long agent : candidateAgents){
            Set<Region> regionSet = agentDestinations.get(agent);
            for(Region region : regionSet){
                start_nodes.add(agentNodeMap.get(agent));
                end_nodes.add(regionNodeMap.get(region));
                capacities.add(1L);
                costs.add(getCost(agent, region, time));
            }
        }

        Map<Region, Long> regionCapacitiyMap = calRegionsCapacities(candidateRegions, candidateAgents.size(), time);

        for(Region region : candidateRegions){
            start_nodes.add(regionNodeMap.get(region));
            end_nodes.add(sink);
            capacities.add(regionCapacitiyMap.get(region));
            costs.add(0L);
        }

        for(int i=source; i<=sink; i++){
            supplies[i] = 0;
            if(i==source)
                supplies[i] = candidateAgents.size();
            if(i==sink)
                supplies[i] = -candidateAgents.size();
        }

        MinCostFlow min_cost_flow = new MinCostFlow();
        for(int i=0; i<start_nodes.size(); i++){
            min_cost_flow.addArcWithCapacityAndUnitCost(start_nodes.get(i), end_nodes.get(i), capacities.get(i),
                    costs.get(i));
        }
        for(int i=0; i<supplies.length; i++){
            min_cost_flow.setNodeSupply(i, supplies[i]);
        }

        if(min_cost_flow.solve() == MinCostFlow.Status.OPTIMAL){
            for(int i=0; i<min_cost_flow.getNumArcs(); i++){
                if(min_cost_flow.getTail(i)!=source && min_cost_flow.getHead(i)!=sink){
                    if(min_cost_flow.getFlow(i) > 0){
                        Long agent = nodeAgentMap.get(min_cost_flow.getTail(i));
                        Region region = nodeRegionMap.get(min_cost_flow.getHead(i));
                        guideAgentToRegion(agent, region, time);
                        agentStartSearchTime.put(agent, time);
                    }
                }
            }
        }

        candidateAgents.clear();
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
            long travelTime = getTravelTimeBetween(curLoc, resource.pickupLoc, currentTime);
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

    // Get the nearest agent for resource from all available agents and occupied agents
    private Long getNearestAgent(Resource resource, long currentTime){
        long availableEarliest = Long.MAX_VALUE;
        Long availableBestAgent = null;
        long occupiedEarliest = Long.MAX_VALUE;
        Long occupiedBestAgent = null;
        for (Long id : availableAgent) {
            if (!agentLastLocation.containsKey(id)) continue;

            LocationOnRoad curLoc = getCurrentLocation(
                    agentLastAppearTime.get(id),
                    agentLastLocation.get(id),
                    currentTime);

            long travelTime = getTravelTimeBetween(curLoc, resource.pickupLoc, currentTime);
            long arriveTime = travelTime + currentTime;

            if (arriveTime < availableEarliest) {
                availableBestAgent = id;
                availableEarliest = arriveTime;
            }
        }

        for(Long id : occupiedAgent){
            if(!agentLastLocation.containsKey(id)) continue;

            LocationOnRoad curLoc = getCurrentLocation(agentLastAppearTime.get(id), agentLastLocation.get(id),
                    currentTime);
            Resource assignedResource = resourceAssignment.get(id);
            if(assignedResource != null){
                long dropoffTime = getTravelTimeBetween(curLoc, assignedResource.dropOffLoc, currentTime);
                long approachTime = getTravelTimeBetween(assignedResource.dropOffLoc, resource.pickupLoc,
                        currentTime + dropoffTime);
                long arriveTime = dropoffTime + approachTime + currentTime;

                if(arriveTime < occupiedEarliest){
                    occupiedBestAgent = id;
                    occupiedEarliest = arriveTime;
                }
            }
        }

        if(availableEarliest <= occupiedEarliest && availableEarliest <= resource.expirationTime){
            return availableBestAgent;
        }
        else if(availableEarliest > occupiedEarliest && occupiedEarliest <= resource.expirationTime){
            assignmentForOccupied.put(occupiedBestAgent, resource);
            occupiedAgent.remove(occupiedBestAgent);
            return null;
        }else {
            return null;
        }
    }

    private List<Region> getKNeighborRegions(Region region, int k){
        List<Region> kRegions = new ArrayList<>();
        List<String> neigborStr = h3.kRing(region.hexAddr, k);
        for(int i=0; i<neigborStr.size(); i++){
            if(hexAddr2Region.containsKey(neigborStr.get(i))){
                kRegions.add(regionList.get(hexAddr2Region.get(neigborStr.get(i))));
            }
        }
        return kRegions;
    }

    private Region sampleARegion(List<Region> regions, long currentTime, Random rnd){
        // choose a region according to probability
        int size = regions.size();
        double[] weightArray = new double[size];
        double[] cumulativeProbs = new double[size];
        double cumulativeWeight = 0.0;
        double sumWeight = 0.0;
        for(int i = 0; i < size; i++){
            double weight = getRegionWeight(regions.get(i), currentTime);
            sumWeight += weight;
            weightArray[i] = weight;
        }

        for(int i = 0; i < size; i++){
            cumulativeWeight += weightArray[i];
            cumulativeProbs[i] = cumulativeWeight / sumWeight;
        }
        int index = sampleIndex(cumulativeProbs, rnd);
        return regions.get(index);
    }

    private Region sampleByDistance(Set<Region> candidateRegions, LocationOnRoad currentLocation,
                                    double speedFactor, double gamma,
                                    Random rnd){
        Region[] regions = candidateRegions.toArray(new Region[candidateRegions.size()]);
        double[] distArray = new double[candidateRegions.size()];
        double[] cumulativeProbs = new double[candidateRegions.size()];
        double cumulativeDist = 0.0, sumDist = 0.0;
        for(int i = 0; i < regions.length; i++){
            Intersection des = regions[i].intersectionList.get(rnd.nextInt(regions[i].intersectionList.size()));

            double dist =
                    map.travelTimeBetween(currentLocation.road.to, des) / speedFactor / Configuration.timeResolution * regions[i].availableAgents.size();

            distArray[i] = Math.pow(dist, gamma);
            sumDist += distArray[i];
        }
        for(int i = 0; i < regions.length; i++){
            cumulativeDist += distArray[i];
            cumulativeProbs[i] = cumulativeDist / sumDist;
        }

        int index = sampleIndex(cumulativeProbs, rnd);
        return regions[index];
    }

    private double getRegionWeight(Region region, long time){
        int timeIndex = temporalUtils.findTimeIntervalIndex(time);
        int k = GlobalParameters.timeHorizon / GlobalParameters.timeInterval;
        double weight = 0.0;
        for(int i = timeIndex; i < timeIndex + k; i++){
            if(i < region.resourceQuantity.size())
                weight += Math.pow(0.8, i - timeIndex) * (region.resourceQuantity.get(i) - GlobalParameters.lambda * region
                        .destinationQuantity.get(i));
//                weight += region.resourceQuantity.get(i) - GlobalParameters.lambda * region
//                        .destinationQuantity.get(i);
        }
        if(weight < 0)
            weight = 0.0;
        return weight;
    }

    private int sampleIndex(double[] cumulativeProbs, Random rnd){
        final double randomValue = rnd.nextDouble();
        int index = Arrays.binarySearch(cumulativeProbs, randomValue);
        if (index < 0) {
            index = -index - 1;
        }
        if (index >= 0 && index < cumulativeProbs.length && randomValue < cumulativeProbs[index]) {
            return index;
        }
        return cumulativeProbs.length - 1;
    }

    private long getCost(long agent, Region region, long time){
        LocationOnRoad curLoc = getCurrentLocation(agentLastAppearTime.get(agent), agentLastLocation.get(agent), time);
        Intersection destination = getDestination(region, time);
        long travelTime = getTravelTimeBetween(curLoc, destination, time);
        int timeIndex = temporalUtils.findTimeIntervalIndex(time);
        int k = GlobalParameters.timeHorizon / GlobalParameters.timeInterval;
        long resourceNum = 0L;
        for(int i=timeIndex; i<timeIndex+k; i++){
            if(i < region.resourceQuantity.size())
                resourceNum += region.resourceQuantity.get(i);
        }
        if(resourceNum == 0)
            return Long.MAX_VALUE;
        return travelTime / resourceNum;
    }

    private Map<Region, Long> calRegionsCapacities(Set<Region> candidateRegions, int numOfAgent, long time){
        Map<Region, Long> regionCapacitityMap = new HashMap<>();
        double sumWeight = 0.0;
        for(Region region : candidateRegions){
            sumWeight += getRegionWeight(region, time);
        }
        for(Region region : candidateRegions){
            double weight = getRegionWeight(region, time);
            long capacity = Math.round(weight / sumWeight * numOfAgent);
            regionCapacitityMap.put(region, capacity);
        }
        return regionCapacitityMap;
    }

    private void guideAgentToRegion(Long agent, Region region, long time){
        LocationOnRoad curLoc = getCurrentLocation(agentLastAppearTime.get(agent), agentLastLocation.get(agent), time);
        Intersection sourceIntersection = curLoc.road.to;
        Intersection destinationIntersection = getDestination(region, time);

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
        agentRoutes.put(agent, shortestTravelTimePath);
    }

    private Intersection getDestination(Region region, long time){
        Intersection bestIntersection = region.intersectionList.get(0);
        Map<Intersection, Integer> intersectionAgentNum = new HashMap<>();
        for(int i=0; i<region.intersectionList.size(); i++){
            intersectionAgentNum.put(region.intersectionList.get(i), 0);
        }
        for(Long agent : region.availableAgents){
            LocationOnRoad curLoc = getCurrentLocation(agentLastAppearTime.get(agent), agentLastLocation.get(agent),
                    time);
            if(intersectionAgentNum.containsKey(curLoc.road.to)){
                intersectionAgentNum.put(curLoc.road.to, intersectionAgentNum.get(curLoc.road.to) + 1);
            }
        }

        double misMatch = Double.MAX_VALUE;
        int intersectionTimeIndex = temporalUtils.getIntersectionTemporalIndex(time);
        int resourceSum = 0;
        for(Intersection i : region.intersectionList){
            resourceSum += intersectionResourceMap.get(i.id).get(intersectionTimeIndex);
        }

        for(int i=0; i<region.intersectionList.size(); i++){
            int agentNum = intersectionAgentNum.get(region.intersectionList.get(i));
            int resourceNum = intersectionResourceMap.get(region.intersectionList.get(i).id).get(intersectionTimeIndex);
            double tmp = agentNum / (double)region.availableAgents.size() - resourceNum / (double) resourceSum;
            if(tmp < misMatch){
                bestIntersection = region.intersectionList.get(i);
                misMatch = tmp;
            }
        }
        return bestIntersection;
    }
}
