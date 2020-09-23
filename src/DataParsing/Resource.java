package DataParsing;

import COMSETsystem.LocationOnRoad;

/**
 *
 * @author TijanaKlimovic
 */
public class Resource extends TimestampAbstract {

	private final double dropffLat; // drop-off latitude of resource
	private final double dropffLon; // drop-off longitude of resource

	private LocationOnRoad pickupLocation; // pickup location on road
	private LocationOnRoad dropffLocation; // dropff location on road
	private final long dropffTime; // drop-off time of resource from raw data

	//constructor initiating each field of the Resource record type
	public Resource(double pickupLat, double pickupLon, double dropffLat, double dropffLon, long time, long dropffTime) {
		super(pickupLat, pickupLon, time);
		this.dropffLat = dropffLat;
		this.dropffLon = dropffLon;
		this.dropffTime = dropffTime;

	}

	/**
	 * returns the dropff latitude of the resource
	 * 
	 * @return {@code this.dropffLat}
	 */
	public double getDropoffLat() {
		return dropffLat;
	}

	/**
	 * returns the dropff longitude of the resource
	 * 
	 * @return {@code this.dropffon}
	 */
	public double getDropoffLon() {
		return dropffLon;
	}

	public void setPickupLocation(LocationOnRoad pickupLocation) { this.pickupLocation = pickupLocation; }

	public void setDropoffLocation(LocationOnRoad dropffLocation) { this.dropffLocation = dropffLocation; }

	public LocationOnRoad getPickupLocation() { return pickupLocation; }

	public LocationOnRoad getDropoffLocation() { return dropffLocation; }

	public long getPickupTime() { return this.getTime(); }

	public long getDropoffTime() { return dropffTime; }
}
