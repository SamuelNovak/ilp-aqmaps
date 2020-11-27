package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;

public class PathPlanner {

	// Flight boundaries
	// TODO keywords?
	public static final double LAT_MAX = 55.946233; // latitude
	public static final double LAT_MIN = 55.942617;
	public static final double LON_MAX = -3.184319; // longitude
	public static final double LON_MIN = -3.192473;
	public static final double MOVE_LENGTH = 0.0003; // length of EVERY drone step in degrees
	public static final double SENSOR_READ_MAX_DISTANCE = 0.0002; // maximum distance from a sensor to receive data - the drone has to be STRICTLY closer than this
	
	
	private ArrayList<SensorReading> map;
	private FeatureCollection noFlyZones;
	
	private double[][] distances; // matrix for distances between nodes (weighted graph) TODO explain
	private ObstacleEvader evader;

	public PathPlanner(ArrayList<SensorReading> map, FeatureCollection noFlyZones) {
		this.map = map;
		this.noFlyZones = noFlyZones;
		
		evader = new ObstacleEvader(noFlyZones);
		
		// Compute the distance matrix
		distances = new double[33 + 1][33 + 1]; // 33 sensors + 1 starting location
		for (int i = 0; i < 33; i++)
			for (int j = 0; j <= i; j++) {
				if (i == j) distances[i][j] = 0;
				else {
					var point_i = map.get(i).toPoint();
					var point_j = map.get(j).toPoint();
					
					distances[i][j] = distance(point_i, point_j);
					
					// include evasion of no fly zones in the distance matrix
					var crossedObstacles = evader.crossedObstacles(point_i, point_j);
					for (var obs : crossedObstacles) {
						distances[i][j] += evader.evasionDistance(point_i, point_j, obs);
					}
					
					// distance matrix is symmetric
					distances[j][i] = distances[i][j];
				}
			}
	}
	
	public static double distance(Point x, Point y) {
		return Math.hypot(x.latitude() - y.latitude(), x.longitude() - y.longitude());
	}

	// Path plan will be a sequence of waypoints for the drone
	// TODO explanation: so in the future the drone can correct for weather etc., now it only knows the general path (but this will include waypoints to help avoid the no fly zones)
	public ArrayList<Point> findPath(double start_lat, double start_lon) {
		
		for (int i = 0; i < 33; i++) { // calculate the distances to the starting location
			distances[33][i] = Math.hypot(start_lat - map.get(i).lat, start_lon - map.get(i).lon);
			distances[i][33] = distances[33][i];
		}
		
		var TSP_path = solveTSP();
		
		// TODO this is DEBUG
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
	
	private ArrayList<Integer> solveTSP() { // TODO: name
		var sequence = new ArrayList<Integer>(); // sequence of sensors to visit, identified by their index in this.map
		
		// ALGORITHM goes here
		// trying: Nearest Insert (O(n^2)), because I am already familiar, and then optimize by 2-opt, swap (O(n^2))
		
		var unused = new ArrayList<Integer>(); // so far unused sensors
		
		{ // isolating this block so it's clear where the local vars belong
			var min = distances[0][1]; // TODO possibly put this into a different method?
			// nearest vertices
			var nearest_a = 0;
			var nearest_b = 1;
			for (int i = 1; i < 34; i++) { // finding the nearest pair of sensors
				for (int j = i + 1; j < 34; j++) {
					if (distances[i][j] < min) {
						min = distances[i][j];
						nearest_a = i;
						nearest_b = j;
					}
				}
			}
			
			// start the sequence with the minimum that was found
			sequence.add(nearest_a);
			sequence.add(nearest_b);
			
			// initialize the set of unused sensors - this will be needed for the nearest insert algorithm
			for (int i = 0; i < 34; i++)
				if (i != nearest_a && i != nearest_b)
					unused.add(i);
		}
		
		// NEAREST INSERT
		
		while (!unused.isEmpty()) {
			var min = distances[sequence.get(0)][unused.get(0)];
			
			// this will hold the unused node that is to be inserted - is nearest to sequence
			var minu = unused.get(0);
			
			// this will hold the node in sequence where the nearest insertion was found
			var mini = 0;
			
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
			
			// remove minu from unused - using cast to object so it actually removes the element minu itself, not an element at index minu
			unused.remove((Object) minu);
		}
		
		// TWO-OPT & SWAP
		for (int i = 0; i < 34; i++)
			for (int j = 0; j < 34; j++) {
				if (i == j)
					continue;
				
				// Prepare vertices sequence[i-1] --- sequence[i] --- sequence[i+1] (mod 34)
				// same for sequence[j-1] --- sequence[j] --- sequence[j+1] (mod 34)
				var vertices_around_i = new int[4];
				var vertices_around_j = new int[4];
				
				for (int k = -1; k < 3; k++) {
					vertices_around_i[k+1] = sequence.get((34 + i + k) % 34);
					vertices_around_j[k+1] = sequence.get((34 + j + k) % 34);
				}
				
				// check if swapping vertices on the same line makes it better
				// this will compare path [i-1] --- [i] --- [i+1] --- [i+2] vs
				// the path               [i-1] --- [i+1] --- [i] --- [i+2]
				// where [a] means sequence[a]
				// this is an extremely localized optimization
				{
					double original_length = 0;
					var swapped_length = distances[vertices_around_i[2]][vertices_around_i[1]]; 
					for (int k = 0; k < vertices_around_i.length - 1; k++) {
						original_length += distances[vertices_around_i[k]][vertices_around_i[k+1]];
						if (k < 2)
							swapped_length += distances[vertices_around_i[k]][vertices_around_i[k+2]];
					}
					
					if (swapped_length < original_length) {
						sequence.set(i, vertices_around_i[2]);
						sequence.set((i+1) % 34, vertices_around_i[1]);
					}
				}
				
				// check if swapping them produces a shorter path
				{						
					// use global path length (straight line distance)
					double original_length = 0;
					for (int k = 0; k < 34; k++)
						original_length += distances[sequence.get(k)][sequence.get((k+1) % 34)];
					
					// create a trial sequence by swapping the endpoints of two edges and compute its global path
					var trial_seq = new ArrayList<Integer>();
					for (int k = 0; k < 34; k++)
						trial_seq.add((int) sequence.get(k));
					trial_seq.set((i + 1) % 34, vertices_around_j[2]);
					trial_seq.set((j + 1) % 34, vertices_around_i[2]);
					
					double trial_length = 0;
					for (int k = 0; k < 34; k++)
						trial_length += distances[trial_seq.get(k)][trial_seq.get((k+1) % 34)];
					
					if (trial_length < original_length) {
						// System.out.println(String.format("Swapping edges %d, %d", i, j));
						sequence.set((i + 1) % 34, vertices_around_j[2]);
						sequence.set((j + 1) % 34, vertices_around_i[2]);
					}
				}
			}
		
		
		// END ALGORITHM
		
		System.out.println(sequence);		
		return sequence;
	}

}
