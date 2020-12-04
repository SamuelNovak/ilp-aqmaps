package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.List;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;


public class ObstacleEvader {

	private ArrayList<ArrayList<Point>> noFlyZones;
	
	// this will hold points close to the points of noFlyZones, but outside the zones
	// indices coupled with noFlyZones: element i for zoneExpansions corresponds to element i of noFlyZones
	private ArrayList<ArrayList<Point>> zoneExpansions;
	
	public ObstacleEvader(FeatureCollection noFlyZones) {		
		var zones = noFlyZones.features();
		
		this.noFlyZones = new ArrayList<ArrayList<Point>>();
		zoneExpansions = new ArrayList<ArrayList<Point>>();
		
		for (int i = 0; i < zones.size(); i++) {
			var polygon = (Polygon) zones.get(i).geometry();
			// get external coordinates
			var points = polygon.coordinates().get(0);
			this.noFlyZones.add((ArrayList<Point>) points);
			
			var offsetPoints = new ArrayList<Point>();
			for (var pt : points) {
				offsetPoints.add(createOffsetPoint(pt, points));
			}
			
			// TODO zoneExpansions simplification - if two points are closer that SENSOR_DISTANCE, contract them
			// alternatively - make sure all points are at least SENSOR_DIST away from the polygon
			zoneExpansions.add(offsetPoints);
		}
		
		
		// TODO debug
		var ftrs = new ArrayList<Feature>();
		for (var points : zoneExpansions) {
			var f = Feature.fromGeometry(Polygon.fromLngLats(List.of(points)));
			ftrs.add(f);
		}
		System.out.println(FeatureCollection.fromFeatures(ftrs).toJson());
	}
	
	public ArrayList<ArrayList<Point>> getNoFlyZones() {
		return noFlyZones;
	}
	
	public ArrayList<ArrayList<Point>> crossedObstacles(Point a1, Point a2) {
		ArrayList<ArrayList<Point>> ret = new ArrayList<ArrayList<Point>>();
		for (var zone : noFlyZones) {			
			if (crossesObstacle(a1, a2, zone))
				ret.add(zone);
		}
		return ret;
	}
	
	private boolean crossesObstacle(Point a1, Point a2, ArrayList<Point> points) {		
		return !obstacleIntersections(a1, a2, points).isEmpty();
	}
	
	/** Find if and where a line intersects an obstacle (no fly zone).
	 * @param a1 Line start point
	 * @param a2 Line end point
	 * @param points Obstacle
	 * @return list of pairs: left: intersection point;
	 * 						  right: corresponding index i, such that intersection occurred with line points[i] -- points[i+1] (mod points.size())
	 */
	private ArrayList<Pair<Point, Integer>> obstacleIntersections(Point a1, Point a2, ArrayList<Point> points) {
		var intersections = new ArrayList<Pair<Point, Integer>>();
		// Iterate until the second-to-last, the last one needs to wrap around (path is cyclic) and so will be handled separately
		for (int i = 0; i < points.size() - 1; i++) {
			var intsection = intersection(a1, a2, points.get(i), points.get((i+1) % points.size()));
			if (intsection != null)
				// Found an intersection with a no-fly zone
				intersections.add(new Pair<Point, Integer>(intsection, i));
		}
		// Here handle the final line segment
		var intsection = intersection(a1, a2, points.get(0), points.get(points.size() - 1));
		if (intsection != null)
			// Found an intersection
			intersections.add(new Pair<Point, Integer>(intsection, points.size() - 1));
		
		return intersections;
	}
	
	/* TODO dead code
	private boolean linesIntersect(Point a1, Point a2, Point b1, Point b2) {
		Line2D line_a = new Line2D.Double(a1.latitude(), a1.longitude(), a2.latitude(), a2.longitude());
		Line2D line_b = new Line2D.Double(b1.latitude(), b1.longitude(), b2.latitude(), b2.longitude());
		return line_a.intersectsLine(line_b);		
	}
	*/
	
	/** Computes the point of intersection of the two lines
	 * @param a1 Start point of first line
	 * @param a2 End point of first line
	 * @param b1 Start point of second line
	 * @param b2 End point of second line
	 * @return
	 */
	// TODO move up
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
	
	public double evasionDistance(Point a1, Point a2) {
		var waypoints = waypointsToAvoidAllObstacles(a1, a2);
		return pathLengthWaypoints(a1, waypoints, a2);
	}

	/** Compute waypoints to get from origin_point to next_point and avoid no fly zones.
	 * @param originPoint
	 * @param nextPoint
	 * @return Sequence of waypoints
	 */
	public ArrayList<Point> waypointsToAvoidAllObstacles(Point originPoint, Point nextPoint) {
		// find first obstacle that stands in the way
		// avoid it
		// call self with this avoided obstacle in clearedObstacles
		// recursive run won't care about the clearedObstacles anymore - speedup
		
		var waypoints = new ArrayList<Point>();
		return waypoints;
		/* TODO disconnected evader
		var lastPoint = originPoint;
		
		var nearestIntersectingObstacle = findNearestIntersectingObstacle(originPoint, nextPoint);
		while (nearestIntersectingObstacle != null) {
			Point nearestIntersection = nearestIntersectingObstacle.left;
			ArrayList<Point> nearestPolygon = nearestIntersectingObstacle.right;
			
			// This assumes that avoiding obstacles separately and putting the waypoints together will not create a longer path
			// as if we avoided all obstacles at once. That might not be the case, but it is a good heuristic.
			waypoints.addAll(waypointsToAvoidSingleObstacle(lastPoint, nextPoint, nearestPolygon));
			
			// TODO debug
			
			var ftrs = new ArrayList<Feature>();
			for (var pt : waypoints) {
				var f = Feature.fromGeometry(pt);
				f.addStringProperty("marker-symbol", "marker");
				ftrs.add(f);
			}
			System.out.println("Waypoints:");
			System.out.println(FeatureCollection.fromFeatures(ftrs).toJson());
			
			// TODO end debug
			
			if (!waypoints.isEmpty())
				lastPoint = waypoints.get(waypoints.size() - 1);
			nearestIntersectingObstacle = findNearestIntersectingObstacle(lastPoint, nextPoint);
		}
		
		return waypoints;
		*/
	}
	
	private double pathLength(ArrayList<Point> path) {
		double length = 0;
		for (int i = 0; i < path.size() - 1; i++)
			length += PathPlanner.distance(path.get(i), path.get(i+1));
		return length;
	}
	
	private double pathLengthWaypoints(Point start, ArrayList<Point> waypoints, Point end) {
		var auxList = new ArrayList<Point>();
		auxList.add(start);
		auxList.addAll(waypoints);
		auxList.add(end);
		return pathLength(auxList);
	}
	
	private double angleBetweenPoints(Point a, Point b) {
		return Math.toDegrees(Math.atan2(b.latitude() - a.latitude(), b.longitude() - a.longitude()));
	}
	
	private Point createPointAtAngleDistanceFromPoint(Point origin, double length, double angle) {
		var longitude = origin.longitude();
		var latitude = origin.latitude();
		
		var angleRad = Math.toRadians(angle);
		System.out.println(angleRad);
		
		var rot_longitude = longitude + length * Math.cos(angleRad);
		var rot_latitude = latitude + length * Math.sin(angleRad);
		return Point.fromLngLat(rot_longitude, rot_latitude);
	}
	
	private Point createOffsetPoint(Point pt, List<Point> points) {
		// TODO this function needs some love, because it's breaking everything else and my heart
		// get the index of this point and find the previous and next point on the polygon
		var pointIndex = points.indexOf(pt);
		var previous = points.get((points.size() + pointIndex - 1) % points.size());
		var next = points.get((pointIndex + 1) % points.size());
		
		var phiPrevious = angleBetweenPoints(pt, previous);
		if (phiPrevious < 0)
			phiPrevious += 360;
		var phiNext = angleBetweenPoints(pt, next);
		if (phiNext < 0)
			phiNext += 360;
		
		var phi = ((phiPrevious + phiNext) / 2);
		
		System.out.println(String.format("phiPrevious = %f; priNext = %f; phi = %f", phiPrevious, phiNext, phi));
		
		return createPointAtAngleDistanceFromPoint(pt, 0.01 * DroneController.SENSOR_READ_MAX_DISTANCE, phi);
	}

}
