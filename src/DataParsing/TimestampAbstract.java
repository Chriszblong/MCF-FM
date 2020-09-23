package DataParsing;

/**
 * Record data type that represents a state of an agent(or resource) in system.
 * A state is defined by the latitude, longitude at which the agent was situated at a specific time.
 * Used to parse datasets. Each agent in a dataset has many timestamps.
 *
 * A timestamp consists of a latitude, longitude, whether the agent was available and the time.
 */

public abstract class TimestampAbstract {


	private final double pickupLat; // latitude at which resource appears
	private final double pickupLon; // longitutde at which agent appears
	private final long time;   // time at which the agent/resource was at (lon,lat) position on map



	public TimestampAbstract(double pickupLat, double pickupLon, long time) {
		this.pickupLat = pickupLat;
		this.pickupLon = pickupLon;
		this.time = time;
	}

	/**
	 * returns the pickup latitude 
	 * 
	 * @return {@code this.pickupLat}
	 */
	public double getPickupLat() {
		return pickupLat;
	}

	/**
	 * returns the pickup longitude
	 * 
	 * @return {@code this.pickupLon} 
	 */
	public double getPickupLon() {
		return pickupLon;
	}

	/**
	 * returns the time
	 * 
	 * @return {@code this.time}
	 */
	public long getTime() {
		return time;
	}   

}