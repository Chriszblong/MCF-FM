package COMSETsystem;

/**
 * A SimpleMap for Testing
 */
public class SimpleMap {

    public final Vertex vertex1;
    public final Vertex vertex2;
    public final Vertex vertex3;
    public final Vertex vertex4;
    public final Vertex vertex5;
    public final Link link1to2;
    public final Link link2to3;
    public final Link link3to4;
    public final Link link4to5;
    public final Intersection intersection1;
    public final Intersection intersection2;
    public final Intersection intersection3;
    public final Intersection intersection4;
    public final Intersection intersection5;
    public final Road roadFrom1to2; // 1km long
    public final Road roadFrom2to3;
    public final Road roadFrom3to4;
    public final Road roadFrom4to5;

    private static Vertex makeVertex(final double longitude, final double latitude, final int id) {
        return new Vertex(longitude, latitude, id, id, id);
    }

    private static Intersection makeIntersection(Vertex vertex) {
        Intersection intersection = new Intersection(vertex);
        vertex.intersection = intersection;
        return intersection;
    }

    private static Road makeRoad(Intersection intersection1, Intersection intersection2) {
        Road r = new Road();
        r.from = intersection1;
        r.to = intersection2;
        r.to.roadsMapTo.put(r.from, r);
        r.from.roadsMapFrom.put(r.to, r);
        return r;
    }

    public static LocationOnRoad makeLocationFromRoad(Road road, double fraction) {
        return new LocationOnRoad(road, road.length * fraction);
    }


    public SimpleMap(){
        vertex1 = makeVertex(100.0, 100.0, 1);
        vertex2 = makeVertex(100.0, 101.0, 2);
        vertex3 = makeVertex(100.0, 102.0, 3);
        vertex4 = makeVertex(100.0, 103.0, 4);
        vertex5 = makeVertex(100.0, 104.0, 5);
        link1to2 = new Link(vertex1, vertex2, 1000, 1);
        link2to3 = new Link(vertex2, vertex3, 1200, 60);
        link3to4 = new Link(vertex3, vertex4, 800, 20);
        link4to5 = new Link(vertex4, vertex5, 900, 10);
        intersection1 = makeIntersection(vertex1);
        intersection2 = makeIntersection(vertex2);
        intersection3 = makeIntersection(vertex3);
        intersection4 = makeIntersection(vertex4);
        intersection5 = makeIntersection(vertex5);
        roadFrom1to2 = makeRoad(intersection1, intersection2);
        roadFrom2to3 = makeRoad(intersection2, intersection3);
        roadFrom3to4 = makeRoad(intersection3, intersection4);
        roadFrom4to5 = makeRoad(intersection4, intersection5);
        roadFrom1to2.addLink(link1to2);
        roadFrom2to3.addLink(link2to3);
        roadFrom3to4.addLink(link3to4);
        roadFrom4to5.addLink(link4to5);
    }
}
