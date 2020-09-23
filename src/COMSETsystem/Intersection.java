package COMSETsystem;

import java.util.*;
import java.awt.geom.Point2D;

/**
 * The Intersection class defines a vertex that connects different streets.
 * @author Robert van Barlingen and Jeroen Schols
 * mail Bobby.van.Barlingen@gmail.com
 */
public class Intersection implements Comparable<Intersection> {

	final public double longitude, latitude;
	
	// projected 2D coordinates
	final public Point2D xy; 
	
	// a unique id
	final public long id;
	
	// the index used to look up the shortest travel time path table (pathTable) in CityMap
	public int pathTableIndex; 
	
	// the vertex at which the intersection is located
	public Vertex vertex; 

	// The roads that end at this intersection, i.e., the roads for which this intersection is
	// the downstream intersection, also called incoming roads.
	public Map<Intersection, Road> roadsMapTo = new TreeMap<>();

	// The roads that start at this intersection, i.e., the roads for which this intersection is
	// the upstream intersection, also called outgoing roads.
	public Map<Intersection, Road> roadsMapFrom = new TreeMap<>();

	/**
	 * Constructor of Intersection.
	 * @param vertex the vertex at which the intersection is located
	 */
	public Intersection (Vertex vertex) {
		this.longitude = vertex.longitude;
		this.latitude = vertex.latitude;
		this.id = vertex.id;
		this.xy = vertex.xy;
		this.vertex =vertex;
	}
	
	/**
	 * Constructing from an existing intersection
	 * @param anIntersection
	 */
	public Intersection(Intersection anIntersection) {
		this.longitude = anIntersection.longitude;
		this.latitude = anIntersection.latitude;
		this.id = anIntersection.id;
		this.xy = new Point2D.Double(anIntersection.xy.getX(), anIntersection.xy.getY());
		this.pathTableIndex = anIntersection.pathTableIndex;
		this.vertex = null;
	}

	/**
	 * Checks if this intersection and the specified intersection are neighbors,
	 * i.e. if there is a road from this to the other or from the other to this.
	 * 
	 * @param i The intersection to check
	 * @return true if there is a road between this and i and false otherwise.
	 */
	public boolean isAdjacent (Intersection i) {
		return (roadsMapFrom.keySet().contains(i) || roadsMapTo.keySet().contains(i));
	}

	/**
	 * Return the road from this intersection to the specified intersection.
	 * 
	 * @param i The intersection that the road goes to
	 * @return The road between this and the other intersection
	 * @throws IllegalArgumentException if there is no road between this and
	 *          the other
	 */
	public Road roadTo (Intersection i) throws IllegalArgumentException {
		if (roadsMapFrom == null || i == null) {
			System.out.println("here");
		}
		if (roadsMapFrom.keySet().contains(i)) {
			return roadsMapFrom.get(i);
		}
		throw new IllegalArgumentException("no road between " +
				"this and i");
	}

	/**
	 * Return a set of all the roads going from this intersection to some
	 * other intersection
	 * 
	 * @return a set of roads from this intersection to other intersections 
	 */
	public Set<Road> getRoadsFrom () {
		return new TreeSet<>(roadsMapFrom.values());
	}

	/**
	 * Return a set of all the roads going from some intersection to this
	 * intersection
	 * 
	 * @return a set of roads going to this intersection 
	 */
	public Set<Road> getRoadsTo () {
		return new TreeSet<>(roadsMapTo.values());
	}

	/**
	 * Return a set of Intersections that you can directly go to from 
	 * this intersection, i.e. there exists a road from this intersection 
	 * to every intersection in the returned set
	 * 
	 * @return a set of intersections that you can go to from this intersection
	 */
	public Set<Intersection> getAdjacentFrom () {
		return roadsMapFrom.keySet();
	}

	/**
	 * Return a set of Intersections from which you can directly go to 
	 * this intersection, i.e. there exists a road from every intersection
	 * in the returned set to this intersection
	 * 
	 * @return a set of intersections from which you can directly go to 
	 *          this intersection
	 */
	public Set<Intersection> getAdjacentTo () {
		return roadsMapTo.keySet();
	}

	/**
	 * Checks if the given intersection is the same as this intersection
	 * 
	 * @param inter The given intersection to check
	 * @return true if the given intersection is the same as this intersection
	 */
	public boolean equals(Intersection inter) {
		return (inter.id == this.id);
	}

	/**
	 * returns the x-coordinate of this intersections
	 * @return x-coordinate
	 */
	public double getX() {
		return this.xy.getX();
	}

	/**
	 * returns the y-coordinate of this intersections
	 * @return y-coordinate
	 */
	public double getY() {
		return this.xy.getY();
	}

	/**
	 * returns the longitude and latitude as a string
	 * @return string of longitude and latitude
	 */
	public String toString() {
		return "(" + latitude + "," + longitude + ")";
	}

	/**
	 * checks whether this is the same intersection as some specified intersection
	 * 
	 * @param other intersection
	 * @return 0 is they are the same, 1 if {@code this.id > other.id} else -1
	 */
	public int compareTo(Intersection other) {
		if (id == other.id)
			return 0;
		else if (id < other.id)
			return -1;
		else
			return 1;
	}

}
