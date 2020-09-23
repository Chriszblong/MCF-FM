package COMSETsystem;

import java.awt.geom.Point2D;

/*
 * The Link class defines a directed link segment between two vertices.
 */
public class Link implements Comparable<Link> {

	// The start (upstream) vertex of the link
	public Vertex from;
	// The end (downstream) vertex of the link
	public Vertex to;
	// length of the link segment in meters
	public final double length;
	// travel time of the link segment in seconds
	public final double travelTime;
	// travel speed of the link segment in meters per second
	public final double speed;
	// a unique id
	public final long id;
	// reference to a Road object that contains this link
	public Road road;
	// the amount of time it takes to travel from the start of the road to the start vertex of this link 
	public double beginTime = -1;

	// min and max coordinates of the link
	public double minX;
	public double minY;
	public double maxX;
	public double maxY;
	
	// an ID counter to get a unique id
	private static long maxId = 0;

	/**
	 * Constructor for Link. Sets the beginning and end vertex, 
	 * the speed limit on the link, the length (distance) of the link and 
	 * (based on the speed limit and distance) the time it takes to traverse
	 * the link.
	 * 
	 * @param from The start vertex
	 * @param to The end vertex
	 * @param length the length of the link
	 * @param speed The speed limit on the link
	 */
	public Link (Vertex from, Vertex to, double length, double speed) {
		this.id = maxId++;
		this.from = from;
		this.to = to;
		this.length = length;
		this.speed = speed;
		this.travelTime = length/speed;
		this.road = null;
		minX = Math.min(from.xy.getX(), to.getX());
		minY = Math.min(from.xy.getY(), to.getY());
		maxX = Math.max(from.xy.getX(), to.getX());
		maxY = Math.max(from.xy.getY(), to.getY());
	}
	
	/**
	 * Constructing from an existing link
	 * @param aLink an existing link
	 * @param from the start intersection
	 * @param to the end intersection
	 */
	public Link (Link aLink, Vertex from, Vertex to) {
		this.id = aLink.id;
		this.from = from;
		this.to = to;
		this.length = aLink.length;
		this.speed = aLink.speed;
		this.travelTime = aLink.travelTime;
		this.beginTime = aLink.beginTime;
		this.road = null;
		minX = aLink.minX;
		minY = aLink.minY;
		maxX = aLink.maxX;
		maxY = aLink.maxY;
	}

	/**
	 * checks whether this is the same link as some specified link
	 * 
	 * @param other link
	 * @return 0 is they are the same, 1 if {@code this.id > other.id} else -1
	 */
	public int compareTo(Link other) {
		if (id == other.id)
			return 0;
		else if (id < other.id)
			return -1;
		else
			return 1;
	}

	/**
	 * Checks if the given link and this link are equal
	 * 
	 * @param link the given link to check
	 * @return true is the two links are equal, false otherwise
	 */
	public boolean equals(Link link) { //ASK: this isnt really used anywhere??
		return (link.from.equals(this.from) && link.to.equals(this.to));
	}

	/**
	 * returns information about the link as a string
	 * 
	 * @return string of information
	 */
	public String toString() {
		return from + "," + to + "," + length + "," + travelTime + "," + speed;
	}
	
	/**
	 * squared distance between a point and the link
	 * @param p a point
	 * @return distance square
	 */
	public double distanceSq(Point2D p) {
		double distSq;
		double x1 = this.from.getX();
		double y1 = this.from.getY();
		double x2 = this.to.getX();
		double y2 = this.to.getY();
		double x = p.getX();
		double y = p.getY();
		double length = (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);

		if (length == 0.0) {
			distSq = distanceSq(x1, y1, x, y);
		} else {
			double t = ((x - x1) * (x2 - x1) + (y - y1) * (y2 - y1)) / length;
			if (t < 0.0) {
				distSq = distanceSq(x1, y1, x, y);
			} else if (t > 1.0) {
				distSq = distanceSq(x2, y2, x, y);
			} else {
				double proj_x = x1 + t * (x2 - x1);
				double proj_y = y1 + t * (y2 - y1);
				distSq = distanceSq(proj_x, proj_y, x, y);
			}
		}
		return distSq;
	}
	
	/**
	 * squared distance between two points
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @return
	 */
	public double distanceSq(double x1, double y1, double x2, double y2) {
		return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
	}
}
