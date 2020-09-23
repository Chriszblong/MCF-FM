package COMSETsystem;

import java.util.*;
import java.awt.geom.Point2D;

/**
 * A Vertex object corresponds to a "node" element in an OSM JSON map.
 * @author Robert van Barlingen and Jeroen Schols
 * mail Bobby.van.Barlingen@gmail.com
 */
public class Vertex implements Comparable<Vertex> {

	final public double longitude, latitude;
	
	// projected 2D coordinates
	final public Point2D xy;
	
	// a unique id
	public long id;
	
	// reference to the intersection at this vertex; null if this vertex is not an intersection
	public Intersection intersection; 

	// The links that end at this vertex, i.e., the links for which this vertex is
	// the downstream vertex, also called incoming roads.
	public Map<Vertex, Link> linksMapTo = new TreeMap<>();

	// The links that start at this vertex, i.e., the links for which this vertex is
	// the upstream vertex, also called outgoing links.
	public Map<Vertex, Link> linksMapFrom = new TreeMap<>();

	/**
	 * Constructor of Vertex. Set the location (longitude and latitude)
	 * of the vertex as well as the id.
	 * 
	 * @param longitude The longitude of the vertex
	 * @param latitude The latitude of the vertex
	 * @param x the projected X coordinate of the vertex
	 * @param y the projected Y coordinate of the vertex
	 * @param id The id of the vertex
	 */
	public Vertex (double longitude, double latitude, double x, double y, long id) {
		this.longitude = longitude;
		this.latitude = latitude;
		this.id = id;
		this.xy = new Point2D.Double(x, y);
		intersection = null; // a vertex has no intersection reference until promoted
	}
	
	/**
	 * Constructing from an existing vertex
	 * @param aVertex
	 */
	public Vertex(Vertex aVertex) {
		this.longitude = aVertex.longitude;
		this.latitude = aVertex.latitude;
		this.id = aVertex.id;
		this.xy = new Point2D.Double(aVertex.getX(), aVertex.getY());
	}

	/**
	 * Adds an edge () from this vertex to a specified vertex 
	 * with a specified distance and speed limit.
	 * 
	 * @param i The vertex the  goes to
	 * @param distance The distance of the  (generally just the distance 
	 *                  between the two vertices
	 * @param speed The speed limit on the  between the vertices
	 */
	public void addEdge (Vertex i, double distance, double speed) {
		if (this.id == i.id) {
			return;
		}
		Link r = new Link(this, i, distance, speed);
		linksMapFrom.put(i, r);
		i.linksMapTo.put(this, r);
	}

	/**
	 * Removes the edge () between this vertex and the specified
	 * vertex
	 * 
	 * @param inter The vertex that the removed  goes to
	 * @throws IllegalArgumentException if there is not  between this 
	 *          vertex and the specified vertex
	 */
	public void removeEdge (Vertex inter) throws IllegalArgumentException {
		for (Vertex i : linksMapFrom.keySet()) {
			if (i.equals(inter)) {
				linksMapFrom.remove(i);
				i.linksMapTo.remove(this);
				return;
			}
		}
		throw new IllegalArgumentException("Trying to remove " +
				" that doesn't exist.");
	}

	/**
	 * Removes this vertex and reconnects all the neighbors such that the
	 * graph remains the same minus this vertex.
	 */
	public void cutVertex () {
		for (Link From : linksMapFrom.values()) {
			for (Link To : linksMapTo.values()) {
				To.from.addEdge(From.to, From.length + 
						To.length, Math.min(From.speed, To.speed));
			}
		}

		severVertex();
	}

	/**
	 * Removes this vertex by cutting all the incoming and outgoing links.
	 */
	public void severVertex() {
		boolean check = true;
		while (check) {
			check = false;
			for (Vertex i : linksMapFrom.keySet()) {
				removeEdge(i);
				check = true;
				break;
			}
		}

		check = true;
		while (check) {
			check = false;
			for (Vertex i : linksMapTo.keySet()) {
				i.removeEdge(this);
				check = true;
				break;
			}
		} 
	}

	/**
	 * Checks if this vertex and the specified vertex are neighbors,
	 * i.e. if there is a  from this to the other or from the other to this.
	 * 
	 * @param i The vertex to check
	 * @return true if there is a  between this and i and false otherwise.
	 */
	public boolean isAdjacent (Vertex i) {
		return (linksMapFrom.keySet().contains(i) || linksMapTo.keySet().contains(i));
	}

	/**
	 * Return the  from this vertex to the specified vertex.
	 * 
	 * @param i The vertex that the  goes to
	 * @return The  between this and the other vertex
	 * @throws IllegalArgumentException if there is no  between this and
	 *          the other
	 */
	public Link To (Vertex i) throws IllegalArgumentException {
		if (linksMapFrom.keySet().contains(i)) {
			return linksMapFrom.get(i);
		}
		throw new IllegalArgumentException("no  between " +
				"this and i");
	}

	/**
	 * Return a set of all the links going from this vertex to some
	 * other vertex
	 * 
	 * @return a set of links from this vertex to other vertices 
	 */
	public Set<Link> getLinksFrom () {
		return new TreeSet<>(linksMapFrom.values());
	}

	/**
	 * Return a set of all the links going from some vertex to this
	 * vertex
	 * 
	 * @return a set of links going to this vertex 
	 */
	public Set<Link> getLinksTo () {
		return new TreeSet<>(linksMapTo.values());
	}

	/**
	 * Return a set of Vertices that you can directly go to from 
	 * this vertex, i.e. there exists a  from this vertex 
	 * to every vertex in the returned set
	 * 
	 * @return a set of vertices that you can go to from this vertex
	 */
	public Set<Vertex> getAdjacentFrom () {
		return linksMapFrom.keySet();
	}

	/**
	 * Return a set of Vertices from which you can directly go to 
	 * this vertex, i.e. there exists a  from every vertex
	 * in the returned set to this vertex
	 * 
	 * @return a set of vertices from which you can directly go to 
	 *          this vertex
	 */
	public Set<Vertex> getAdjacentTo () {
		return linksMapTo.keySet();
	}

	/**
	 * Checks if the given vertex is the same as this vertex
	 * 
	 * @param inter The given vertex to check
	 * @return true if the given vertex is the same as this vertex
	 */
	public boolean equals(Vertex inter) {
		return (inter.id == this.id);
	}

	/**
	 * returns the Euclidean distance from this vertex to the specified vertex
	 * 
	 * @param vertex specified vertex
	 * @return distance between this vertex and specified vertex
	 */
	public double distanceTo(Vertex vertex) {
		return this.xy.distance(vertex.xy);
	}

	/**
	 * returns the x-coordinate of this vertices
	 * @return x-coordinate
	 */
	public double getX() {
		return this.xy.getX();
	}

	/**
	 * returns the y-coordinate of this vertices
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
	 * checks whether this is the same vertex as some specified vertex
	 * 
	 * @param other vertex
	 * @return 0 is they are the same, 1 if {@code this.id > other.id} else -1
	 */
	public int compareTo(Vertex other) {
		if (id == other.id)
			return 0;
		else if (id < other.id)
			return -1;
		else
			return 1;
	}

}
