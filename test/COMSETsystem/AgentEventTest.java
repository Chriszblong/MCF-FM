package COMSETsystem;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AgentEventTest {

    public static final int CUSTOMER_TRIP_TIME = 500;
    public static final long TIME_TO_PICKUP_CUSTOMER = 300L;
    public static final int TRIGGER_TIME = 100;
    public static final int AVAILABLE_TIME = 50;
    public static final int RESOURCE_MAX_LIFE_TIME = 600;
    @Mock
    Simulator mockSimulator;
    @Mock
    Event mockEvent;
    @Mock
    ResourceEvent mockResourceEvent;
    @Mock
    LocationOnRoad mockLocationOnRoad;
    @Mock
    Road mockRoad;
    @Mock
    Simulator.PickUp mockNoPickUp;
    @Mock
    FleetManager mockFleetManager;
    @Mock
    TrafficPattern mockTrafficPattern;
    @Mock
    CityMap mockCityMap;
    @Mock
    ScoreInfo mockScoreInfo;

    private final SimpleMap testMap = new SimpleMap();

    @Before
    public void BeforeEachTest() {
        mockFleetManager.trafficPattern = mockTrafficPattern;
        mockSimulator.trafficPattern = mockTrafficPattern;
        mockSimulator.map = mockCityMap;
        mockSimulator.score = mockScoreInfo;
    }

   /**
     * Tests cruising this path intersection 1 -> intersection 2 -> intersection 3.
     *
     * @throws Exception from spyEvent.Trigger if any
     */
    @Test
    public void testTrigger_cruising() throws Exception {
        long stubTravelTime = 10;
        double initTimeFromStartIntersection = testMap.roadFrom1to2.travelTime / 2;
        // Construction agent reaching intersection2
        LocationOnRoad locAtMiddleOfRoad = new LocationOnRoad(testMap.roadFrom1to2, initTimeFromStartIntersection);

        AgentEvent agentEvent = new AgentEvent(locAtMiddleOfRoad, TRIGGER_TIME, mockSimulator, mockFleetManager);

        // Verify initial state
        assertEquals(AgentEvent.State.INITIAL, agentEvent.state);

        // stub calculation in traffic pattern
        when(mockTrafficPattern.roadTravelTimeToEndIntersection(anyLong(), any(LocationOnRoad.class))).thenReturn(stubTravelTime);
        
        AgentEvent nextEvent = (AgentEvent) agentEvent.trigger();
        // return intersection3 as next intersection
        when(mockFleetManager.onReachIntersection(anyLong(), anyLong(), any()))
                .thenReturn(testMap.intersection3);

        // Verify that next AgentEvent will trigger when reaching the end of road1
        long nextEventTime = TRIGGER_TIME + stubTravelTime;

        assertEquals(AgentEvent.State.INTERSECTION_REACHED, nextEvent.state);
        assertEquals(testMap.roadFrom1to2, nextEvent.loc.road);
        assertEquals(nextEventTime, nextEvent.getTime());

        // stub calculation in traffic pattern
        when(mockTrafficPattern.roadTravelTimeFromStartIntersection(anyLong(), any(LocationOnRoad.class))).thenReturn(stubTravelTime);
        nextEvent = (AgentEvent) agentEvent.trigger();

        // Verify that next AgentEvent will trigger when reaching the end of road2
        nextEventTime += stubTravelTime;
        assertEquals(AgentEvent.State.INTERSECTION_REACHED, nextEvent.state);
        assertEquals(testMap.roadFrom2to3, nextEvent.loc.road);
        assertEquals(nextEventTime, nextEvent.getTime());
    }

    /**
     * Test the following sequence of events
     * 1. Travel to intersection just before pickup
     * 2. Travel to pickup
     * 3. pickup
     * 4. travel to dropoff
     * 5. dropoff
     * 6. back to cruising
     */
    @Test
    public void testTrigger_pickUpAndDropOff() throws Exception {
        // Create a Resource to be picked up half way down on road2to3 and to be dropped off
        // half way down on roadFrom4to5. Agent should traverse roadFrom2to3, roadFrom3to4,
        // and roadFrom4to5
        ResourceEvent customer = makeCustomer(
                SimpleMap.makeLocationFromRoad(testMap.roadFrom2to3, 0.5),
                SimpleMap.makeLocationFromRoad(testMap.roadFrom4to5, 0.5));

        // Create an agentEvent that has a pickup upon reaching intersection2 the beginning of
        // roadFrom2to3
        LocationOnRoad locAtReachedIntersection = new LocationOnRoad(testMap.roadFrom1to2,
                testMap.roadFrom1to2.travelTime);
        AgentEvent agentEvent = new AgentEvent(locAtReachedIntersection, TRIGGER_TIME,
                mockSimulator, mockFleetManager);

        long travelTime = 10;
        when(mockTrafficPattern.travelRoadForTime(anyLong(), any(LocationOnRoad.class), anyLong())).thenReturn(locAtReachedIntersection);
        when(mockTrafficPattern.roadTravelTimeFromStartIntersection(anyLong(), any(LocationOnRoad.class))).thenReturn(travelTime);
        when(mockCityMap.travelTimeBetween(any(LocationOnRoad.class), any(LocationOnRoad.class))).thenReturn(5L);
        when(mockTrafficPattern.roadTravelTimeToEndIntersection(anyLong(), any(LocationOnRoad.class))).thenReturn(travelTime);

        agentEvent.assignTo(customer, 1);
        agentEvent = (AgentEvent) agentEvent.trigger();
        long nextEventTime = TRIGGER_TIME + travelTime;

        // Trigger the event
        AgentEvent pickUpEvent = (AgentEvent) agentEvent.trigger();

        // Verify that pickup was detected with correct pickuptime.
        nextEventTime += travelTime;
        assertEquals(AgentEvent.State.PICKING_UP, pickUpEvent.state);
        assertEquals(nextEventTime, pickUpEvent.getTime());
        assertEquals(testMap.roadFrom2to3, pickUpEvent.loc.road);

        // Trigger pickup Event
        AgentEvent travelToDropoffEvent = (AgentEvent) pickUpEvent.trigger();

        // Verify customer was picked up at half-way point of roadFrom2to3
        long expectedPickupTime = nextEventTime;
        assertEquals(expectedPickupTime, customer.pickupTime);

        // Verify the behavior of recording approach time
        verify(mockScoreInfo).recordApproachTime(nextEventTime, TRIGGER_TIME, 1, AVAILABLE_TIME, 5);

        // Verify returned travel event represents travel to the end of the roadFrom2to3
        nextEventTime += travelTime;
        assertEquals(AgentEvent.State.INTERSECTION_REACHED, travelToDropoffEvent.state);
        assertEquals( nextEventTime, travelToDropoffEvent.getTime());
        assertEquals(testMap.roadFrom2to3, travelToDropoffEvent.loc.road);
        assertTrue(travelToDropoffEvent.isPickup);

        // Setup expectations, we expect a call to FleetManager upon reaching end of road2to3 to
        // get next intersection which is intersection 4, i.e. the end of roadFrom3to4.
        when(mockFleetManager.onReachIntersectionWithResource(eq(travelToDropoffEvent.id),
                eq(nextEventTime), any(),
                    any(Resource.class)))
                .thenReturn(testMap.intersection4);

        // Trigger travel to DropOff's second segment
        AgentEvent travelToDropOffEvent2 = (AgentEvent) travelToDropoffEvent.trigger();

        nextEventTime += travelTime;
        assertEquals(AgentEvent.State.INTERSECTION_REACHED, travelToDropOffEvent2.state);
        assertEquals(nextEventTime,
                travelToDropOffEvent2.getTime());
        assertEquals(testMap.roadFrom3to4, travelToDropOffEvent2.loc.road);
        assertTrue(travelToDropOffEvent2.isPickup);

        // Now trigger to create drop-off event
        AgentEvent dropoffEvent = (AgentEvent) travelToDropOffEvent2.trigger();

        nextEventTime += travelTime;
        assertEquals(AgentEvent.State.DROPPING_OFF, dropoffEvent.state);
        assertEquals(nextEventTime,
                dropoffEvent.getTime());
        assertEquals(testMap.roadFrom4to5, dropoffEvent.loc.road);

        // trigger drop-off
        AgentEvent backToCruisingEvent = (AgentEvent) dropoffEvent.trigger();

        // Verify the behavior of recording completed trip time
        verify(mockScoreInfo).recordCompletedTrip(nextEventTime, expectedPickupTime, 5);

        assertEquals(nextEventTime, backToCruisingEvent.startSearchTime);

        nextEventTime += travelTime;
        verify(mockSimulator, times(1)).removeEvent(customer);
        assertEquals(AgentEvent.State.INTERSECTION_REACHED, backToCruisingEvent.state);
        assertEquals(testMap.roadFrom4to5, backToCruisingEvent.loc.road);
        assertFalse(backToCruisingEvent.isPickup);
        assertNull(backToCruisingEvent.assignedResource);
        assertEquals(nextEventTime, backToCruisingEvent.getTime());
    }

    private ResourceEvent makeCustomer(LocationOnRoad pickupLocation,
                                       LocationOnRoad dropoffLocation) {
        return new ResourceEvent(pickupLocation, dropoffLocation,
                AVAILABLE_TIME, 0, mockSimulator, mockFleetManager, RESOURCE_MAX_LIFE_TIME);
    }

    @Test
    public void testNavigate_withPickUp() throws Exception {
        LocationOnRoad locationOnRoad = spy(new LocationOnRoad(testMap.roadFrom1to2, testMap.roadFrom1to2.travelTime));
        when(locationOnRoad.toString()).thenReturn("123,45t");


        ResourceEvent resource = new ResourceEvent(
                new LocationOnRoad(testMap.roadFrom2to3, 20L),
                new LocationOnRoad(testMap.roadFrom2to3, 10L),
                100L,
                1000L,
                mockSimulator,
                mockFleetManager, RESOURCE_MAX_LIFE_TIME
        );

        AgentEvent spyEvent = spy(new AgentEvent(locationOnRoad, 100, mockSimulator, mockFleetManager));
        spyEvent.assignTo(resource, 1);

        spyEvent = (AgentEvent) spyEvent.trigger();

        assertEquals(AgentEvent.State.INTERSECTION_REACHED, spyEvent.state);

        spyEvent = (AgentEvent) spyEvent.trigger();

        assertEquals(AgentEvent.State.PICKING_UP, spyEvent.state);

    }

    @Test
    public void testExpiration_withAssignedAgent() throws Exception {
        LocationOnRoad locationOnRoad = spy(new LocationOnRoad(testMap.roadFrom1to2, testMap.roadFrom1to2.travelTime));
        when(locationOnRoad.toString()).thenReturn("123,45t");

        ResourceEvent resource = new ResourceEvent(
                new LocationOnRoad(testMap.roadFrom2to3, 20L),
                new LocationOnRoad(testMap.roadFrom2to3, 10L),
                100L,
                1000L,
                mockSimulator,
                mockFleetManager, RESOURCE_MAX_LIFE_TIME
        );
        resource.state = ResourceEvent.State.EXPIRED;

        AgentEvent spyEvent = spy(new AgentEvent(locationOnRoad, 101L, mockSimulator, mockFleetManager));
        // Agent init state: move to the end of intersection
        spyEvent = (AgentEvent) spyEvent.trigger();

        spyEvent.assignTo(resource, 101L);
        resource.assignTo(spyEvent);

        spyEvent = (AgentEvent) spyEvent.trigger();

        assertEquals(AgentEvent.State.PICKING_UP, spyEvent.state);

        resource.trigger();

        assertEquals(AgentEvent.State.INTERSECTION_REACHED, spyEvent.state);
    }

    @Test
    public void testTwoRoadsIntesecting() {
        assertEquals(testMap.intersection1, testMap.roadFrom1to2.from);
        assertEquals(testMap.intersection2, testMap.roadFrom1to2.to);
        assertEquals(testMap.roadFrom1to2.to, testMap.roadFrom2to3.from);
        assertEquals(testMap.intersection3, testMap.roadFrom2to3.to);
    }

}
