package COMSETsystem;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeMap;

import com.google.common.collect.ImmutableList;
import net.iakovlev.timeshape.TimeZoneEngine;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import DataParsing.GeoProjector;
import DataParsing.KdTree;

import java.util.PriorityQueue;
import java.awt.geom.Point2D;
import java.time.ZoneId;


/**
 * The CityMap represents the map of a city.
 * The map is represented as a directed graph of intersections connected by roads.
 * (See Intersection and Road class for more details).
 */
public class CityMap {

	// A mapping from all the intersection ids to corresponding Intersections
	private Map<Long, Intersection> intersections;
	
	// A list of roads
	private List<Road> roads;

	// A projector to convert between lat,lon coordinates and xy coordinates.
	private GeoProjector projector;

	// kdTree for map matching
	private KdTree kdTree;

	// Shortest travel-time path table.
	private ImmutableList<ImmutableList<PathTableEntry>> immutablePathTable;
	
	// A map from an intersection's path table index to the intersection itself.
	private HashMap<Integer, Intersection> intersectionsByPathTableIndex;

	/*
	 * Constructor of CityMap
	 */
	public CityMap(Map<Long, Intersection> intersections, List<Road> roads, 
			GeoProjector projector, KdTree kdTree) {
		this.intersections = intersections;
		this.projector = projector;
		this.kdTree = kdTree;
		this.roads = roads;

		// setup pathTableIndex for every intersection
		intersectionsByPathTableIndex = new HashMap<Integer, Intersection>();
		int index = 0;
		for (Intersection intersection : intersections.values()) {
			intersection.pathTableIndex = index++;
			intersectionsByPathTableIndex.put(intersection.pathTableIndex, intersection);
		}

	}

	
	/**
	 * Create an empty CityMap object for making a copy.
	 * See makeCopy().
	 */
	public CityMap() {}

	/**
	 * Gets the time it takes to move from one intersection to the next
	 * intersection.
	 *
	 * Warning: This function assumes traversal at the speed limits of the roads; the computed travel time
	 * may be different than the actual travel time.
	 *
	 * @param source The intersection to depart from
	 * @param destination The intersection to arrive at
	 * @return the time in seconds it takes to go from source to destination
	 */
	public double travelTimeBetween (Intersection source, Intersection destination) {
		return immutablePathTable.get(source.pathTableIndex).get(destination.pathTableIndex).travelTime;
	}


	/**
	 * Gets the time it takes to move from a location on a first road to a location on a second road. 
	 *
	 * Warning: This function assumes traversal at the speed limit of the roads; the computed travel time
	 * may be different than the actual travel time.
	 *
	 * @param source The location to depart from
	 * @param destination The location to arrive at
	 * @return the time in seconds it takes to go from source to destination
	 */
	public long travelTimeBetween (LocationOnRoad source, LocationOnRoad destination) {
		double travelTime = -1;

		if (source.road == destination.road && source.getDisplacementOnRoad(destination) >= 0) {
			// If the two locations are on the same road and source is closer to the start intersection than destination, 
			// then the travel time is the difference of travelTimeFromStartIntersection between source and destination.
			travelTime = travelTime = source.getDisplacementOnRoad(destination) / source.road.speed;
		} else {
			LocationOnRoad endIntersectionOfSource = new LocationOnRoad(source.road, source.road.length);
			double travelTimeToEndIntersectionOfSource = source.getDisplacementOnRoad(endIntersectionOfSource) / source.road.speed;
			LocationOnRoad startIntersectionOfDestination = new LocationOnRoad(destination.road, 0);
			double travelTimeFromStartIntersectionOfDestination = startIntersectionOfDestination.getDisplacementOnRoad(destination) / destination.road.speed;
			double travelTimeFromEndIntersectionOfSourceToStartIntersectionOfDestination = travelTimeBetween(source.road.to, destination.road.from);
			travelTime = travelTimeToEndIntersectionOfSource + travelTimeFromEndIntersectionOfSourceToStartIntersectionOfDestination + travelTimeFromStartIntersectionOfDestination;
		}
		return Math.round(travelTime);
	}
	
	/**
	 * @return { @code projector }
	 */
	public GeoProjector projector() {
		return projector;
	}

	/**
	 * Finds nearest link of a point defined by the
	 * { @code longitude, latitude }.
	 *
	 * @param longitude The longitude of the point
	 * @param latitude The latitude of the point
	 * @return The closest link to the given point
	 */
	public Link getNearestLink(double longitude, double latitude){
		double[] xy = projector.fromLatLon(latitude, longitude);
		Point2D p = new Point2D.Double(xy[0], xy[1]);
		return kdTree.nearest(p);    	
	}

	/**
	 * Compute all-pair shortest travel times. This is done by computing one-to-all shortest travel times
	 * from each intersection using Dijkstra.
	 */
	public void calcTravelTimes() {
		ArrayList<ArrayList<PathTableEntry>> pathTable;

		// initialize path table
		pathTable = new ArrayList<ArrayList<PathTableEntry>>();
		for (int i = 0; i < intersections.size(); i++) {
			ArrayList<PathTableEntry> aList = new ArrayList<PathTableEntry>();
			for (int j = 0; j < intersections.size(); j++) {
				aList.add(null);
			}
			pathTable.add(aList);
		}

		// creates a queue entry for each intersection
		HashMap<Intersection, DijkstraQueueEntry> queueEntry = new HashMap<>();
		for (Intersection i : intersections.values()) {
			queueEntry.put(i, new DijkstraQueueEntry(i));
		}

		for (Intersection source : intersections.values()) {
			// 'reset' every queue entry
			for (DijkstraQueueEntry entry : queueEntry.values()) {
				entry.cost = Double.MAX_VALUE;
				entry.inQueue = true;
			}

			// source is set at distance 0
			DijkstraQueueEntry sourceEntry = queueEntry.get(source);
			sourceEntry.cost = 0;
			pathTable.get(source.pathTableIndex).set(source.pathTableIndex, new PathTableEntry(0L, source.pathTableIndex));

			PriorityQueue<DijkstraQueueEntry> queue = new PriorityQueue<>(queueEntry.values());

			while (!queue.isEmpty()) {
				DijkstraQueueEntry entry = queue.poll();
				entry.inQueue = false;

				for (Road r : entry.intersection.getRoadsFrom()) {
					DijkstraQueueEntry v = queueEntry.get(r.to);
					if (!v.inQueue) continue;
					double ncost = entry.cost + r.travelTime;
					if (v.cost > ncost) {
						queue.remove(v);
						v.cost = ncost;
						pathTable.get(source.pathTableIndex).set(v.intersection.pathTableIndex, new PathTableEntry(v.cost, entry.intersection.pathTableIndex));
						queue.add(v);
					}
				}
			}
		}

		// Make the path table unmodifiable
		makePathTableUnmodifiable(pathTable);
	}

	/**
	 * Make a path table unmodifiable.
	 * @param pathTable
	 */
	public void makePathTableUnmodifiable(ArrayList<ArrayList<PathTableEntry>> pathTable) {
		ArrayList<ImmutableList<PathTableEntry>> aListOfImmutableList = new ArrayList<ImmutableList<PathTableEntry>>();
		for (int i = 0; i < intersections.size(); i++) {
			ArrayList<PathTableEntry> aListOfPathTableEntries = pathTable.get(i);
			aListOfImmutableList.add(ImmutableList.copyOf(aListOfPathTableEntries));
			aListOfPathTableEntries.clear();
		}
		pathTable.clear();

		immutablePathTable = ImmutableList.copyOf(aListOfImmutableList);
	}

	/**
	 * Get the shortest path between a given source and a given destination
	 * @param source the source intersection
	 * @param destination the destination intersection
	 * @return LinkedList<Intersection> an ordered list of intersections forming the path
	 */
	public LinkedList<Intersection> shortestTravelTimePath(Intersection source, Intersection destination) {
		LinkedList<Intersection> path = new LinkedList<Intersection>();
		path.addFirst(destination);
		int current = destination.pathTableIndex;
		while (current != source.pathTableIndex) {
			int pred = immutablePathTable.get(source.pathTableIndex).get(current).predecessor;
			path.addFirst(intersectionsByPathTableIndex.get(pred));
			current = pred;
		}
		return path;
	}

	private class DijkstraQueueEntry implements Comparable<DijkstraQueueEntry> {
		Intersection intersection;
		double cost = Double.MAX_VALUE;
		boolean inQueue = true;

		DijkstraQueueEntry(Intersection intersection) {
			this.intersection = intersection;
		}

		@Override
		public int compareTo(DijkstraQueueEntry j) {
			if (this.cost < j.cost) {
				return -1;
			} else if (this.cost > j.cost) {
				return 1;
			} else if (this.intersection.id < j.intersection.id) {
				return -1;
			} else if (this.intersection.id > j.intersection.id) {
				return 1;
			} else {
				return 0;
			}
		}
	}

	private class PathTableEntry {
		final double travelTime;
		final int predecessor;

		PathTableEntry(double travelTime, int predecessor) {
			this.travelTime = travelTime;
			this.predecessor = predecessor;
		}
	}

	/**
	 * @return { @code roads }
	 */
	public List<Road> roads() {
		return roads;
	}

	/**
	 * @return { @code intersections }
	 */	
	public Map<Long, Intersection> intersections() {
		return intersections;
	}

	/**
	 * 
	 * @return a deep copy of the map
	 */
	public CityMap makeCopy() {
		// deeply copy intersections
		Map<Long, Vertex> verticesCopy = new TreeMap<Long, Vertex>();
		Map<Long, Intersection> intersectionsCopy = new TreeMap<Long, Intersection>();
		for (Intersection intersection : intersections.values()) {
			for (Road road : intersection.roadsMapFrom.values()) {
				ArrayList<Link> linksCopy = new ArrayList<Link>();
				for (Link link : road.links) {
					// create start vertex if not existing yet
					if (!verticesCopy.containsKey(link.from.id)) {
						// create a new vertex
						Vertex vertexFrom = new Vertex(link.from); 
						verticesCopy.put(vertexFrom.id, vertexFrom);
					}				
					// create end vertex if not existing yet
					if (!verticesCopy.containsKey(link.to.id)) {
						// create a new vertex
						Vertex vertexTo = new Vertex(link.to);
						verticesCopy.put(vertexTo.id, vertexTo);
					}
					// create a link
					Vertex vertexFrom = verticesCopy.get(link.from.id);
					Vertex vertexTo = verticesCopy.get(link.to.id);
					Link linkCopy = new Link(link, vertexFrom, vertexTo);
					vertexFrom.linksMapFrom.put(vertexTo, linkCopy);
					vertexTo.linksMapTo.put(vertexFrom, linkCopy);
					linksCopy.add(linkCopy);
				}
				Link firstLink = linksCopy.get(0);

				// create start intersection if not existing yet
				if (!intersectionsCopy.containsKey(road.from.id)) {
					// create a new intersection
					Intersection intersectionFrom = new Intersection(road.from);
					intersectionFrom.vertex = firstLink.from;
					firstLink.from.intersection = intersectionFrom;
					intersectionsCopy.put(intersectionFrom.id, intersectionFrom);
				}
				Link lastLink = linksCopy.get(linksCopy.size()-1);
				// create end intersection if not existing yet
				if (!intersectionsCopy.containsKey(road.to.id)) {
					// create a new intersection
					Intersection intersectionTo = new Intersection(road.to);
					intersectionTo.vertex = lastLink.to;
					lastLink.to.intersection = intersectionTo;
					intersectionsCopy.put(intersectionTo.id,  intersectionTo);
				}
				// create a road
				Intersection intersectionFrom = intersectionsCopy.get(road.from.id);
				Intersection intersectionTo = intersectionsCopy.get(road.to.id);
				Road roadCopy = new Road(road, intersectionFrom, intersectionTo, linksCopy);
				for (Link linkCopy : linksCopy) {
					linkCopy.road = roadCopy;
				}
				intersectionFrom.roadsMapFrom.put(intersectionTo,  roadCopy);
				intersectionTo.roadsMapTo.put(intersectionFrom, roadCopy);
			}
		}

		// now create road list
		List<Road> roadsCopy = new ArrayList<>();
		for (Intersection inter : intersectionsCopy.values()) {
			for (Road road : inter.getRoadsFrom()) {
				roadsCopy.add(road);
			}
		}

		CityMap cityMap = new CityMap();
		cityMap.intersections = intersectionsCopy;
		cityMap.roads = roadsCopy;
		cityMap.immutablePathTable = immutablePathTable;
		cityMap.projector = projector;
		cityMap.kdTree = kdTree;
		
		cityMap.intersectionsByPathTableIndex = new HashMap<Integer, Intersection>();
		for (Intersection intersection : cityMap.intersections.values()) {
			cityMap.intersectionsByPathTableIndex.put(intersection.pathTableIndex, intersection);
		}
		
		return cityMap;
	}
	
	/**
	 * Compute the time zone ID of the map based on an arbitrary location of the map.
	 * It is assumed that the entire map falls into a single time zone. In other words,
	 * the map should not cross more than one time zones.
	 * @return the time zone ID of the map
	 */
	public ZoneId computeZoneId() {
		// get an arbitrary location of the map
		Intersection intersection = intersections.values().toArray(new Intersection[intersections.size()])[0];
		// get the time zone id
		Logger.getRootLogger().setLevel(Level.OFF); // Do this just so that there is no warning message. 
		TimeZoneEngine engine = TimeZoneEngine.initialize();
		Optional<ZoneId> zoneId = engine.query(intersection.latitude, intersection.longitude);

		return zoneId.get();
	}

}
