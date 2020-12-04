package uk.ac.ed.inf.aqmaps;

import com.mapbox.geojson.Point;

/** An abstract representation of a move that the drone makes.
 */
public class Move {

	// a Move knows where it's coming from and where it ends up
	private final Point original;
	private final Point next;
	// the direction of motion as a multiple of 10 degrees
	private final int direction;
	// representation of the (potential) sensor reading
	private final SensorReading sensor;
	
	public Move(Point original, int direction, Point next, SensorReading sensor) {
		this.original = original;
		this.next = next;
		this.direction = direction;
		this.sensor = sensor;
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
	
	public String serialize(int moveNumber) {
		// format: {moveNumber},{original longitude},{original latitude},{direction},{next longitude},{next latitude},{sensor location W3W}
		return String.format("%d,%f,%f,%d,%f,%f,%s\n",
				moveNumber, original.longitude(), original.latitude(), 
				direction, next.longitude(), next.latitude(),
				sensor == null ? "null" : sensor.location);
	}
}
