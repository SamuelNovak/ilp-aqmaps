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

	// drone parameters
	private final double MOVE_LENGTH = 0.0003; // length of EVERY drone step in degrees
	private final double SENSOR_READ_MAX_DISTANCE = 0.0002; // maximum distance from a sensor to receive data - the drone has to be STRICTLY closer than this
	private final int MAX_BATTERY = 150;
	
	private final ArrayList<SensorReading> sensors;
	
	private int battery = MAX_BATTERY;
	private double latitude, longitude;
	
	private ArrayList<Move> trajectory;
	
	private ObstacleEvader evader;
	// private RotationDirection avoidingDirection = RotationDirection.None; // TODO rename to evasionDirection
	
	public DroneController(ArrayList<SensorReading> sensors, ObstacleEvader evader) {
		this.sensors = sensors;
		this.evader = evader;
	}

	public void executePathPlan(ArrayList<Point> waypoints) {
		// initialize state
		trajectory = new ArrayList<Move>();
		battery = MAX_BATTERY;
		
		var start = waypoints.get(0);
		longitude = start.longitude();
		latitude = start.latitude();
		
		
		int waypointIndex = 1;
		Point target;
		var evasionDirection = RotationDirection.None;
		
		while (waypointIndex < waypoints.size()) {
			target = waypoints.get(waypointIndex);
			if (droneDistance(target) < SENSOR_READ_MAX_DISTANCE) {
				// waypoint reached - navigate towards the next waypoint
				waypointIndex++;
				continue;
			}
			
			// real angle between current location and target
			var targetAngle = Math.toDegrees(Math.atan2(target.latitude() - latitude, target.longitude() - longitude));
			// allowed angle: multiple of 10, also within [0,350] (no negatives)
			var moveAngle = (10 * (int) Math.round(targetAngle / 10) + 360) % 360;
			
			var here = Point.fromLngLat(longitude, latitude);
			// take a step in the phi direction
			var next = computeMove(moveAngle);
			
			// get the nearest obstacle crossed by this move, if any (taking nearest in case there are multiple)
			var obstacle = moveIntersectsObstacle(next);
			if (obstacle != null) {
				if (evasionDirection.equal(RotationDirection.None))
					evasionDirection = evader.chooseEvasionDirection(obstacle, here, targetAngle);

				while (obstacle != null) {
					moveAngle = (moveAngle + 360 + evasionDirection.getValue() * 10) % 360;
					next = computeMove(moveAngle);
					obstacle = moveIntersectsObstacle(next);
				}
				// found a place to go

			} else {
				evasionDirection = RotationDirection.None;
			}
			
			// store original location
			var old_longitude = longitude;
			var old_latitude = latitude;
			
			// update the drone location and battery
			longitude = next.longitude();
			latitude = next.latitude();
			battery -= 1;
			
			// check if there is a sensor there (in the new location)
			// if there is a sensor, its data will be stored in trajectory
			var sensorsInRange = getSensorsInRange();
			if (sensorsInRange == null) {
				trajectory.add(new Move(Point.fromLngLat(old_longitude, old_latitude), moveAngle, Point.fromLngLat(longitude, latitude), null));
			} else {
				SensorReading sensor = null;
				if (sensorsInRange.size() == 1) {
					// the easy case
					sensor = sensorsInRange.get(0);
				} else if (sensorsInRange.size() == 2) {
					// the more complex case TODO
					System.out.println("Two in range");
					sensor = sensorsInRange.get(0);
				} else {
					// panic TODO
					System.out.println("Way too many sensors in one location!");
					sensor = sensorsInRange.get(0);
				}
				
				trajectory.add(new Move(Point.fromLngLat(old_longitude, old_latitude), moveAngle, Point.fromLngLat(longitude, latitude), sensor));
			}
			
			// if battery is depleted after this move, drone has to stop
			if (battery == 0)
				break;
		}
		
		System.out.println("Drone journey finished.\nRemaining battery: " + Integer.toString(battery));
	}
	
	private Point computeMove(double angle) {
		var nextLongitude = longitude + MOVE_LENGTH * Math.cos(Math.toRadians(angle));
		var nextLatitude = latitude + MOVE_LENGTH * Math.sin(Math.toRadians(angle));
		var next = Point.fromLngLat(nextLongitude, nextLatitude);
		return next;
	}
	
	private ArrayList<Point> moveIntersectsObstacle(Point end) {
		var start = Point.fromLngLat(longitude, latitude);
		
		//return (!evader.crossesAnyObstacles(start, end));
		return evader.nearestCrossedObstacle(start, end);
	}
	
	private ArrayList<SensorReading> getSensorsInRange() {
		var sensorsInRange = new ArrayList<SensorReading>();
		for (var s : sensors) {
			if (droneDistance(s) < SENSOR_READ_MAX_DISTANCE) {
				sensorsInRange.add(s);
				break;
			}
		}
		if (sensorsInRange.isEmpty())
			return null;
		else
			return sensorsInRange;
	}

	private double droneDistance(Point pt) {
		return Math.hypot(pt.longitude() - longitude, pt.latitude() - latitude);
	}
	
	private double droneDistance(SensorReading sensor) {
		return Math.hypot(sensor.lon - longitude, sensor.lat - latitude);
	}
	
	public void serializeTrajectory(String flightpathFilename, String readingsMapFilename) {
		@SuppressWarnings("unchecked") // sensors is the correct type, so the result of clone is correct as well
		var unusedSensors = (ArrayList<SensorReading>) sensors.clone();
		
		// prepare for file writing
    	var flightpathFile = new File(flightpathFilename);
    	var readingsFile = new File(readingsMapFilename);
    	try {
    		// create the files if they don't exist already
			flightpathFile.createNewFile();
			readingsFile.createNewFile();
			
			// prepare file writers
			var flightpathWriter = new FileWriter(flightpathFile);
			var readingsWriter = new FileWriter(readingsFile);
			
			var trajectoryPoints = new ArrayList<Point>();
			var features = new ArrayList<Feature>();
			
			for (int i = 0; i < trajectory.size(); i++) {
				var move = trajectory.get(i);
				// write the move into flightpath and add its origin to the points representing trajectory
				flightpathWriter.write(move.serialize(i+1));
				trajectoryPoints.add(move.getOriginal());
				
				// check if there was a sensor
				var sensor = move.getSensor();
				if (sensor != null) {
					if (!unusedSensors.contains(sensor))
						// do not want the same sensor on the map multiple times
						continue;
					
					features.add(createSensorMarker(sensor, true));
					unusedSensors.remove(sensor);
				}
				
			}
			
			// add the end point of last move - the final point where drone stopped
			trajectoryPoints.add(trajectory.get(trajectory.size() - 1).getNext());
		
			// add any unvisited sensors to the map (marked as unvisited)
			for (var sensor : unusedSensors)
				features.add(createSensorMarker(sensor, false));
			
			// create LineString = the drone's trajectory
			features.add(Feature.fromGeometry(LineString.fromLngLats(trajectoryPoints)));
			
			// write JSON
			var collection = FeatureCollection.fromFeatures(features);
			readingsWriter.write(collection.toJson());
			
			flightpathWriter.close();
			readingsWriter.close();

		} catch (IOException e) {
			// if the file could not be created or written
			e.printStackTrace();
		}
	}
	
	private Feature createSensorMarker(SensorReading sensor, boolean visited) {
		var marker = Feature.fromGeometry(sensor.toPoint());
		
		// compute marker properties
		Pair<ColoursSymbols.Colour, ColoursSymbols.Symbol> properties;
		if (visited)
			properties = ColoursSymbols.getColourSymbol(sensor.reading, sensor.battery);
		else
			properties = ColoursSymbols.getNotVisited();
		
		var colour = properties.left;
		var symbol = properties.right;
		
		// assign marker properties
		marker.addStringProperty("location", sensor.location);
		marker.addStringProperty("rgb-string", colour.getValue());
		marker.addStringProperty("marker-color", colour.getValue());
		marker.addStringProperty("marker-symbol", symbol.getValue());
		
		return marker;
	}
}
