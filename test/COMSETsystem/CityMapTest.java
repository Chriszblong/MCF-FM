package COMSETsystem;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class CityMapTest {

    @Test
    public void testTravelTimeBetween_sameLocation() {
        CityMap map = new CityMap();
        SimpleMap simpleMap = new SimpleMap();
        simpleMap.roadFrom1to2.speed = 1; // m/s
        LocationOnRoad location1 = SimpleMap.makeLocationFromRoad(simpleMap.roadFrom1to2, 0.2);
        assertEquals(1000.0, simpleMap.roadFrom1to2.length, 0.0);
        assertEquals(200, location1.getStaticTravelTimeOnRoad());
        assertEquals(0, map.travelTimeBetween(location1, location1));
    }

    @Test
    public void testTravelTimeBetween_differentLocationOnSameRoad() {
        CityMap map = new CityMap();
        SimpleMap simpleMap = new SimpleMap();
        simpleMap.roadFrom1to2.speed = 1; // m/s
        LocationOnRoad origin = SimpleMap.makeLocationFromRoad(simpleMap.roadFrom1to2, 0.2);
        LocationOnRoad destination = SimpleMap.makeLocationFromRoad(simpleMap.roadFrom1to2, 0.3);
        assertEquals(100, map.travelTimeBetween(origin, destination));
    }

    @Test
    public void testTravelTimeBetween_pastDestinationOnSameRoad() {
        CityMap spyMap = spy(new CityMap());
        SimpleMap simpleMap = new SimpleMap();
        simpleMap.roadFrom1to2.speed = 1; // m/s
        LocationOnRoad origin = SimpleMap.makeLocationFromRoad(simpleMap.roadFrom1to2, 0.31);
        LocationOnRoad destination = SimpleMap.makeLocationFromRoad(simpleMap.roadFrom1to2, 0.3);

        // setup mock return value for navigation back from intersection2 to intersection1
        doReturn(3600.0).when(spyMap).travelTimeBetween(simpleMap.intersection2, simpleMap.intersection1);

        // 3600 + time from start of road to destination + time from origin to end of road
        // 3600 + 300 + (1000-310)
        assertEquals(4590, (long)(spyMap.travelTimeBetween(origin, destination)));
    }

    @Test
    public void testTravelTimeBetween_pastDestinationOnButReallyCloseSameRoad() {
        CityMap spyMap = spy(new CityMap());
        SimpleMap simpleMap = new SimpleMap();
        simpleMap.roadFrom1to2.speed = 1; // m/s

        // The two locations are so close that rounding error causes the travel time in the
        // converted StaticLocationOnRoad object to be the same but because travelTimeBetween
        // use the double-typed distances to determine which route to take, it will recognize that
        // the origin is past the destination on the road.
        LocationOnRoad origin = SimpleMap.makeLocationFromRoad(simpleMap.roadFrom1to2, 0.30005);
        LocationOnRoad destination = SimpleMap.makeLocationFromRoad(simpleMap.roadFrom1to2, 0.3);

        // setup mock return value for navigation back from intersection2 to intersection1
        doReturn(3600.0).when(spyMap).travelTimeBetween(simpleMap.intersection2, simpleMap.intersection1);

        // 3600 + time from start of road to destination + time from origin to end of road
        // 3600 + 300 + (1000-Round(300.05)) = 4600
        assertEquals(4600, (long)(spyMap.travelTimeBetween(origin, destination)));
    }
}

