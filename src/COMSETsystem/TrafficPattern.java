package COMSETsystem;

import java.util.ArrayList;

/**
 * TrafficPattern is a data structure that represents how the traffic condition changes over the time
 * of a day.
 */
public class TrafficPattern {
    public long step;
    private long firstEpochBeginTime;
    private long lastEpochBeginTime;
    private double firstEpochSpeedFactor;
    private double lastEpochSpeedFactor;

    private final ArrayList<TrafficPatternItem> trafficPattern;

    public TrafficPattern(long step) {
        this.step = step;
        this.trafficPattern = new ArrayList<TrafficPatternItem>();
    }

    public void addTrafficPatternItem(long epochBeginTime, double speedFactor) {
        TrafficPatternItem trafficPatternItem = new TrafficPatternItem(epochBeginTime, speedFactor);
        trafficPattern.add(trafficPatternItem);
        if (trafficPattern.size() == 1) {
            firstEpochBeginTime = epochBeginTime;
            firstEpochSpeedFactor = speedFactor;
        }
        lastEpochBeginTime = epochBeginTime;
        lastEpochSpeedFactor = speedFactor;
    }

    class TrafficPatternItem {
        public long epochBeginTime;
        public double speed_factor;
        public TrafficPatternItem(long epochBeginTime, double speed_factor) {
            this.epochBeginTime = epochBeginTime;
            this.speed_factor = speed_factor;
        }
        public String toString() {
            return this.epochBeginTime +","+ this.speed_factor;
        }
    }

    double getSpeedFactor(long time) {
        if (time < this.firstEpochBeginTime) {
            return this.firstEpochSpeedFactor;
        }
        if (time >= this.lastEpochBeginTime) {
            return this.lastEpochSpeedFactor;
        }
        int patternIndex = (int) ((time - this.firstEpochBeginTime) / step);
        return this.trafficPattern.get(patternIndex).speed_factor;
    }

    // compute the dynamic travel time to travel a certain distance of a link starting at a certain time
    public double dynamicForwardTravelTime(double time, double unadjustedSpeed, double distance) {

        double totalDistance = 0.0;
        double totalTime = 0.0;
        double currentTime = time;

        double speedFactor;
        double adjustedSpeed;

        while (true) {
            double stepTime = -1.0;
            // travel for one step
            if (currentTime >= this.lastEpochBeginTime) {
                speedFactor = this.lastEpochSpeedFactor;
                adjustedSpeed = unadjustedSpeed * speedFactor;
                stepTime = (distance - totalDistance) / adjustedSpeed;
                totalTime += stepTime;
                break;
            } else {
                if (currentTime < this.firstEpochBeginTime) {
                    stepTime = this.firstEpochBeginTime - currentTime;
                    speedFactor = this.firstEpochSpeedFactor;
                } else {
                    int patternIndex = (int) ((currentTime - this.firstEpochBeginTime) / step);
                    stepTime = trafficPattern.get(patternIndex).epochBeginTime + step - currentTime;
                    speedFactor = this.trafficPattern.get(patternIndex).speed_factor;
                }
                adjustedSpeed = unadjustedSpeed * speedFactor;
                double stepDistance = adjustedSpeed * stepTime;
                if (totalDistance + stepDistance < distance) {
                    // finish a full step
                    totalDistance += stepDistance;
                    totalTime += stepTime;
                    currentTime += stepTime;
                } else {
                    // finish a partial step
                    double remainingDistance = distance - totalDistance;
                    double remainingTime = remainingDistance / adjustedSpeed;
                    totalTime += remainingTime;
                    break;
                }
            }
        }
        return totalTime;
    }

    // dynamic travel time from a location on a road to the end of the road starting at a given time (long type)
    public long roadTravelTimeToEndIntersection(long time, LocationOnRoad loc) {
        return (long)Math.round(roadTravelTimeToEndIntersection((double)time, loc));
    }

    // dynamic travel time from a location on a road to the end of the road starting at a given time (double type).
    public double roadTravelTimeToEndIntersection(double time, LocationOnRoad loc) {
        Road road = loc.road;
        LocationOnRoad endIntersection = LocationOnRoad.createFromRoadEnd(road);
        return roadForwardTravelTime(time, loc, endIntersection);
    }

    // dynamic travel time from a location on a road to the start of the road starting at a given time (long type).
    public long roadTravelTimeFromStartIntersection(long time, LocationOnRoad loc) {
        return (long)Math.round(roadTravelTimeFromStartIntersection((double)time, loc));
    }

    // dynamic travel time from a location on a road to the start of the road starting at a given time (double type).
    public double roadTravelTimeFromStartIntersection(double time, LocationOnRoad loc) {
        Road road = loc.road;
        LocationOnRoad startIntersection = LocationOnRoad.createFromRoadStart(road);
        return roadForwardTravelTime(time, startIntersection, loc);
    }

    // dynamic travel time from a location on a road to another location on the same road at a given time (long type).
    public long roadForwardTravelTime(long time, LocationOnRoad loc1, LocationOnRoad loc2) {
        return (long)Math.round(roadForwardTravelTime((double)time, loc1, loc2));
    }

    // dynamic travel time from a location on a road to another location on the same road at a given time (double type).
    public double roadForwardTravelTime(double time, LocationOnRoad loc1, LocationOnRoad loc2) {
        assert loc1.upstreamTo(loc2) : "loc1 must be upstream to loc2";

        return dynamicForwardTravelTime(time, loc1.road.speed, loc1.getDisplacementOnRoad(loc2));
    }

    // compute the travel distance along a link for a certain time starting at a given time
    public double[] dynamicTravelDistance(double time, double unadjustedSpeed, double travelTime, double maxDistance) {
        double totalDistance = 0.0;
        double totalTime = 0.0;
        double currentTime = time;

        double speedFactor;
        double adjustedSpeed;
        double distance;

        while (true) {
            double stepTime = -1.0;
            // travel for one step
            if (currentTime >= this.lastEpochBeginTime) {
                speedFactor = this.lastEpochSpeedFactor;
                adjustedSpeed = unadjustedSpeed * speedFactor;
                distance = (travelTime - totalTime) * adjustedSpeed;
                if (totalDistance + distance > maxDistance) {
                    double remainingDistance = maxDistance - totalDistance;
                    totalTime += remainingDistance / adjustedSpeed;
                    totalDistance = maxDistance;
                } else {
                    totalDistance += distance;
                    totalTime = travelTime;
                }
                break;
            } else {
                if (currentTime < this.firstEpochBeginTime) {
                    stepTime = this.firstEpochBeginTime - currentTime;
                    speedFactor = this.firstEpochSpeedFactor;
                } else {
                    int patternIndex = (int) ((currentTime - this.firstEpochBeginTime) / step);
                    stepTime = trafficPattern.get(patternIndex).epochBeginTime + step - currentTime;
                    speedFactor = trafficPattern.get(patternIndex).speed_factor;
                }
                adjustedSpeed = unadjustedSpeed * speedFactor;
                if (totalTime + stepTime > travelTime) {
                    double remainingTime = travelTime - totalTime;
                    distance = adjustedSpeed * remainingTime;
                    totalDistance += distance;
                    totalTime = travelTime;
                    break;
                } else {
                    distance = adjustedSpeed * stepTime;
                    if (totalDistance + distance > maxDistance) {
                        double remainingDistance = maxDistance - totalDistance;
                        totalTime += remainingDistance / adjustedSpeed;
                        totalDistance = maxDistance;
                        break;
                    } else {
                        totalDistance += distance;
                        totalTime += stepTime;
                        currentTime += stepTime;
                    }
                }
            }
        }
        double[] returnValue = {totalDistance, totalTime};
        return returnValue;
    }

    // The location when traveling along a road from a given location for a given amount of time (double type) starting at a given time.
    public LocationOnRoad travelRoadForTime(long time, LocationOnRoad locationOnRoad, long travelTime) {
        return travelRoadForTime((double)time, locationOnRoad, (double)travelTime);

    }

    // The location when traveling along a road from a given location for a given amount of time (long type) starting at a given time.
    public LocationOnRoad travelRoadForTime(double time, LocationOnRoad locationOnRoad, double travelTime) {
        double[] distanceTimePair = dynamicTravelDistance(time, locationOnRoad.road.speed, travelTime, locationOnRoad.road.length);
        double traveledDistance = distanceTimePair[0];
        double traveledTime = distanceTimePair[1];
        if (traveledTime < travelTime) {
            // reached the end of road before travel time is used out
            return LocationOnRoad.createFromRoadEnd(locationOnRoad.road);
        } else {
            return new LocationOnRoad(locationOnRoad, traveledDistance);
        }
    }
}

