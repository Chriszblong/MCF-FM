package DataParsing;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import MapCreation.MapCreator;

/**       
 * The CSVNewYorkParser class parses a New York TLC data file for a month before July of 2016.
 * The following columns are extracted from each row to create a Resource object.
 * 
 * 1. "tpep_pickup_datetime": This time stamp is treated as the time at which the resource (passenger) 
 *    is introduced to the system.
 * 2. "pickup_longitude", "pickup_latitude": The location at which the resource (passenger) is introduced.
 * 3. "dropoff_longitude", "dropoff_latitude": The location at which the resource (passenger) is dropped off. 
 *   
 * @author TijanaKlimovic
 */
public class CSVNewYorkParser {

	// absolute path to csv file to be parsed
	private final String path;

	// list of all resources
	private final ArrayList<Resource> resources = new ArrayList<>();

	DateTimeFormatter dtf;

	ZoneId zoneId;

	/**
	 * Constructor of the CSVNewYorkParser class
	 * @param path full path to the resource dataset file
	 * @param zoneId the time zone id of the studied area
	 */
	// resource specified in csv file located at path
	public CSVNewYorkParser(String path, ZoneId zoneId) {
		this.path = path;
		dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		// TLC Trip Record data uses local time. So the zone ID is America/New_York
		this.zoneId = zoneId;
	}

	/**
	 * Converts the date+time (timestamp) string into the Linux epoch.
	 *
	 * @param timestamp string containing formatted date and time data to be
	 * converted
	 * @return long value of the timestamp string
	 */
	private Long dateConversion(String timestamp) {
		long l;
		LocalDateTime ldt = LocalDateTime.parse(timestamp, dtf);
		ZonedDateTime zdt = ZonedDateTime.of(ldt, zoneId);
		l = zdt.toEpochSecond(); //Returns Linux epoch, i.e., the number of seconds since January 1, 1970, 00:00:00 GMT until time specified in zdt
		return l;
	}

	/**
	 * Parse the csv file.
	 * 
	 * @return ArrayList<Resource>
	 */
	public ArrayList<Resource> parse(long timeResolution) {

		try {
			Scanner sc = new Scanner(new File(path));   //scanner will scan the file specified by path
			sc.useDelimiter(",|\n");    //scanner will skip over "," and "\n" found in file
			sc.nextLine(); // skip the header

			//while there are tokens in the file the scanner will scan the input
			//each line in input file will contain 4 tokens for the scanner and will be in the format : latitude longitude time type
			//per line of input file we will create a new TimestampAgRe object
			// and save the 4 tokens of each line in the corresponding field of the TimestampAgRe object
			int resource_count = 0;
			while (sc.hasNext()) {
				sc.next();// skip first VendorID
				long time = dateConversion(sc.next()) * timeResolution;
				long dropoffTime = dateConversion(sc.next()) * timeResolution;
				sc.next();// skip these fields
				sc.next();
				double pickupLon = Double.parseDouble(sc.next());
				double pickupLat = Double.parseDouble(sc.next());
				sc.next();// skip these fields
				sc.next();
				double dropoffLon = Double.parseDouble(sc.next());
				double dropoffLat = Double.parseDouble(sc.next());
				sc.nextLine(); //skip rest of fileds in this line
				// Only keep the resources such that both pickup location and dropoff location are within the bounding polygon.
				if (!(MapCreator.insidePolygon(pickupLon, pickupLat) && MapCreator.insidePolygon(dropoffLon, dropoffLat))) {
					continue;
				}
				if (pickupLat == dropoffLat && pickupLon == dropoffLon) {
					continue;
				}
				resources.add(new Resource(pickupLat, pickupLon, dropoffLat, dropoffLon, time, dropoffTime)); //create new resource with the above fields
				resource_count += 1;
				//if (resource_count == 100)
				//	break;
			}
			sc.close();
		} catch (Exception e) {

			e.printStackTrace();
		}
		return resources;
	}

}
