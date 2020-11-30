package uk.ac.ed.inf.aqmaps;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;

public class DroneController {

	// Flight boundaries
	// TODO keywords?
	public static final double LAT_MAX = 55.946233; // latitude
	public static final double LAT_MIN = 55.942617;
	public static final double LON_MAX = -3.184319; // longitude
	public static final double LON_MIN = -3.192473;
	public static final double MOVE_LENGTH = 0.0003; // length of EVERY drone step in degrees
	public static final double SENSOR_READ_MAX_DISTANCE = 0.0002; // maximum distance from a sensor to receive data - the drone has to be STRICTLY closer than this
	
	private final ArrayList<SensorReading> sensors;
	private final String flightpathFilename;
	private final String readingsMapFilename;
	
	private int battery = 150;
	private double latitude, longitude;
	
	private ArrayList<Move> trajectory; // TODO move into executePathPlan?
	
	public DroneController(ArrayList<SensorReading> sensors, String flightpathFilename, String readingsMapFilename) {
		this.sensors = sensors;
		this.flightpathFilename = flightpathFilename;
		this.readingsMapFilename = readingsMapFilename;
	}

	public void executePathPlan(ArrayList<Point> waypoints) {
		trajectory = new ArrayList<Move>();
		
		var start = waypoints.get(0);
		longitude = start.longitude();
		latitude = start.latitude();
		
		for (int i = 1; i < waypoints.size(); i++) {
			// starting at i = 1, because waypoint number 0 is the starting location
			var target = waypoints.get(i);
			
			// TODO battery!!
			
			// approach the target
			// will try to get close to the target, as it could be a sensor
			while (droneDistance(target) >= SENSOR_READ_MAX_DISTANCE) {
				// real angle between current location and target
				var theta = Math.toDegrees(Math.atan2(target.latitude() - latitude, target.longitude() - longitude));
				// allowed angle: multiple of 10, also within [0,350] (no negatives)
				var phi = (10 * (int) Math.round(theta / 10) + 360) % 360;
				
				// take a step in the phi direction
				var next_longitude = longitude + MOVE_LENGTH * Math.cos(Math.toRadians(phi));
				var next_latitude = latitude + MOVE_LENGTH * Math.sin(Math.toRadians(phi));
				
				// check if there is a sensor there
				SensorReading sensor = null;
				for (var s : sensors) {
					if (droneDistance(s) < SENSOR_READ_MAX_DISTANCE) {
						sensor = s;
						break;
					}
				}
				
				// record the move
				trajectory.add(new Move(Point.fromLngLat(longitude, latitude), phi, Point.fromLngLat(next_longitude, next_latitude), sensor));
				
				// update the drone location and battery
				longitude = next_longitude;
				latitude = next_latitude;
				battery -= 1;
			}
		}
		
		System.out.println("Battery: " + Integer.toString(battery));
		serializeTrajectory();
	}

	private double droneDistance(Point pt) {
		return Math.hypot(pt.longitude() - longitude, pt.latitude() - latitude);
	}
	
	private double droneDistance(SensorReading sensor) {
		return Math.hypot(sensor.lon - longitude, sensor.lat - latitude);
	}
	
	private void serializeTrajectory() { // TODO name
		var unused_sensors = (ArrayList<SensorReading>) sensors.clone();
		
		
		// prepare for file writing
    	var flightpathFile = new File(flightpathFilename);
    	var readingsFile = new File(readingsMapFilename);
    	try {
    		// create the file if it doesn't exist already
			flightpathFile.createNewFile();
			// TODO maybe both files should be in a different try {}?
			readingsFile.createNewFile();
			
			var flightpathWriter = new FileWriter(flightpathFile);
			var readingsWriter = new FileWriter(readingsFile);
			
			var points = new ArrayList<Point>();
			var features = new ArrayList<Feature>();
			
			for (int i = 0; i < trajectory.size(); i++) {
				var move = trajectory.get(i);
				flightpathWriter.write(move.serialize(i+1));
				points.add(move.getOriginal());
				
				var sensor = move.getSensor();
				if (sensor != null) {
					var marker = Feature.fromGeometry(sensor.toPoint());
					// TODO actual properties
					marker.addStringProperty("marker-color", "#cc0000");
					features.add(marker);
					unused_sensors.remove(sensor);
				}
				
			}
			
			// end point of last move
			points.add(trajectory.get(trajectory.size() - 1).getNext());
			
			for (var sensor : unused_sensors) {
				var marker = Feature.fromGeometry(sensor.toPoint());
				marker.addStringProperty("marker-color", "#aaaaaa");
				features.add(marker);
			}
			
			features.add(Feature.fromGeometry(LineString.fromLngLats(points)));
			var collection = FeatureCollection.fromFeatures(features);
			
			readingsWriter.write(collection.toJson());
			
			flightpathWriter.close();
			readingsWriter.close();

		} catch (IOException e) {
			// if the file could not be created or written
			e.printStackTrace();
		}
	}
}
