package COMSETsystem;

import java.text.NumberFormat;
import java.util.ArrayList;

/**
 * This class is used to give a performance report and the score. It prints
 * the total running time of the simulation, the used memory and the score.
 * It uses Runtime which allows the application to interface with the
 * environment in which the application is running.
 */
class ScoreInfo {

    final Configuration configuration;
    final Simulator simulator;

    // Total trip time of all resources to which agents have been assigned.
    private long totalResourceTripTime = 0;
    protected long getTotalResourceTripTime() {
        return totalResourceTripTime;
    }

    // Total wait time of all resources. The wait time of a resource is the amount of time
    // since the resource is introduced to the system until it is picked up by an agent, or it expires
    private long totalResourceWaitTime = 0;
    protected long getTotalResourceWaitTime() {
        return totalResourceWaitTime;
    }
    private void accumulateResourceWaitTime(long waitTime) {
        totalResourceWaitTime += waitTime;
    }

    // Total search time of all agents. The search time of an agent for a research is the amount of time
    // since the agent is labeled as empty, i.e., added to emptyAgents, until it picks up a resource.
    private long totalAgentSearchTime = 0;
    protected long getTotalAgentSearchTime() {
        return totalAgentSearchTime;
    }
    protected void recordApproachTime(long currentTime, long startSearchTime, long assignTime, long availableTime,
                                      long staticApproachTime) {
        totalAgentSearchTime += currentTime - startSearchTime;
        totalSearches++;
        accumulateResourceWaitTime(currentTime - availableTime);
        totalAgentCruiseTime += assignTime - startSearchTime;

        long approachTime = currentTime - assignTime;
        this.totalAgentApproachTime += approachTime;
        approachTimeCheckRecords.add(new IntervalCheckRecord(assignTime, approachTime,
                staticApproachTime));
    }

    // Total cruise time of all agents. The cruise time of an agent for a research is the amount of time
    // since the agent is labeled as empty until it is assigned to a resource.
    private long totalAgentCruiseTime = 0;

    // Total approach time of all agents. The approach time of an agent for a research is the amount of time
    // since the agent is assigned to a resource until agent reaches the resource.
    private long totalAgentApproachTime = 0;

    // The number of expired resources.
    private long expiredResources = 0;
    protected void recordExpiration() {
        expiredResources++;
        accumulateResourceWaitTime(configuration.resourceMaximumLifeTime);
    }

    // The number of resources that have been introduced to the system.
    protected long totalResources = 0;

    // The number of assignments that have been made, and dropped off
    private long totalAssignments = 0;

    // The number of times agents begin searching, i.e. they are just cruising around before picking up a resource.
    private long totalSearches = 0;

    // The number of times an agent fails to reach an assigned resource before the resource expires
    private long totalAbortions = 0;
    protected void recordAbortion() {
        totalAbortions++;
    }

    private final ArrayList<IntervalCheckRecord> approachTimeCheckRecords = new ArrayList<>();
    private final ArrayList<IntervalCheckRecord> completedTripTime = new ArrayList<>();

    final Runtime runtime = Runtime.getRuntime();
    final NumberFormat format = NumberFormat.getInstance();
    final StringBuilder sb = new StringBuilder();

    final long startTime;
    long allocatedMemory;

    /**
     * Constructor for ScoreInfo class. Runs beginning, this method
     * initializes all the necessary things.
     */
    ScoreInfo(Configuration configuration, Simulator simulator) {
        this.configuration = configuration;
        this.simulator = simulator;
        startTime = System.nanoTime();
        // Suppress memory allocation information display
        // beginning();
    }

    /**
     * Initializes and gets the max memory, allocated memory and free
     * memory. All of these are added to the Performance Report which is
     * saved in the StringBuilder. Furthermore also takes the time, such
     * that later on we can compare to the time when the simulation is over.
     * The allocated memory is also used to compare to the allocated memory
     * by the end of the simulation.
     */
    void beginning() {
        // Getting the memory used
        long maxMemory = runtime.maxMemory();
        allocatedMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();

        // probably unnecessary
        sb.append("Performance Report: " + "\n");
        sb.append("free memory: ").append(format.format(freeMemory / 1024)).append("\n");
        sb.append("allocated memory: ").append(format.format(allocatedMemory / 1024)).append("\n");
        sb.append("max memory: ").append(format.format(maxMemory / 1024)).append("\n");

        // still looking into this one "freeMemory + (maxMemory -
        // allocatedMemory)"
        sb.append("total free memory: ")
                .append(format.format(
                        (freeMemory + (maxMemory - allocatedMemory)) / 1024))
                .append("\n");

        System.out.print(sb.toString());
    }

    /**
     * Calculate the time the simulation took by taking the time right now
     * and comparing to the time when the simulation started. Add the total
     * time to the report and the score as well. Furthermore, calculate the
     * allocated memory by the participant's implementation by comparing the
     * previous allocated memory with the current allocated memory. Print
     * the Performance Report.
     */
    void end() {
        // Empty the string builder
        sb.setLength(0);

        long endTime = System.nanoTime();
        long totalTime = (endTime - startTime) / 1000000000;

        System.out.println("\nrunning time: " + totalTime);

        System.out.println("\n***Simulation environment***");
        System.out.println("JSON map file: " + configuration.mapJSONFile);
        System.out.println("Resource dataset file: " + configuration.resourceFile);
        System.out.println("Bounding polygon KML file: " + configuration.boundingPolygonKMLFile);
        System.out.println("Number of agents: " + configuration.numberOfAgents);
        System.out.println("Number of resources: " + totalResources);
        System.out.println("Resource Maximum Life Time: " +
                configuration.resourceMaximumLifeTimeInSeconds + " seconds");
        System.out.println("Fleet Manager class: " + configuration.fleetManagerClass.getName());
        System.out.println("Time resolution: " + Configuration.timeResolution);
        System.out.println("Simulation Start Time: " + simulator.simulationStartTime);
        System.out.println("Simulation End Time: " + simulator.simulationEndTime);
        System.out.println("Final Simulation Time: " + simulator.simulationTime);

        System.out.println("\n***Statistics***");

        if (totalResources != 0) {
            // Collect the "search" time for the agents that are empty at the end of the simulation.
            // These agents are in search status and therefore the amount of time they spend on
            // searching until the end of the simulation should be counted toward the total search time.
            // FIXME: Move this loop into the simulator and just add the time to totalAgentSearchTime and increment
            //   totalSearches.  This will allow us to make emptyAgents private.
            long totalRemainTime = 0;
            for (AgentEvent ae : simulator.emptyAgents) {
                totalRemainTime += (simulator.simulationEndTime - ae.startSearchTime);
            }

            sb.append("average agent search time: ")
                    .append(Math.floorDiv(
                            configuration.toSeconds(totalAgentSearchTime + totalRemainTime),
                            (totalAssignments + simulator.emptyAgents.size())))
                    .append(" seconds \n");
            sb.append("average resource wait time: ")
                    .append(Math.floorDiv(configuration.toSeconds(totalResourceWaitTime),
                            totalResources))
                    .append(" seconds \n");
            sb.append("resource expiration percentage: ")
                    .append(Math.floorDiv(expiredResources * 100,
                            totalResources))
                    .append("%\n");
//            sb.append("average agent search time: " + (configuration.toSeconds(totalAgentSearchTime + totalRemainTime)) / (double)(totalAssignments + simulator.emptyAgents.size()) + " seconds \n");
//            sb.append("average resource wait time: " + configuration.toSeconds(totalResourceWaitTime) / (double)totalResources + " seconds " +
//                    "\n");
//            sb.append("resource expiration percentage: " + expiredResources * 100 / (double)totalResources + "%\n");
            sb.append("\n");
            sb.append("average agent cruise time: ")
                    .append(Math.floorDiv(configuration.toSeconds(totalAgentCruiseTime),
                            totalAssignments)).append(" seconds \n");
            sb.append("average agent approach time: ")
                    .append(Math.floorDiv(configuration.toSeconds(totalAgentApproachTime),
                            totalAssignments)).append(" seconds \n");
            sb.append("average resource trip time: ")
                    .append(Math.floorDiv(configuration.toSeconds(totalResourceTripTime),
                            totalAssignments))
                    .append(" seconds \n");
            sb.append("total number of assignments: ")
                    .append(totalAssignments)
                    .append("\n");
            sb.append("total number of abortions: ")
                    .append(totalAbortions)
                    .append("\n");
            sb.append("total number of searches: ")
                    .append(totalSearches)
                    .append("\n");
        } else {
            sb.append("No resources.\n");
        }

        System.out.print(sb.toString());

        // TODO: Add configuration to control these checks.
        System.out.println("********** Complated Trips time checks");
        checkAndPrintIntervalRecords(completedTripTime, 10, 0.06);
        // checkAndPrintIntervalRecords(resourcePickupTimeCheckRecords, Integer.MAX_VALUE, 0.0);

        System.out.println("********** Approach time checks");
        checkAndPrintIntervalRecords(approachTimeCheckRecords, 10, 0.06);
        // checkAndPrintIntervalRecords(approachTimeCheckRecords, Integer.MAX_VALUE, 0.0);
    }

    private void checkAndPrintIntervalRecords(ArrayList<IntervalCheckRecord> checkRecords, int print_limit,
                                              double threshold) {
        double l2 = 0.0;
        int below_threshold_count = 0;
        System.out.println("time,simulated_ratio,expected_ratio,difference");
        for (final IntervalCheckRecord checkRecord: checkRecords) {
            final double ratio = computeRatio(checkRecord);
            // FIXME: store speedfactor in IntervalCheckRecord and we can get rid of this dependence on simulator and
            //  trafficPattern
            final double reference_ratio = simulator.trafficPattern.getSpeedFactor(checkRecord.time);
            final double diff = ratio-reference_ratio;
            if (Math.abs(diff) > threshold || Double.isNaN(diff)) {
                if (print_limit > 0) {
                    System.out.println(checkRecord.time + "," + ratio + "," + reference_ratio + "," + diff);
                }
                print_limit--;
                below_threshold_count++;
            }
            l2 += ratio * ratio;
        }
        System.out.println("Threshold =" + threshold + "; Count =" + below_threshold_count);
        System.out.println("Ratios RMS =" +
                Math.sqrt(l2 / completedTripTime.size())
                + "; Count =" + completedTripTime.size());
    }

    private double computeRatio(IntervalCheckRecord checkRecord) {
        // Take care of the special case of a match in which both interval and expected_interval are zeroes.
        // Think of this case as taking the limit as we approach 0/0. We assume that the default
        // speedfactor applies.
        // FIXME: store speedfactor in IntervalCheckRecord and we can get rid of this dependence on simulator and
        //  trafficPattern
        return (checkRecord.interval == 0 && checkRecord.expected_interval == 0) ?
                simulator.trafficPattern.getSpeedFactor(checkRecord.time) :
                checkRecord.expected_interval/(double)checkRecord.interval;
    }

    protected void recordCompletedTrip(long dropOffTime, long pickupTime, long staticTripTime) {
        long tripTime = dropOffTime - pickupTime;
        totalResourceTripTime += tripTime;
        totalAssignments++;
        completedTripTime.add(new IntervalCheckRecord(
                pickupTime, tripTime, staticTripTime));
    }

    private static class IntervalCheckRecord {
        public final long time;
        public final long interval;
        public final long expected_interval;

        IntervalCheckRecord(long time, long interval, long expected_interval) {
            this.time = time;
            this.interval = interval;
            this.expected_interval = expected_interval;
        }
    }
}
