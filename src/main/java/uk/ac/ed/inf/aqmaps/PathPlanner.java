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
						distances[i][j] += 0;//evader.evasionDistance(point_i, point_j, obs);
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
		
		// calculate the distances to the starting location - vertex at index 33
		for (int i = 0; i < 33; i++) {
			distances[33][i] = Math.hypot(start_lat - map.get(i).lat, start_lon - map.get(i).lon);
			distances[i][33] = distances[33][i];
		}
		
		// generate a high-level flight plan - sequence of vertices (sensors) in a good order to visit them
		var tsp_sequence = solveTSP();

		var waypoints = new ArrayList<Point>();
		
		for (int i = 0; i < 34; i++) {
			Point current_point, next_point;
			
			if (i == 0) {
				// starting point (vertex 33)
				current_point = Point.fromLngLat(start_lon, start_lat);
				next_point = map.get(tsp_sequence.get(i+1)).toPoint(); // writing i+1 explicitly for clarity
			} else if (i == 33) {
				// we are at the last vertex, need to go back
				current_point = map.get(tsp_sequence.get(i)).toPoint();
				next_point = Point.fromLngLat(start_lon, start_lat);
			} else {
				current_point = map.get(tsp_sequence.get(i)).toPoint();
				next_point = map.get(tsp_sequence.get(i + 1)).toPoint();
			}
			
			waypoints.add(current_point);

			var obstacles = evader.crossedObstacles(current_point, next_point);
			if (!obstacles.isEmpty()) {
				System.out.println("Need to avoid");
				// TODO evasion algorithm
				// done by the evader
				for (var obs : obstacles) {
					waypoints.addAll(evader.waypointsEvadeObstacle(current_point, next_point, obs));
				}
			}
			
			// TODO don't deal with angles here because that will be drone's job (and done by drone so it can in the *future* deal with wind)
			
			// calculate the angle (from conventional 0 being the +x = increasing longitude)
			// also we want this angle to be positive (hence the +360 mod 360)
			var delta_lon = next_point.longitude() - current_point.longitude();
			var delta_lat = next_point.latitude() - current_point.latitude();
			var theta = (Math.toDegrees(Math.atan2(delta_lat, delta_lon)) + 360) % 360;
			
			// get angle that is the nearest multiple of 10 degrees
			var phi = 10 * Math.round(theta / 10);
			
			// check if the direction of travel needed is already allowed
			if (theta != phi) {				
				// calculate the midpoint so that the path can be split into two paths of allowed angles
				var sigma = theta - phi;
				var dist = Math.hypot(delta_lon, delta_lat);
				
				var move_in_phi = dist * Math.cos(Math.toRadians(sigma));
				// var move_in_
				var new_delta_lon = move_in_phi * Math.cos(Math.toRadians(phi));
				var new_delta_lat = move_in_phi * Math.sin(Math.toRadians(phi));
				
				waypoints.add(Point.fromLngLat(current_point.longitude() + new_delta_lon, current_point.latitude() + new_delta_lat));
			
			}
			
			
			System.out.println(theta);
			System.out.println(phi);
		}
		
		
		// TODO this is DEBUG
		{
			var pts = new ArrayList<Point>();
			var ftrs = new ArrayList<Feature>();
			for (var i : tsp_sequence) {
				if (i == 33)
					pts.add(Point.fromLngLat(start_lon, start_lat));
				else
					pts.add(Point.fromLngLat(map.get(i).lon, map.get(i).lat));
				
				var pt_marker = Feature.fromGeometry(pts.get(pts.size() - 1));
				pt_marker.addNumberProperty("Vertex id", i);
				ftrs.add(pt_marker);
			}
			{
				var i = tsp_sequence.get(0);
				if (i == 33)
					pts.add(Point.fromLngLat(start_lon, start_lat));
				else
					pts.add(Point.fromLngLat(map.get(i).lon, map.get(i).lat));
			}
			var ftr = Feature.fromGeometry((Geometry) LineString.fromLngLats(pts));

			ftrs.addAll(noFlyZones.features());
			ftrs.add(ftr);
			FeatureCollection col = FeatureCollection.fromFeatures(ftrs);
			System.out.println(col.toJson());
		}
		// END DEBUG
		
		return null;
	}
	
	/** Generate a heuristically good sequence of vertices (sensors) to visit - Travelling Salesperson Problem
	 * @return Sequence of vertices identified by their index in this.map.
	 * Guaranteed to start with vertex 33 (the starting point).
	 */
	private ArrayList<Integer> solveTSP() { // TODO: name
		
		// get initial sequence of vertices from Nearest Insert
		var sequence = nearestInsert();

		// optimize the obtained sequence by Swap + 2-Opt (in place)
		swap2opt(sequence);

		// need to rotate the sequence (i.e. cyclical shift) so that it starts at the starting location (vertex number 33)
		{
			int initial_vertex_index = 0;
			for (int i = 0; i < 34; i++) {
				if (sequence.get(i) == 33) {
					initial_vertex_index = i;
					break;
				}
			}
			
			var rotated_sequence = new ArrayList<Integer>();
			for (int i = 0; i < 34; i++)
				rotated_sequence.add(sequence.get((i + initial_vertex_index) % 34));
			
			sequence = rotated_sequence;
		}
		
		// TODO remove print
		System.out.println(sequence);
		
		{
			double total_distance = 0;
			for (int i = 0; i < 34; i++) {
				total_distance += distances[sequence.get(i)][sequence.get((i+1) % 34)];
			}
			System.out.print("Total TSP distance: ");
			System.out.println(total_distance);
		}
		
		return sequence;
	}
	
	/** Generate a TSP circuit (as a sequence of vertices) using the Nearest Insert heuristic
	 * @return Sequence of vertices identified by their index in this.map
	 */
	private ArrayList<Integer> nearestInsert() {
		// sequence of sensors to visit, identified by their index in this.map
		var sequence = new ArrayList<Integer>();
		
		// so far unused sensors
		var unused = new ArrayList<Integer>();
		
		// find the first two vertices to connect
		// isolating this block so it's clear where the local vars belong
		{
			var min = distances[0][1];
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
		
		// expand the TSP cycle with the nearest vertices, until all of them are used
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
		
		return sequence;
	}
	
	
	/** Method to optimize an existing TSP circuit using a mixed Swap & 2-Opt heuristics - done in place
	 * @param sequence An existing TSP circuit - sequence of vertex ids (indices in this.map)
	 */
	// TODO: pokec o tom, ze z pokusov sa zda, ze takto ich zmiesat je lepsie
	private void swap2opt(ArrayList<Integer> sequence) {
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
	}

}
