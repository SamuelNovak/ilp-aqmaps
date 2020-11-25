package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;

public class PathSolver {

	// Flight boundaries
	public static final double LAT_MAX = 55.946233; // latitude
	public static final double LAT_MIN = 55.942617;
	public static final double LON_MAX = -3.184319; // longitude
	public static final double LON_MIN = -3.192473;
	public static final double MOVE_LENGTH = 0.0003; // length of EVERY drone step in degrees
	
	
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
		
		// DEBUG
		
		var lines = new ArrayList<Feature>();
		
		// END DEBUG
		
		// Compute the distance matrix
		distances = new double[33 + 1][33 + 1]; // 33 sensors + 1 starting location
		for (int i = 0; i < 33; i++)
			for (int j = 0; j <= i; j++) {
				if (i == j) distances[i][j] = 0;
				else {
					distances[i][j] = distance(map.get(i), map.get(j));
					distances[j][i] = distances[i][j]; // TODO toto sa da urobit aj jednostranne, ale neviem ci treba
				}
				
				// DEBUG
				
				var pts = new ArrayList<Point>();
				pts.add(Point.fromLngLat(map.get(i).lon, map.get(i).lat));
				pts.add(Point.fromLngLat(map.get(j).lon, map.get(j).lat));
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
		return Math.hypot(x.lat - y.lat, x.lon - y.lon);
	}

	// Path will be a sequence of directions the drone is to take
	public ArrayList<Move> findPath(double start_lat, double start_lon) {
		// TODO: first ordering of nodes
		// TODO: then optimize for no-flight zones
		// TODO: use a visibility matrix?
		// TODO: perturbation?
		
		for (int i = 0; i < 33; i++) { // calculate the distances to the starting location
			distances[33][i] = Math.hypot(start_lat - map.get(i).lat, start_lon - map.get(i).lon);
			distances[i][33] = distances[33][i];
		}
		
		var TSP_path = solveTSP(start_lat, start_lon);
		
		// TODO DEBUG
		var pts = new ArrayList<Point>();
		for (var i : TSP_path) {
			if (i == 33)
				pts.add(Point.fromLngLat(start_lon, start_lat));
			else
				pts.add(Point.fromLngLat(map.get(i).lon, map.get(i).lat));
		}
		{
			var i = TSP_path.get(0);
			if (i == 33)
				pts.add(Point.fromLngLat(start_lon, start_lat));
			else
				pts.add(Point.fromLngLat(map.get(i).lon, map.get(i).lat));
		}
		var ftr = Feature.fromGeometry((Geometry) LineString.fromLngLats(pts));
		
		var ftrs = new ArrayList<Feature>();
		ftrs.addAll(noFlyZones.features());
		ftrs.add(ftr);
		FeatureCollection col = FeatureCollection.fromFeatures(ftrs);
		System.out.println(col.toJson());
		// END DEBUG
		
		return null;
	}
	
	private ArrayList<Integer> solveTSP(double start_lat, double start_lon) { // TODO: name
		var sequence = new ArrayList<Integer>(); // sequence of sensors to visit, identified by their index in this.map
		
		// ALGORITHM goes here
		// trying: Nearest Insert (O(n^2)), because I am already familiar, and then optimize by 2-opt (O(n^2))
		
		var unused = new ArrayList<Integer>(); // so far unused sensors
		
		{ // isolating this block so it's clear where the local vars belong
			var min = distances[0][1]; // TODO possibly put this into a different method?
			var na = 0; // nearest sensors
			var nb = 1;
			for (int i = 1; i < 34; i++) { // finding the nearest pair of sensors
				for (int j = i + 1; j < 34; j++) {
					if (distances[i][j] < min) {
						min = distances[i][j];
						na = i;
						nb = j;
					}
				}
			}
			
			sequence.add(na); // start the sequence with the minimum that was found
			sequence.add(nb);
			
			for (int i = 0; i < 34; i++) // initialize the set of unused sensors - this will be needed for the nearest insert algorithm
				if (i != na && i != nb)
					unused.add(i);
		}
		
		// NEAREST INSERT
		
		while (!unused.isEmpty()) {
			var min = distances[sequence.get(0)][unused.get(0)];
			var minu = unused.get(0); // this will hold the unused node that is to be inserted - is nearest to sequence
			var mini = 0; // this will hold the node in sequence where the nearest insertion was found
			// TODO comments: replace sensor with the more generic "node" or "vertex"
			// find an unused sensor that is nearest to some other sensor which is already in the sequence 
			for (int i = 0; i < sequence.size(); i++) { // i corresponds to index of a sensor already used in the sequence
				for (var u : unused) { // u is an unused sensor
					if (distances[sequence.get(i)][u] < min) {
						min = distances[sequence.get(i)][u];
						mini = i;
						minu = u;
					}
				}
			}
			// now need to decide on which side of mini to insert minu - before or after
			var dist_before = distances[(sequence.size() + mini - 1) % sequence.size()][minu];
			var dist_after  = distances[(mini + 1) % sequence.size()][minu];
			if (dist_before < dist_after) {
				// insert to sequence[mini]
				sequence.add(mini, minu);
			} else {
				// insert to sequence[mini + 1 mod size]
				sequence.add((mini + 1) % sequence.size(), minu);
			}
			// remove minu from unused // using cast to object so it actually removes the element, not index
			unused.remove((Object) minu);
		}
		
		// TWO-OPT
		for (int i = 0; i < 34; i++)
			for (int j = 0; j < 34; j++) {
				if (i == j)
					continue;
				
				var vertex_i  = sequence.get(i);
				var vertex_ii = sequence.get((i + 1) % 34);
				var vertex_j  = sequence.get(j);
				var vertex_jj = sequence.get((j + 1) % 34);
				
				// check if swapping them produces a shorter path
				if (distances[vertex_i][vertex_jj] + distances[vertex_j][vertex_ii] <
					distances[vertex_i][vertex_ii] + distances[vertex_j][vertex_jj])
				{
					// System.out.println(String.format("Swapping edges %d, %d", i, j));
					sequence.set((i + 1) % 34, vertex_jj);
					sequence.set((j + 1) % 34, vertex_ii);
				}
			}
		
		
		// END ALGORITHM
		
		System.out.println(sequence);
		
		return sequence;
	}

}
