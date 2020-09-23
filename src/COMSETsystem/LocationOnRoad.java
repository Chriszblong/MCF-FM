package COMSETsystem;

import javax.xml.stream.Location;

public class LocationOnRoad {
    public Road road;
    private final double distanceFromStartIntersection;

    public LocationOnRoad(Road road, double distanceFromStartIntersection) {
        this.road = road;
        this.distanceFromStartIntersection = distanceFromStartIntersection;
    }

    public LocationOnRoad(LocationOnRoad locationOnRoad, double displacement) {
        this.road = locationOnRoad.road;
        this.distanceFromStartIntersection = locationOnRoad.distanceFromStartIntersection+ displacement;
        assert 0 <= this.distanceFromStartIntersection && this.distanceFromStartIntersection <= road.length;
    }

    public boolean upstreamTo(LocationOnRoad destination) {
        return getDisplacementOnRoad(destination) >= 0;
    }

    public double getDisplacementOnRoad(LocationOnRoad destination) {
        assert this.road.id == destination.road.id : "two links must be on the same road";
        double displacement = destination.distanceFromStartIntersection - this.distanceFromStartIntersection;
        return displacement;
    }

    public long getStaticTravelTimeOnRoad() {
        return Math.round(distanceFromStartIntersection/road.speed);
    }

    public boolean atEndIntersection() {
        return this.distanceFromStartIntersection == this.road.length;
    }

    // create location at the end of a road
    public static LocationOnRoad createFromRoadEnd(Road road) {
        return new LocationOnRoad(road, road.length);
    }

    // create location at the start of a road
    public static LocationOnRoad createFromRoadStart(Road road) {
        return new LocationOnRoad(road, 0);
    }

    public static LocationOnRoad copyWithReplacedRoad(Road road, LocationOnRoad locationOnRoad) {
        return new LocationOnRoad(road, locationOnRoad.distanceFromStartIntersection);
    }

    public String toString() {
        return "(road: " + road.id + ", distanceFromStartIntersection: " + distanceFromStartIntersection;
    }
}
