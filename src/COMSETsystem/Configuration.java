package COMSETsystem;

import MapCreation.MapCreator;

/**
 * Class to hold the configuration parameters of the simulation. Call static method Configuration.make() first to
 * create a singleton configuration object, then call Configuration.get() to retrieve the singleton.
 */
public class Configuration {
    // Full path to an OSM JSON map file
    public final String mapJSONFile;

    // Full path to a KML defining the bounding polygon to crop the map
    public final String boundingPolygonKMLFile;

    // Full path to a TLC New York Yellow trip record file
    public final String resourceFile;

    public static long timeResolution = 1000000;
    public static double minimumDistance = 54/(double)timeResolution;

    // FIXME: The field numberOfAgents should be protected or private. Most code, beside agent creation and placement
    //   code don't need to know its value. Also the field dynamicTraffic should also be hidden.
    // The number of agents that are deployed (at the beginning of the simulation).
    public final long numberOfAgents;
    public final boolean dynamicTrafficEnabled;

    // Members accessible from COMSETsystem, hidden from others, i.e. they have no business knowing them.
    public final long resourceMaximumLifeTime, resourceMaximumLifeTimeInSeconds;
    // Traffic pattern epoch in seconds
    public final long trafficPatternEpoch, trafficPatternEpochInSeconds;
    // Traffic pattern step in seconds
    public final long trafficPatternStep, trafficPatternStepInSeconds;
    public final long agentPlacementRandomSeed;

    protected static Configuration singletonConfiguration;

    // A class that extends BaseAgent and implements a search routing strategy
    protected final Class<? extends FleetManager> fleetManagerClass;

    // The map that everything will happen on.
    final CityMap map;

    private Configuration(Class<? extends FleetManager> fleetManagerClass,
                          String mapJSONFile,
                          String resourceFile,
                          long numberOfAgents,
                          String boundingPolygonKMLFile,
                          long resourceMaximumLifeTime,
                          long agentPlacementRandomSeed,
                          boolean dynamicTrafficEnabled,
                          long trafficPatternEpoch,
                          long trafficPatternStep) {
        this.fleetManagerClass = fleetManagerClass;
        this.mapJSONFile = mapJSONFile;
        this.resourceFile = resourceFile;
        this.numberOfAgents = numberOfAgents;
        this.boundingPolygonKMLFile = boundingPolygonKMLFile;
        resourceMaximumLifeTimeInSeconds = resourceMaximumLifeTime;
        this.resourceMaximumLifeTime = resourceMaximumLifeTimeInSeconds * timeResolution;
        this.agentPlacementRandomSeed = agentPlacementRandomSeed;
        this.dynamicTrafficEnabled = dynamicTrafficEnabled;

        trafficPatternEpochInSeconds = trafficPatternEpoch;
        this.trafficPatternEpoch = trafficPatternEpochInSeconds * timeResolution;
        trafficPatternStepInSeconds = trafficPatternStep;
        this.trafficPatternStep = trafficPatternStepInSeconds * timeResolution;

        map = makeCityMap();

        // Pre-compute shortest travel times between all pairs of intersections.
        System.out.println("Pre-computing all pair travel times...");
        map.calcTravelTimes();
    }

    public static void make(Class<? extends FleetManager> fleetManagerClass,
                            String mapJSONFile,
                            String resourceFile,
                            long numberOfAgents,
                            String boundingPolygonKMLFile,
                            long resourceMaximumLifetime,
                            long agentPlacementRandomSeed,
                            boolean dynamicTraffic,
                            long trafficPatternEpoch,
                            long trafficPatternStep) {
        if (singletonConfiguration == null) {
            singletonConfiguration = new Configuration(
                    fleetManagerClass,
                    mapJSONFile,
                    resourceFile,
                    numberOfAgents,
                    boundingPolygonKMLFile,
                    resourceMaximumLifetime,
                    agentPlacementRandomSeed,
                    dynamicTraffic,
                    trafficPatternEpoch,
                    trafficPatternStep);
        }
    }

    /**
     * Get the singleton instance of the configuration.
     * @return Configuration instance
     */
    public static Configuration get() {
        assert singletonConfiguration != null;
        return singletonConfiguration;
    }

    /* Beside make() and get(), most methods should be static. Much safer that way to avoid initialization problems
    where they are called before proper initialization of the singleton.
     */
    public long toSeconds(long scaledSimulationTime) {
        return scaledSimulationTime / timeResolution;
    }

    public double toSimulatedSpeed(double distancePerSecond) {
        return distancePerSecond / timeResolution;
    }

    private CityMap makeCityMap() {
        MapCreator creator = new MapCreator(this);
        System.out.println("Creating the map...");

        creator.createMap();

        // Output the map
        return creator.outputCityMap();
    }
}
