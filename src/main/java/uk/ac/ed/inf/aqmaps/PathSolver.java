package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;

import com.mapbox.geojson.FeatureCollection;

public class PathSolver {

	private ArrayList<SensorReading> map;
	private FeatureCollection noFlyZones;
	private int rand_seed;
	
	private double[][] distances; // matrix for distances between nodes (weighted graph) TODO explain

	public PathSolver(ArrayList<SensorReading> map, FeatureCollection noFlyZones, int rand_seed) {
		this.map = map;
		this.noFlyZones = noFlyZones;
		this.rand_seed = rand_seed;
		
		
		// Compute the distance matrix
		distances = new double[33][33];
		for (int i = 0; i < 33; i++)
			for (int j = 0; j <= i; j++) {
				if (i == j) distances[i][j] = 0;
				else {
					distances[i][j] = distance(map.get(i), map.get(j));
					distances[j][i] = distances[i][j]; // TODO toto sa da urobit aj jednostranne, ale neviem ci treba
				}
			}
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
