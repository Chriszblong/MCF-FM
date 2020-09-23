import COMSETsystem.BaseAgent;
import COMSETsystem.Configuration;
import COMSETsystem.FleetManager;
import COMSETsystem.Simulator;

import java.io.IOException;
import java.util.logging.LogManager;
import java.util.Random;
import java.util.Properties;
import java.io.FileInputStream;

/**
 * The Main class parses the configuration file and starts the simulator.
 */

public class Main {
    public static long timeResolution;

    public static void main(String[] args) {

        String configFile = "etc/config.properties";
        try {
            Properties prop = new Properties();
            prop.load(new FileInputStream(configFile));

            //get the property values

            String mapJSONFile = prop.getProperty("comset.map_JSON_file");
            if (mapJSONFile == null) {
                System.out.println("The map JSON file must be specified in the configuration file.");
                System.exit(1);
            }
            mapJSONFile = mapJSONFile.trim();

            String datasetFile = prop.getProperty("comset.dataset_file");
            if (datasetFile == null) {
                System.out.println("The resource dataset file must be specified in the configuration file.");
                System.exit(1);
            }
            datasetFile = datasetFile.trim();

            String numberOfAgentsArg = prop.getProperty("comset.number_of_agents");
            long numberOfAgents = -1;
            if (numberOfAgentsArg != null) {
                numberOfAgents = Long.parseLong(numberOfAgentsArg.trim());
            } else {
                System.out.println("The number of agents must be specified in the configuration file.");
                System.exit(1);
            }

            String boundingPolygonKMLFile = prop.getProperty("comset.bounding_polygon_KML_file");
            if (boundingPolygonKMLFile == null) {
                System.out.println("The bounding polygon KML file must be specified in the configuration file.");
                System.exit(1);
            }
            boundingPolygonKMLFile = boundingPolygonKMLFile.trim();

            String agentClassName = prop.getProperty("comset.agent_class");
            if (agentClassName == null) {
                System.out.println("The agent class must be specified the configuration file.");
                System.exit(1);
            }
            agentClassName = agentClassName.trim();

            long resourceMaximumLifeTime = -1;
            String resourceMaximumLifeTimeArg = prop.getProperty("comset.resource_maximum_life_time");
            if (resourceMaximumLifeTimeArg != null) {
                resourceMaximumLifeTime = Long.parseLong(resourceMaximumLifeTimeArg.trim());
            } else {
                System.out.println("The resource maximum life time must be specified the configuration file.");
                System.exit(1);
            }

            boolean dynamicTraffic = false;
            String dynamicTrafficArg = prop.getProperty("comset.dynamic_traffic");
            if (dynamicTrafficArg != null) {
                dynamicTraffic = Boolean.parseBoolean(dynamicTrafficArg.trim());
            }

            long trafficPatternEpoch = 900; // in seconds
            String trafficPatternEpochArg = prop.getProperty("comset.traffic_pattern_epoch");
            if (trafficPatternEpochArg != null) {
                trafficPatternEpoch = Long.parseLong(trafficPatternEpochArg.trim());
            } else {
                System.out.println("The traffic pattern epoch must be specified the configuration file.");
                System.exit(1);
            }

            long trafficPatternStep = 60; // in seconds
            String trafficPatternStepArg = prop.getProperty("comset.traffic_pattern_step");
            if (trafficPatternStepArg != null) {
                trafficPatternStep = Long.parseLong(trafficPatternStepArg.trim());
            } else {
                System.out.println("The traffic pattern step must be specified the configuration file.");
                System.exit(1);
            }

            boolean displayLogging = false;
            String displayLoggingArg = prop.getProperty("comset.logging");
            if (displayLoggingArg != null) {
                displayLogging = Boolean.parseBoolean(displayLoggingArg.trim());
            }

            long agentPlacementSeed = -1;
            String agentPlacementSeedArg = prop.getProperty("comset.agent_placement_seed");
            if (agentPlacementSeedArg != null) {
                agentPlacementSeed = Long.parseLong(agentPlacementSeedArg.trim());
            }
            if (agentPlacementSeed < 0) {
                Random random = new Random();
                agentPlacementSeed = random.nextLong();
            }

            if (!displayLogging) {
                LogManager.getLogManager().reset();
            }

            Class<?> fleetManagerClass = Class.forName(agentClassName);
            //noinspection unchecked
            Configuration.make((Class<? extends FleetManager>) fleetManagerClass,
                    mapJSONFile, datasetFile, numberOfAgents, boundingPolygonKMLFile,
                    resourceMaximumLifeTime, agentPlacementSeed, dynamicTraffic, trafficPatternEpoch,
                    trafficPatternStep);

            Simulator simulator = new Simulator(Configuration.get());

            simulator.run();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
