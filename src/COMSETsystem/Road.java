package COMSETsystem;

import java.util.ArrayList;

/*
 * The Road class defines a directed road segment between two intersections.
 * A road may consist of one or more links wherein each each link is a
 * directed straight line connecting two vertices.
 */
public class Road implements Comparable<Road> {

	// The start (upstream) intersection of the road
	public Intersection from;
	// The end (downstream) intersection of the road
	public Intersection to;
	// length of the road segment in meters
	public double length;
	// travel time of the road segment in seconds
	public double travelTime;
	// average speed in m/s
	public double speed;
	// a unique id
	public final long id;

	// an ID counter to get a unique id
	private static long maxId = 0;
	
	// links that constitute the road
	public final ArrayList<Link> links;

    /**
     * Constructing an "empty" road object.
     */
	public Road() {
		this.id = maxId++;
		this.length = 0;
		this.travelTime = 0;
		links = new ArrayList<>();
	}
	
	/**
	 * Creating a copy of a road
	 * @param road the road to copy
	 * @param from the start intersection
	 * @param to the end intersection
	 * @param links a list of links
	 */
	public Road(Road road, Intersection from, Intersection to, ArrayList<Link> links) {
		this.id = road.id;
		this.length = road.length;
		this.travelTime = road.travelTime;
		this.speed = road.speed;
		this.from = from;
		this.to= to;
		this.links = links;
	}
	
	/**
	 * Add a link to the road and accumulate travel time as a road can consists of
	 * multiple links. This code assume links are added in order, otherwise the beginTime
	 * for the link will not be correct.
	 *
	 */
	public void addLink(Link link) {
		links.add(link);
		link.road = this;
		link.beginTime = this.travelTime;
		this.length += link.length;
		this.travelTime += link.travelTime;
	}

	public void setSpeed() {
		// compute the average speed
		this.speed = this.length / this.travelTime;
	}

	/**
	 * checks whether this is the same road as some specified road
	 * 
	 * @param other road
	 * @return 0 is they are the same, 1 if {@code this.id > other.id} else -1
	 */
	public int compareTo(Road other) {
		return Long.compare(id, other.id);
	}

	/**
	 * Checks if the given road and this road are equal
	 * 
	 * @param road the given road to check
	 * @return true is the two roads are equal, false otherwise
	 */
	public boolean equals(Road road) { //ASK: this isnt really used anywhere??
		return (road.from.equals(this.from) && road.to.equals(this.to));
	}

	/**
	 * returns information about the road as a string
	 * 
	 * @return string of information
	 */
	public String toString() {
		return from + "," + to + "," + length + "," + travelTime + "," + speed;
	}
}
