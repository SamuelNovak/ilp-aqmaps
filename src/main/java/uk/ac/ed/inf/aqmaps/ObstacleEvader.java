package uk.ac.ed.inf.aqmaps;

import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

public class ObstacleEvader {

	private FeatureCollection noFlyZones;
	
	public ObstacleEvader(FeatureCollection noFlyZones) {
		this.noFlyZones = noFlyZones;
		// TODO preprocess obstacles
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
	
	private boolean linesIntersect(Point a1, Point a2, Point b1, Point b2) {
		// Store values locally, so that they can be used multiple times without overhead,
		// and so that the computation is clear
		var a1x = a1.longitude();
		var a1y = a1.latitude();
		
		var a2x = a2.longitude();
		var a2y = a2.latitude();
		
		var b1x = b1.longitude();
		var b1y = b1.latitude();
		
		var b2x = b2.longitude();
		var b2y = b2.latitude();
		
		// See report for formulation
		var beta = ( (b1y - a1y)*(a2x - a1x) - (b1x - a1x)*(a2y - a1y) )
				 / ( (b2x - b1x)*(a2y - a1y) - (b2y - b1y)*(b2x - b1x) );
		
		if ((beta < 0) || (beta > 1)) // intersection can only occur if beta is within the interval [0,1]
			return false;
		
		var alpha = (b1x - a1x + beta * (b2x - b1x))
				  / (a2x - a1x);
		
		if ((alpha < 0) || (alpha > 1)) // alpha must also be in the interval [0,1] 
			return false;
		
		// otherwise (so both alpha and beta are in [0,1]), there is an intersection of these lines
		return true;
	} 

}
