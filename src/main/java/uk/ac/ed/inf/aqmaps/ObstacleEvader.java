package uk.ac.ed.inf.aqmaps;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;


public class ObstacleEvader {

	private FeatureCollection noFlyZones;
	
	// this will hold points close to the points of noFlyZones, but outside the zones
	private ArrayList<ArrayList<Point>> zoneExpansions;
	
	public ObstacleEvader(FeatureCollection noFlyZones) {
		this.noFlyZones = noFlyZones;
		
		var zones = noFlyZones.features();
		zoneExpansions = new ArrayList<ArrayList<Point>>();
		for (int i = 0; i < zones.size(); i++) {
			var polygon = (Polygon) zones.get(i).geometry();
			// get external coordinates
			var points = polygon.coordinates().get(0);
			
			var offsetPoints = new ArrayList<Point>();
			for (var pt : points) {
				offsetPoints.add(createOffsetPoint(pt, points));
			}
			
			zoneExpansions.add(offsetPoints);
		}
	}
	
	public ArrayList<Polygon> crossedObstacles(Point a1, Point a2) {
		var ret = new ArrayList<Polygon>();
		for (var ftr : noFlyZones.features()) {
			assert(ftr.geometry() instanceof Polygon); // TODO nejak lepsie toto urobit
			var polygon = (Polygon) ftr.geometry();
			
			if (crossesObstacle(a1, a2, polygon))
				ret.add(polygon);
		}
		return ret;
	}
	
	private boolean crossesObstacle(Point a1, Point a2, Polygon obstacle) {
		// Getting the 0th element = the external outline (don't care if they have interior holes)
		var points = obstacle.coordinates().get(0);
		
		// Iterate until the second-to-last, the last one needs to wrap around (path is cyclic) and so will be handled separately
		for (int i = 0; i < points.size() - 1; i++) {
			if (linesIntersect(a1, a2, points.get(i), points.get(0)))
				// Found an intersection with a no-fly zone
				return true;
		}
		// Here handle the final line segment
		if (linesIntersect(a1, a2, points.get(0), points.get(points.size() -1)))
			// Found an intersection
			return true;
		
		return false;
	}
	
	private boolean linesIntersect(Point a1, Point a2, Point b1, Point b2) {
		Line2D line_a = new Line2D.Double(a1.latitude(), a1.longitude(), a2.latitude(), a2.longitude());
		Line2D line_b = new Line2D.Double(b1.latitude(), b1.longitude(), b2.latitude(), b2.longitude());
		return line_a.intersectsLine(line_b);		
	}
	
	/** Computes the point of intersection of the two lines
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
	
	public double evasionDistance(Point a1, Point a2) {
		var waypoints = waypointsToAvoidAllObstacles(a1, a2);
		return pathLengthWaypoints(a1, waypoints, a2);
	}

	/** Compute waypoints to get from origin_point to next_point and avoid no fly zones.
	 * @param origin_point
	 * @param next_point
	 * @return Sequence of waypoints
	 */
	public ArrayList<Point> waypointsToAvoidAllObstacles(Point origin_point, Point next_point) {
		// find first obstacle that stands in the way
		// avoid it
		// call self with this avoided obstacle in clearedObstacles
		// recursive run won't care about the clearedObstacles anymore - speedup
		
		var waypoints = new ArrayList<Point>();
		var lastPoint = origin_point;
		
		var nearestIntersectingObstacle = findNearestIntersectingObstacle(origin_point, next_point);
		while (nearestIntersectingObstacle != null) {
			Point nearestIntersection = nearestIntersectingObstacle.left;
			Polygon nearestPolygon = nearestIntersectingObstacle.right;
			
			// This assumes that avoiding obstacles separately and putting the waypoints together will not create a longer path
			// as if we avoided all obstacles at once. That might not be the case, but it is a good heuristic.
			waypoints.addAll(waypointsToAvoidSingleObstacle(lastPoint, next_point, nearestPolygon, RotationDirection.Both));
			
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
			nearestIntersectingObstacle = findNearestIntersectingObstacle(lastPoint, next_point);
		}
		
		return waypoints;
	}
	
	/** Calculate waypoints around a particular obstacle, with possibility of specifying the preferred direction
	 * @param origin_point
	 * @param next_point
	 * @param obstacle
	 * @param preferredDirection
	 * 		  0 go both ways, +1 positive (counterclockwise), -1 negative (clockwise)
	 * @return List of waypoints, or null if unreachable using these specific arguments.
	 */
	private ArrayList<Point> waypointsToAvoidSingleObstacle(Point origin_point, Point next_point, Polygon obstacle, RotationDirection direction) {
		// base case - no more waypoints needed
		if (!crossesObstacle(origin_point, next_point, obstacle))
			return new ArrayList<Point>();
		
		// get points of the obstacle polygon (index 0, want exterior)
		var points = obstacle.coordinates().get(0);
		
		// some initial point (which has to exist) so we can initialize the angles
		var pt0 = points.get(0);
		
		// angles from origin_point
		var theta_min = angleBetweenPoints(origin_point, pt0);
		var theta_min_pt = pt0;
		
		var theta_max = theta_min;
		var theta_max_pt = pt0;
		
		// find the actual min and max theta (assuming here it's a valid polygon - has at least 3 points)
		for (int i = 1; i < points.size(); i++) {
			var pt = points.get(i);
			var theta = angleBetweenPoints(origin_point, pt);
			
			if (theta > theta_max) {
				theta_max = theta;
				theta_max_pt = pt;
			}
			if (theta < theta_min) { // TODO together? they should not coincide in a valid polygon
				theta_min = theta;
				theta_min_pt = pt;
			}
		}
		
		// round up or down to allowed angles to get better avoidance
		theta_min = 10 * Math.floor(theta_min / 10);
		theta_max = 10 * Math.ceil(theta_max / 10);
		
		// now generate waypoints in both ways around the polygon
		// first waypoints are the points already found, just need to offset them so they no longer lie on the polygon
		Point waypointThetaMin = createOffsetPoint(theta_min_pt, (ArrayList<Point>) points);
		Point waypointThetaMax = createOffsetPoint(theta_max_pt, (ArrayList<Point>) points);
		

		ArrayList<Point> waypointsNegative = null;
		ArrayList<Point> waypointsPositive = null;
		
		if (!pointOutOfBounds(waypointThetaMin) && direction.allowsPositive()) {
			waypointsNegative = new ArrayList<Point>();
			waypointsNegative.add(waypointThetaMin);
			
			// TODO decide based on position w.r.t. obstacle - pos/neg will depend on this
			
			var followingWaypoints = waypointsToAvoidSingleObstacle(waypointThetaMin, next_point, obstacle, RotationDirection.Positive);
			if (followingWaypoints == null) {
				// impossible to go this way at some point
				waypointsNegative = null;
			} else {
				waypointsNegative.addAll(followingWaypoints);
			}
		}
		
		if (!pointOutOfBounds(waypointThetaMax) && direction.allowsNegative()) {
			waypointsPositive = new ArrayList<Point>();
			waypointsPositive.add(waypointThetaMax);
			
			var followingWaypoints = waypointsToAvoidSingleObstacle(waypointThetaMax, next_point, obstacle, RotationDirection.Negative);
			if (followingWaypoints == null) {
				// impossible to go this way at some point
				waypointsPositive = null;
			} else {
				waypointsPositive.addAll(followingWaypoints);
			}
		}
		
		if (waypointsNegative == null)
			return waypointsPositive;
		else if (waypointsPositive == null)
			return waypointsNegative;
		else {
			// pick the shorter one
			var lengthNegative = pathLengthWaypoints(origin_point, waypointsNegative, next_point);
			var lengthPositive = pathLengthWaypoints(origin_point, waypointsPositive, next_point);
			if (lengthNegative < lengthPositive)
				return waypointsNegative;
			else
				return waypointsPositive;
		}
	}
	
	/** Find the nearest intersection and the obstacle polygon it belongs to.
	 * @param origin_point
	 * @param next_point
	 * @return Pair of intersection Point and its Polygon
	 */
	private Pair<Point, Polygon> findNearestIntersectingObstacle(Point origin_point, Point next_point) {
		Point nearestIntersection = null;
		Polygon nearestPolygon = null;
		
		// -1 used because no distance will ever be < 0
		double min_distance = -1;
		
		int intersections = 0;
		
		for (var obstacleFeature : noFlyZones.features()) {
			// guaranteed that no fly zones will be polygons
			var obstaclePolygon = (Polygon) obstacleFeature.geometry();
			
			// get (outer - hence the index 0) points of the polygon
			var points = obstaclePolygon.coordinates().get(0);
			final var numberOfPoints = points.size();
			for (var i = 0; i < numberOfPoints; i++) {
				var p1 = points.get(i);
				var p2 = points.get((i+1) % numberOfPoints);
				
				var inter = intersection(origin_point, next_point, p1, p2);
				
				if (inter != null) {
					// intersection was found
					var dist = PathPlanner.distance(origin_point, inter);
					intersections++;
					if (dist < min_distance || min_distance == -1) {
						// this is the new minimum distance
						nearestIntersection = inter;
						nearestPolygon = obstaclePolygon;
						min_distance = dist;
					}
				}
			}
		}
		
		System.out.println(String.format("Intersections: %d", intersections));
		
		if (min_distance > -1)
			return new Pair<Point, Polygon>(nearestIntersection, nearestPolygon);
		else
			// no intersecting obstacle found
			return null;
	}
	
	private Point createPointAtAngleDistanceFromPoint(Point origin, double length, double angle) {
		var longitude = origin.longitude();
		var latitude = origin.latitude();
		
		var rot_longitude = longitude + length * Math.cos(Math.toRadians(angle));
		var rot_latitude = latitude + length * Math.sin(Math.toRadians(angle));
		return Point.fromLngLat(rot_longitude, rot_latitude);
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
	
	private boolean pointOutOfBounds(Point point) {
		var longitude = point.longitude();
		var latitude = point.latitude();
		
		return ((longitude >= DroneController.LON_MAX) || (longitude <= DroneController.LON_MIN)
				|| (latitude >= DroneController.LAT_MAX) || (latitude <= DroneController.LAT_MIN));
	}
	
	private Point createOffsetPoint(Point pt, List<Point> points) {
		// for thetaMin
		// get the index of this point and find the previous and next point on the polygon
		var pointIndex = points.indexOf(pt);
		var previous = points.get((points.size() + pointIndex - 1) % points.size());
		var next = points.get((pointIndex + 1) % points.size());
		
		var phiPrevious = angleBetweenPoints(pt, previous);
		var phiNext = angleBetweenPoints(pt, next);
		
		var phi = ((phiPrevious + phiNext) / 2 + 180) % 360;
		
		return createPointAtAngleDistanceFromPoint(pt, 0.99 * DroneController.SENSOR_READ_MAX_DISTANCE, phi);
	}

}
