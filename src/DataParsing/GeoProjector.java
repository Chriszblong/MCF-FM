package DataParsing;
import COMSETsystem.Intersection;
import java.lang.Math;

/**
 * @author Bo Xu
 * mail bo.5.xu@here.com
 *
 * The GeoProjector class projects a lat,lon location to a point in 2D space. Suitable for a 
 * small geographic area (e.g., a city) which can be considered flat.
 */
public class GeoProjector {
	public final static double EARTH_RADIUS = 6370000.0; // Earth radius in meters

	// A reference location which can be any location in the considered geographic area.
	final double ref_lat;
	final double ref_lon;

	final double metersPerLatDegree;
	final double metersPerLonDegree;

	/**
	 * Constructor of class. The parameters specify a reference location which can be any location 
	 * in the considered geographic area.
	 * 
	 * @param ref_lat latitude of a local position 
	 * @param ref_lon longitude of a local position
	 */	
	public GeoProjector(double ref_lat, double ref_lon) {
		this.ref_lat = ref_lat;
		this.ref_lon = ref_lon;

		metersPerLatDegree = distanceGreatCircle(ref_lat, ref_lon, ref_lat + 1.0, ref_lon);
		metersPerLonDegree = distanceGreatCircle(ref_lat, ref_lon, ref_lat, ref_lon + 1.0);
	}

	/**
	 * Project a lat,lon location to 2D space
	 * @param lat latitude 
	 * @param lon longitude
	 * @return projected 2D point 
	 */		
	public double[] fromLatLon(double lat, double lon) {
		double x = (lon - ref_lon) * metersPerLonDegree;
		double y = (lat - ref_lat) * metersPerLatDegree;
		double[] xy = {x, y};
		return xy;
	}

	/**
	 * Project a lat,lon location to 2D space
	 * @param lat latitude 
	 * @param lon longitude
	 * @return projected 2D point 
	 */		
	public double[] toLatLon(double x, double y) {
		double lon = ref_lon + (x / metersPerLonDegree);
		double lat = ref_lat + (y / metersPerLatDegree);
		double[] latLon = {lat, lon};
		return latLon;
	}

	/**
	 * Compute great-circle distance between two locations on earth modeled as a sphere.  
	 * @param lat1 latitude of the first location
	 * @param lon1 longitude of the first location
	 * @param lat2 latitude of the second location
	 * @param lon2 longitude of the second location
	 * @return great-circle distance in meters
	 */		
	public static double distanceGreatCircle(double lat1, double lon1, double lat2, double lon2) {
		double rad_lat1 = Math.toRadians(lat1);
		double rad_lon1 = Math.toRadians(lon1);
		double rad_lat2 = Math.toRadians(lat2);
		double rad_lon2 = Math.toRadians(lon2);
		double q1 = Math.cos(rad_lat1) * Math.cos(rad_lon1) * Math.cos(rad_lat2) * Math.cos(rad_lon2);
		double q2 = Math.cos(rad_lat1) * Math.sin(rad_lon1) * Math.cos(rad_lat2) * Math.sin(rad_lon2);
		double q3 = Math.sin(rad_lat1) * Math.sin(rad_lat2);
		double q = q1+q2+q3;
		if (q > 1.0)
			q = 1.0;
		if (q < -1.0)
			q = -1.0;
		return (Math.acos(q) * EARTH_RADIUS);
	}
}
