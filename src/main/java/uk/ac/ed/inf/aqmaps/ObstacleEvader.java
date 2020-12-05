package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;


public class ObstacleEvader {

	// Confinement area
	private final double LAT_MAX = 55.946233; // latitude
	private final double LAT_MIN = 55.942617;
	private final double LON_MAX = -3.184319; // longitude
	private final double LON_MIN = -3.192473;
	
	private ArrayList<Obstacle> noFlyZones;
	private Obstacle boundary;
	private HashMap<Obstacle, Point> averages;
	
	public ObstacleEvader(FeatureCollection noFlyZones) {		
		var zones = noFlyZones.features();
		// initialized with size
		averages = new HashMap<Obstacle, Point>(zones.size());
		
		this.noFlyZones = new ArrayList<Obstacle>();
		
		for (int i = 0; i < zones.size(); i++) {
			var polygon = (Polygon) zones.get(i).geometry();
			// get external coordinates (hence index 0)
			var obstacle = Obstacle.fromList(polygon.coordinates().get(0));
			this.noFlyZones.add(obstacle);
			averages.put(obstacle, getAveragePoint(obstacle));
		}
		
		// generate the boundary polygon
		boundary = new Obstacle();
		boundary.add(Point.fromLngLat(LON_MIN, LAT_MIN));
		boundary.add(Point.fromLngLat(LON_MAX, LAT_MIN));
		boundary.add(Point.fromLngLat(LON_MAX, LAT_MAX));
		boundary.add(Point.fromLngLat(LON_MIN, LAT_MAX));
		boundary.add(Point.fromLngLat(LON_MIN, LAT_MIN));
		
		// handle it exactly like other boundaries of no-fly zones - no-fly zone is everything on this boundary and outside, and it works exactly the same as buildings (uses intersections)
		this.noFlyZones.add(boundary);
		averages.put(boundary, getAveragePoint(boundary));
	}
	
	private Point getAveragePoint(List<Point> points) {
		double longitude = 0;
		double latitude = 0;
		
		for (var pt : points) {
			longitude += pt.longitude();
			latitude += pt.latitude();
		}
		
		return Point.fromLngLat(longitude / points.size(), latitude / points.size());
	}
	
	public boolean crossesAnyObstacles(Point a1, Point a2) {
		return !allCrossedObstacles(a1, a2).isEmpty();
	}
	
	public Obstacle nearestCrossedObstacle(Point origin, Point end) {		
		Obstacle nearestObstacle = null;
		// a distance will never be negative - using -1 to signify no value yet
		double minDistance = -1;
		
		for (var obs : noFlyZones) {
			var intersections = obstacleIntersections(origin, end, obs);
			for (var inter : intersections) {
				var dist = Math.hypot(origin.longitude() - inter.left.longitude(), origin.latitude() - inter.left.latitude());
				if (minDistance == -1 || dist < minDistance) {
					minDistance = dist;
					nearestObstacle = obs;
				}
			}
		}
		
		return nearestObstacle;
	}
	
	private ArrayList<Obstacle> allCrossedObstacles(Point a1, Point a2) {
		ArrayList<Obstacle> ret = new ArrayList<Obstacle>();
		for (var zone : noFlyZones) {			
			if (crossesOneObstacle(a1, a2, zone))
				ret.add(zone);
		}
		return ret;
	}
	
	private boolean crossesOneObstacle(Point a1, Point a2, Obstacle obstacle) {		
		return !obstacleIntersections(a1, a2, obstacle).isEmpty();
	}
	
	/** Find if and where a line intersects an obstacle (no fly zone).
	 * @param a1 Line start point
	 * @param a2 Line end point
	 * @param obstacle Obstacle
	 * @return list of pairs: left: intersection point;
	 * 						  right: corresponding index i, such that intersection occurred with line points[i] -- points[i+1] (mod points.size())
	 */
	private ArrayList<Pair<Point, Integer>> obstacleIntersections(Point a1, Point a2, Obstacle obstacle) {
		var intersections = new ArrayList<Pair<Point, Integer>>();
		// Iterate until the second-to-last, the last one needs to wrap around (path is cyclic) and so will be handled separately
		for (int i = 0; i < obstacle.size() - 1; i++) {
			var intsection = intersection(a1, a2, obstacle.get(i), obstacle.get((i+1) % obstacle.size()));
			if (intsection != null)
				// Found an intersection with a no-fly zone
				intersections.add(new Pair<Point, Integer>(intsection, i));
		}
		// Here handle the final line segment
		var intsection = intersection(a1, a2, obstacle.get(0), obstacle.get(obstacle.size() - 1));
		if (intsection != null)
			// Found an intersection
			intersections.add(new Pair<Point, Integer>(intsection, obstacle.size() - 1));
		
		return intersections;
	}
	
	/** Computes the point of intersection of the two lines
	 * @param a1 Start point of first line
	 * @param a2 End point of first line
	 * @param b1 Start point of second line
	 * @param b2 End point of second line
	 * @return
	 */
	private Point intersection(Point a1, Point a2, Point b1, Point b2) {
		var a1x = a1.longitude();
		var a1y = a1.latitude();
		var a2x = a2.longitude();
		var a2y = a2.latitude();
		var b1x = b1.longitude();
		var b1y = b1.latitude();
		var b2x = b2.longitude();
		var b2y = b2.latitude();
		
		double alpha, beta;
		
		try {
			alpha = ((a1x - b1x)*(b1y - b2y) - (a1y - b1y)*(b1x - b2x))/((a1x - a2x)*(b1y - b2y) - (a1y - a2y)*(b1x - b2x));
			beta = (-(a1x - a2x)*(a1y - b1y) + (a1x - b1x)*(a1y - a2y))/((a1x - a2x)*(b1y - b2y) - (a1y - a2y)*(b1x - b2x));
		} catch (Exception e) {
			// division by 0 => the lines are parallel
			// however we know they will never be the same line, so we can safely return no intersection
			return null;
		}
		
		if (alpha < 0 || alpha > 1 || beta < 0 || beta > 1)
			// there is no intersection (in the line segments)
			return null;
		
		// calculate the intersection point
		var x = b1x + beta*(-b1x + b2x);
		var y = b1y + beta*(-b1y + b2y);
		return Point.fromLngLat(x, y);
	}

	public RotationDirection chooseEvasionDirection(Obstacle obstacle, Point origin, double angle) {
		var average = averages.get(obstacle);
		var phi = Math.toDegrees(Math.atan2(average.latitude() - origin.latitude(), average.longitude() - origin.longitude()));
		
		if (phi > angle)
			return RotationDirection.Negative;
		else
			return RotationDirection.Positive;
	}
}
