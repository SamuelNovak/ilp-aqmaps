package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;

public class PathSolver {

	private ArrayList<SensorReading> map;
	private FeatureCollection noFlyZones;
	private int rand_seed;
	
	private double[][] distances; // matrix for distances between nodes (weighted graph) TODO explain
	private ObstacleEvader evader;

	public PathSolver(ArrayList<SensorReading> map, FeatureCollection noFlyZones, int rand_seed) {
		this.map = map;
		this.noFlyZones = noFlyZones;
		this.rand_seed = rand_seed;
		
		evader = new ObstacleEvader(noFlyZones);
		
		// for debug:
		
		var lines = new ArrayList<Feature>();
		
		// end debug
		
		// Compute the distance matrix
		distances = new double[33][33];
		for (int i = 0; i < 33; i++)
			for (int j = 0; j <= i; j++) {
				if (i == j) distances[i][j] = 0;
				else {
					distances[i][j] = distance(map.get(i), map.get(j));
					distances[j][i] = distances[i][j]; // TODO toto sa da urobit aj jednostranne, ale neviem ci treba
				}
				
				// DEBUG
				
				var pts = new ArrayList<Point>();
				pts.add(Point.fromLngLat(map.get(i).lng, map.get(i).lat));
				pts.add(Point.fromLngLat(map.get(j).lng, map.get(j).lat));
				var ftr = Feature.fromGeometry((Geometry) LineString.fromLngLats(pts));
				ftr.addStringProperty("stroke", evader.crossesObstacle(pts.get(0), pts.get(1)) ? "#ff00c0" : "#00cc00");
				lines.add(ftr);
				
				// END DEBUG
			}
		
		// DEBUG
		
		lines.addAll(noFlyZones.features());
		FeatureCollection col = FeatureCollection.fromFeatures(lines);
		System.out.println(col.toJson());
		
		// END DEBUG
	}
	
	public static double distance(SensorReading x, SensorReading y) {
		return Math.hypot(x.lat - y.lat, x.lng - y.lng);
	}

	// Path will be a sequence of directions the drone is to take
	public ArrayList<Move> findPath(double start_lat, double start_lon) {
		// TODO: first ordering of nodes
		// TODO: then optimize for no-flight zones
		// TODO: use a visibility matrix?
		// TODO: perturbation?
		return null;
	}

}
