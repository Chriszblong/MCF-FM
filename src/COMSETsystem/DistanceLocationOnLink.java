package COMSETsystem;

public class DistanceLocationOnLink {
    public Link link;
    public double distanceFromStartVertex;

    public DistanceLocationOnLink(Link link, double distanceFromStartVertex) {
        this.link = link;
        this.distanceFromStartVertex = distanceFromStartVertex;
    }

    public boolean upstreamTo(DistanceLocationOnLink aLoc) {
        assert this.link.road.id == aLoc.link.road.id : "two links must be on the same road";
        Road road = this.link.road;
        if (road.links.indexOf(this.link) < road.links.indexOf(aLoc.link)) {
            return true;
        } else if (road.links.indexOf(this.link) == road.links.indexOf(aLoc.link)) {
            if (this.distanceFromStartVertex <= aLoc.distanceFromStartVertex) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean atEndIntersection() {
        return (this.link.id == this.link.road.links.get(this.link.road.links.size()-1).id &&
                this.distanceFromStartVertex == this.link.road.links.get(this.link.road.links.size()-1).length);
    }

    // create location at the end of a road
    public static DistanceLocationOnLink createFromRoadEnd(Road road) {
        Link lastLink = road.links.get(road.links.size()-1);
        return new DistanceLocationOnLink(lastLink, lastLink.length);
    }

    // create location at the end of a road
    public static DistanceLocationOnLink createFromRoadStart(Road road) {
        Link firstLink = road.links.get(0);
        return new DistanceLocationOnLink(firstLink, 0);
    }

    public String toString() {
        return "(road: " + link.road.id + ", link: " + link.id + ", distanceFromStartVertex: " + distanceFromStartVertex;
    }
}
