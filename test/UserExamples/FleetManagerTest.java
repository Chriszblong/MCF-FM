package UserExamples;

import COMSETsystem.*;
import UserExamples.RandomDestinationFleetManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * This test FleetManager Class basic functionality
 * It will initially only test the RandomDestinationFleetManager and we'll generalize later to cover
 * all Fleet Manager Implementations
 */
@RunWith(MockitoJUnitRunner.class)
public class FleetManagerTest {
    public static final long RESOURCE_AVAILALBLE_TIME = 1000L;
    @Mock
    CityMap mockMap;
    @Mock
    Resource mockResource;

    final SimpleMap testMap = new SimpleMap();

    /**
     * Test onReachIntersection creates a route when there is no route and returns the first
     * intersection in the route
     */
    @Test
    public void test_onReachIntersection_emptyRoute() {
        // Instantiate a FleetManager
        RandomDestinationFleetManager spyFleetManager = spy(new RandomDestinationFleetManager(mockMap));
        LocationOnRoad currentLoc = new LocationOnRoad(testMap.roadFrom1to2, testMap.roadFrom1to2.travelTime);
        Intersection[] intersections  = {testMap.intersection3, testMap.intersection4};
        doReturn(new LinkedList<>(Arrays.asList(intersections)))
                .when(spyFleetManager).getRandomRoute(1, currentLoc);

        Intersection intersection = spyFleetManager.onReachIntersection(1, 1000, currentLoc);
        verify(spyFleetManager, times(1)).getRandomRoute(1, currentLoc);
        assertEquals(testMap.intersection3, intersection);

        // Check that a route has been created
        assertTrue(spyFleetManager.agentRoutes.containsKey(1L));
    }

    /**
     * Test onReachIntersection creates a route with already planned route and returns the first
     * intersection in the route
     */
    @Test
    public void test_onReachIntersection_nonEmptyRoute() {
        RandomDestinationFleetManager spyFleetManager = spy(new RandomDestinationFleetManager(mockMap));
        LocationOnRoad currentLoc = new LocationOnRoad(testMap.roadFrom1to2, testMap.roadFrom1to2.travelTime);
        Intersection[] intersections  = {testMap.intersection3, testMap.intersection4, testMap.intersection5};
        spyFleetManager.agentRoutes.put(1L, new LinkedList<>(Arrays.asList(intersections)));

        Intersection intersection = spyFleetManager.onReachIntersection(1L, 1000, currentLoc);

        assertEquals(testMap.intersection3, intersection);
        verify(spyFleetManager, times(0)).planRoute(anyLong(), any(LocationOnRoad.class));
        // Check the remaining part of the route has the left over part of the route
        // Argument to containsKey and get must have long type        assertTrue(spyFleetManager.agentRoutes.containsKey(1L));
        assertEquals(testMap.intersection4, spyFleetManager.agentRoutes.get(1L).getFirst());
    }

    /**
     * Test onReachIntersectionWithResource creates a route when there is no route and returns the first
     * intersection in the route
     */
    @Test
    public void test_onReachIntersectionWithResource_emptyRoute() {
        // Instantiate a FleetManager
        RandomDestinationFleetManager spyFleetManager = spy(new RandomDestinationFleetManager(mockMap));
        LocationOnRoad currentLoc = new LocationOnRoad(testMap.roadFrom1to2, testMap.roadFrom1to2.travelTime);
        Intersection[] intersections  = {testMap.intersection3, testMap.intersection4};

        // setup mockResource
        mockResource.pickupLoc = SimpleMap.makeLocationFromRoad(testMap.roadFrom1to2, 0.5);
        mockResource.dropOffLoc = SimpleMap.makeLocationFromRoad(testMap.roadFrom3to4, 0.75);
        doReturn(new LinkedList<>(Arrays.asList(intersections))).when(spyFleetManager)
                .planRouteToTarget(mockResource.pickupLoc, mockResource.dropOffLoc);

        Intersection intersection = spyFleetManager.onReachIntersectionWithResource(
                1, 1000, currentLoc, mockResource);
        assertEquals(testMap.intersection3, intersection);

        // Check that a route has been created
        assertTrue(spyFleetManager.agentRoutes.containsKey(1L));
    }

    /**
     * Test onReachIntersectionWithResource creates a route with already planned route and returns the first
     * intersection in the route
     */
    @Test
    public void test_onReachIntersectionWithResource_nonEmptyRoute() {
        RandomDestinationFleetManager spyFleetManager = spy(new RandomDestinationFleetManager(mockMap));
        LocationOnRoad currentLoc = new LocationOnRoad(testMap.roadFrom1to2, testMap.roadFrom1to2.travelTime);
        Intersection[] intersections  = {testMap.intersection3, testMap.intersection4, testMap.intersection5};
        spyFleetManager.agentRoutes.put(1L, new LinkedList<>(Arrays.asList(intersections)));

        Intersection intersection = spyFleetManager.onReachIntersectionWithResource(
                1L, 1000, currentLoc, mockResource);

        assertEquals(testMap.intersection3, intersection);
        verify(spyFleetManager, times(0)).planRoute(anyLong(), any(LocationOnRoad.class));
        // Check the remaining part of the route has the left over part of the route
        // Argument to containsKey and get must have long type        assertTrue(spyFleetManager.agentRoutes.containsKey(1L));
        assertEquals(testMap.intersection4, spyFleetManager.agentRoutes.get(1L).getFirst());
    }

    @Test
    // TODO Is this test doing the right thing?
    public void test_onResourceAvailabilityChange_resourceAvailable() {
        RandomDestinationFleetManager spyFleetManager = spy(new RandomDestinationFleetManager(mockMap));
        // spyFleetManager.agentsCreated(new HashSet<>(Collections.singletonList(1L)));
        LocationOnRoad resourceWaitingLocation = SimpleMap.makeLocationFromRoad(testMap.roadFrom1to2, 0.5);

        // Set up return values from other class functions.
        doReturn(1L).when(spyFleetManager).getNearestAvailableAgent(mockResource,
                RESOURCE_AVAILALBLE_TIME);

        AgentAction action = spyFleetManager.onResourceAvailabilityChange(mockResource,
                FleetManager.ResourceState.AVAILABLE, resourceWaitingLocation, RESOURCE_AVAILALBLE_TIME);

        assertEquals(AgentAction.Type.ASSIGN, action.getType());

    }
}
