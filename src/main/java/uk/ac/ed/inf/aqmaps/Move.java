package uk.ac.ed.inf.aqmaps;

import com.mapbox.geojson.Point;

/**
 * An abstract representation of a move that a drone can make.
 *
 */
public class Move {

	private int direction;
	private SensorReading sensor; // sensor that the drone should expect to hit after this move
	
	// a Move knows where it's coming from and where it ends up
	private Point original;
	private Point next;
	
	public Move(Point original, int direction, SensorReading sensor) {
		this.original = original;
		this.direction = direction;
		this.sensor = sensor;
		
		var new_lon = original.longitude() + PathPlanner.MOVE_LENGTH * Math.cos(Math.toRadians((double) direction));
		var new_lat = original.latitude() + PathPlanner.MOVE_LENGTH * Math.sin(Math.toRadians((double) direction));
		next = Point.fromLngLat(new_lon, new_lat);
	}

	public int getDirection() {
		return direction;
	}

	public SensorReading getSensor() {
		return sensor;
	}

	public Point getOriginal() {
		return original;
	}

	public Point getNext() {
		return next;
	}
}
