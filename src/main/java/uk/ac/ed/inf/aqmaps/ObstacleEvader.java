package uk.ac.ed.inf.aqmaps;

import java.awt.geom.Line2D;
import java.util.ArrayList;

import javax.print.attribute.standard.JobOriginatingUserName;

import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;


public class ObstacleEvader {

	private FeatureCollection noFlyZones;
	
	public ObstacleEvader(FeatureCollection noFlyZones) {
		this.noFlyZones = noFlyZones;
	}
	
	// TODO this can be done all in one go
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
	// TODO maybe make this more efficient?
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
	
	// TODO why: want to store intersection along with what it intersects in the polygon
	// TODO might be unnecessary in the future
	private class IntersectionTriPoint { // I really miss being able to create arbitrary tuples on the run :(
		Point b1, b2, intersection;
		public IntersectionTriPoint(Point b1, Point b2, Point intersection) {
			this.b1 = b1;
			this.b2 = b2;
			this.intersection = intersection;
		}
	}
	
	public double evasionDistance(Point a1, Point a2, Polygon obstacle) {
		var intersections = new ArrayList<IntersectionTriPoint>();
		
		// Getting the 0th element = the external outline (don't care if they have interior holes)
		var points = obstacle.coordinates().get(0);
		
		// interate until the second-to-last, the last one needs to wrap around (path is cyclic) and so will be handled separately
		for (int i = 0; i < points.size() - 1; i++) {
			if (linesIntersect(a1, a2, points.get(i), points.get(0))) {
				intersections.add(new IntersectionTriPoint(points.get(i), points.get(0), intersection(a1, a2, points.get(i), points.get(0))));
			}
		}
		
		// Here handle the final line segment
		if (linesIntersect(a1, a2, points.get(0), points.get(points.size() -1)))
			intersections.add(new IntersectionTriPoint(points.get(0), points.get(points.size() -1), intersection(a1, a2, points.get(0), points.get(points.size() -1))));
		
		if (intersections.isEmpty())
			return 0;
		
		// find which intersection is the first one encountered (closest to a1) - this is where the evasion starts
		var first_intersection_tp = intersections.get(0);
		var min = Math.hypot(a1.longitude() - first_intersection_tp.intersection.longitude(), a1.latitude() - first_intersection_tp.intersection.latitude());
		for (var i : intersections) {
			var dst =Math.hypot(a1.longitude() - i.intersection.longitude(), a1.latitude() - i.intersection.latitude());
			if (dst < min) {
				min = dst;
				first_intersection_tp = i;
			}
		}
		
		// now go around the obstacle in either direction until point a2 is visible via a straight line that doesn't cross obstacles
		// Note: this ignores any other obstacles that may be in the way, but this is a sufficient heuristic
		
		// distance when going in positive and negative steps when iterating over polygon points
		double ev_dist_pos = 0;
		double ev_dist_neg = 0;
		
		// decide which of the side points in the IntersectionTriPoint is the positive and which negative
		int index_positive = 0;
		int index_negative = 0;
		
		// find b1 and then depending on the direction where b2 is, decide which point is start for positive and negative iteration
		for (int i = 0; i < points.size(); i++) {
			if (points.get(i) == first_intersection_tp.b1) { // comparison by reference
				if (points.get((i + 1) % points.size()) == first_intersection_tp.b2) {
					// the line i -- i+1 is the one that has the intersection => need to go away from here
					index_positive = (i + 1) % points.size(); // first_intersection_tp.b2
					index_negative = i; // first_intersection_tp.b1
				} else {
					index_positive = i; // first_intersection_tp.b1
					index_negative = (i - 1) % points.size(); // first_intersection_tp.b2
					// TODO rename in comments if renamed
				}
				break;
			}
		}
		
		
		while (true) {
			var here = points.get(index_positive);
			
			// get a point with a small offset in the direction of a2 (the endpoint of the original line),
			// so that we do not check the point here for intersection - this would always be true, because
			// the point belongs to the polygon
			var aux_lon = here.longitude() + 0.0001 * (a2.longitude() - here.longitude());
			var aux_lat = here.latitude() + 0.0001 * (a2.latitude() - here.latitude());
			var aux = Point.fromLngLat(aux_lon, aux_lat);
			if (!crossesObstacle(aux, a2, obstacle))
				break;
			
			
			var next_index = (index_positive + 1) % points.size();
			var next = points.get(next_index);
			
			ev_dist_pos += Math.hypot(here.longitude() - next.longitude(), here.latitude() - next.latitude());
			index_positive = next_index;
		}
		
		while (true) {
			var here = points.get(index_negative);
			
			// get a point with a small offset in the direction of a2 (the endpoint of the original line),
			// so that we do not check the point here for intersection - this would always be true, because
			// the point belongs to the polygon
			var aux_lon = here.longitude() + 0.0001 * (a2.longitude() - here.longitude());
			var aux_lat = here.latitude() + 0.0001 * (a2.latitude() - here.latitude());
			var aux = Point.fromLngLat(aux_lon, aux_lat);
			if (!crossesObstacle(aux, a2, obstacle))
				break;
			
			
			var next_index = (index_negative - 1) % points.size();
			var next = points.get(next_index);
			
			ev_dist_neg += Math.hypot(here.longitude() - next.longitude(), here.latitude() - next.latitude());
			index_negative = next_index;
		}
		
		// return the smaller evasion distance
		if (ev_dist_pos < ev_dist_neg)
			return ev_dist_pos;
		else
			return ev_dist_neg;
	}

	/** Compute waypoints to get from origin_point to next_point and avoid no fly zones.
	 * @param origin_point
	 * @param next_point
	 * @return Sequence of waypoints
	 */
	public ArrayList<Point> waypointsToAvoidObstacles(Point origin_point, Point next_point) {
		return waypointsToAvoidRemainingObstacles(origin_point, next_point, new ArrayList<Polygon>());
	}
	
	private ArrayList<Point> waypointsToAvoidRemainingObstacles(Point origin_point, Point next_point, ArrayList<Polygon> clearedObstacles) {
		// find first obstacle that stands in the way
		// avoid it
		// call self with this avoided obstacle in clearedObstacles
		// recursive run won't care about the clearedObstacles anymore - speedup
		
		Point nearestIntersection = null;
		Polygon nearestPolygon = null;
		// -1 used because no distance will ever be < 0
		double min_distance = -1;
		
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
					
					if (dist < min_distance || min_distance == -1) {
						// this is the new minimum distance
						nearestIntersection = inter;
						nearestPolygon = obstaclePolygon;
						min_distance = dist;
					}
				}
			}
		}
		
		// now get a waypoint to avoid this newfound nearest obstacle
		// TODO comments (but will be explained in report)
		
		// get points of the nearest obstacle polygon (index 0, want exterior)
		var points = nearestPolygon.coordinates().get(0);
		
		// just so we can initialize the angles
		var pt0 = points.get(0);
		
		// angles from origin_point
		// TODO function for angles
		var theta_min = Math.toDegrees(Math.atan2(pt0.latitude() - origin_point.latitude(), pt0.longitude() - origin_point.longitude()));
		var theta_max = theta_min;
		
		// angles from the target point
		var phi_min = Math.toDegrees(Math.atan2(pt0.latitude() - next_point.latitude(), pt0.longitude() - next_point.longitude()));
		var phi_max = phi_min;
		
		// assuming it's a valid polygon - has at least 3 points
		for (int i = 1; i < points.size(); i++) {
			var pt = points.get(i);
			var theta = Math.toDegrees(Math.atan2(pt.latitude() - origin_point.latitude(), pt.longitude() - origin_point.longitude()));
			var phi = Math.toDegrees(Math.atan2(pt.latitude() - next_point.latitude(), pt.longitude() - next_point.longitude()));
			
			if (theta > theta_max)
				theta_max = theta;
			if (theta < theta_min) // TODO together?
				theta_min = theta;
			
			if (phi > phi_max)
				phi_max = phi;
			if (phi < phi_min)
				phi_min = phi;
		}
		
		// round up or down to allowed angles to get better avoidance
		theta_min = 10 * Math.floor(theta_min / 10);
		theta_max = 10 * Math.ceil(theta_max / 10);
		phi_min = 10 * Math.floor(phi_min / 10);
		phi_max = 10 * Math.ceil(phi_max / 10);
		
		// find the possible waypoints - intersections of lines: 
		// - from origin_point at theta_max and from next_point at phi_min
		// - from origin_point at theta_min and from next_point at phi_max
		// use the 2 * original distance between origin and next as length of each line
		
		// TODO this whole thing has to change, bc potential waypoints can be out of bounds 
		
		var dist = 2 * PathPlanner.distance(origin_point, next_point);

		var origin_to_theta_max = abcdefgh(origin_point, dist, theta_max);
		var origin_to_theta_min = abcdefgh(origin_point, dist, theta_min);
		
		var next_to_phi_max = abcdefgh(next_point, dist, phi_max);
		var next_to_phi_min = abcdefgh(next_point, dist, phi_min);
		
		var waypoint_left = intersection(origin_point, origin_to_theta_max, next_point, next_to_phi_min);
		var waypoint_right = intersection(origin_point, origin_to_theta_min, next_point, next_to_phi_max);
		
		if (waypoint_left == null) {
			System.out.println("waypoint_left null!!!"); // TODO should NOT happen!
			if (waypoint_right == null) {
				System.out.println("PANIC! Both waypoints null!!!");
			}
		}
		if (waypoint_right == null) {
			System.out.println("waypoint_right null!!!");
		}
		
		return new ArrayList<Point>();
	}
	
	
	private Point abcdefgh(Point origin, double length, double angle) { // TODO name srsly
		var longitude = origin.longitude();
		var latitude = origin.latitude();
		
		var rot_longitude = longitude + length * Math.cos(Math.toRadians(angle));
		var rot_latitude = latitude + length * Math.sin(Math.toRadians(angle));
		return Point.fromLngLat(rot_longitude, rot_latitude);
	}

}
