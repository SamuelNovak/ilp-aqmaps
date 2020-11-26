package uk.ac.ed.inf.aqmaps;

import java.awt.geom.Line2D;

import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

public class ObstacleEvader {

	private FeatureCollection noFlyZones;
	
	public ObstacleEvader(FeatureCollection noFlyZones) {
		this.noFlyZones = noFlyZones;
		// TODO preprocess obstacles - mozno mi treba tazisko?
	}
	
	public boolean crossesObstacle(Point a1, Point a2) {
		for (var ftr : noFlyZones.features()) {
			assert(ftr.geometry() instanceof Polygon); // TODO nejak lepsie toto urobit
			var polygon = (Polygon) ftr.geometry();
			
			var points = polygon.coordinates().get(0); // Getting the 0th element = the external outline (don't care if they have interior holes)
			
			for (int i = 0; i < points.size() - 1; i++) { // interate until the second-to-last, than one needs to wrap around and so will be handled separately
				if (linesIntersect(a1, a2, points.get(i), points.get(0)))
					return true; // Found an intersection with a no-fly zone
			}
			// Here handle the final line segment
			if (linesIntersect(a1, a2, points.get(0), points.get(points.size() -1)))
				return true; // Found an intersection
		}
		return false;
	}
	
	private static boolean linesIntersect(Point a1, Point a2, Point b1, Point b2) { // TODO java.awt.geom.Line2D?
		Line2D line_a = new Line2D.Double(a1.latitude(), a1.longitude(), a2.latitude(), a2.longitude());
		Line2D line_b = new Line2D.Double(b1.latitude(), b1.longitude(), b2.latitude(), b2.longitude());
		return line_a.intersectsLine(line_b);		
	}
	
	public double evasionDistance(Point a1, Point a2) {
		
		return 0;
	}

}
