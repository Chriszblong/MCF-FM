package COMSETsystem;

import java.util.Set;

public abstract class FleetManager {

    protected final CityMap map;

    // Should not be accessible by subclasses.
    TrafficPattern trafficPattern;

    public enum ResourceState {
        AVAILABLE,
        PICKED_UP,
        DROPPED_OFF,
        EXPIRED
    }

    public abstract void onAgentIntroduced(long agentId, LocationOnRoad currentLoc, long time);

    public abstract AgentAction onResourceAvailabilityChange(Resource resource, ResourceState state,
                                                             LocationOnRoad currentLoc, long time);

    public abstract Intersection onReachIntersection(long agentId, long time, LocationOnRoad currentLoc);

    public abstract Intersection onReachIntersectionWithResource(long agentId, long time, LocationOnRoad currentLoc,
                                                                 Resource resource);

    public FleetManager(CityMap map) {
        this.map = map;
    }

    void setTrafficPattern(TrafficPattern trafficPattern) {
        this.trafficPattern = trafficPattern;
    }

    public LocationOnRoad getCurrentLocation(long lastAppearTime, LocationOnRoad lastLocation,
                                             long currentTime) {
        long elapseTime = currentTime - lastAppearTime;
        return trafficPattern.travelRoadForTime(lastAppearTime, lastLocation, elapseTime);
    }
}
