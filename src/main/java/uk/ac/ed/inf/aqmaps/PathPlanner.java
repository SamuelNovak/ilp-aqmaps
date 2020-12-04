package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;

import com.mapbox.geojson.Point;

public class PathPlanner {
	
	private final ArrayList<SensorReading> sensorList;
	private final int NUMBER_OF_SENSORS;
	
	// matrix for distances between nodes (undirected weighted graph)
	private double[][] distances;

	public PathPlanner(ObstacleEvader evader, ArrayList<SensorReading> sensorList) {		
		this.sensorList = sensorList;
		this.NUMBER_OF_SENSORS = sensorList.size();
		
		// Compute the distance matrix
		distances = new double[NUMBER_OF_SENSORS + 1][NUMBER_OF_SENSORS + 1]; // (NUMBER_OF_SENSORS sensors) + (1 starting location)
		for (int i = 0; i < NUMBER_OF_SENSORS; i++)
			for (int j = 0; j <= i; j++) {
				if (i == j) distances[i][j] = 0;
				else {
					var iPoint = sensorList.get(i).toPoint();
					var jPoint = sensorList.get(j).toPoint();
					
					// TODO
					distances[i][j] = !evader.crossedObstacles(iPoint, jPoint).isEmpty() ? 10 : distance(iPoint, jPoint); // evader.evasionDistance(point_i, point_j);
					
					// distance matrix is symmetric
					distances[j][i] = distances[i][j];
				}
			}
	}
	
	// TODO identifier
	static double distance(Point x, Point y) {
		return Math.hypot(x.latitude() - y.latitude(), x.longitude() - y.longitude());
	}

	// Path plan will be a sequence of waypoints for the drone
	// TODO explanation: so in the future the drone can correct for weather etc., now it only knows the general path (but this will include waypoints to help avoid the no fly zones)
	public ArrayList<Point> findPath(double startLatitude, double startLongitude) {
		
		// calculate the distances to the starting location - vertex at index NUMBER_OF_SENSORS
		for (int i = 0; i < NUMBER_OF_SENSORS; i++) {
			distances[NUMBER_OF_SENSORS][i] = Math.hypot(startLatitude - sensorList.get(i).lat, startLongitude - sensorList.get(i).lon);
			distances[i][NUMBER_OF_SENSORS] = distances[NUMBER_OF_SENSORS][i];
		}
		
		// generate a high-level flight plan - sequence of vertices (sensors) in a good order to visit them
		var tspSequence = solveTSP();

		// waypoints that will guide the drone
		var waypoints = new ArrayList<Point>();
		
		// TODO
		for (int i = 0; i < NUMBER_OF_SENSORS + 1; i++) {
			Point currentPoint, nextPoint;
			
			// assign the correct current and next points - take into account that one point might be the starting point (so it is not in not in this.sensorList)
			if (i == 0) {
				// starting point (vertex NUMBER_OF_SENSORS)
				currentPoint = Point.fromLngLat(startLongitude, startLatitude);
				// writing i+1 explicitly for clarity
				nextPoint = sensorList.get(tspSequence.get(i+1)).toPoint();
			} else if (i == NUMBER_OF_SENSORS) {
				// we are at the last vertex, need to go back
				currentPoint = sensorList.get(tspSequence.get(i)).toPoint();
				nextPoint = Point.fromLngLat(startLongitude, startLatitude);
			} else {
				// general part of the sequence
				currentPoint = sensorList.get(tspSequence.get(i)).toPoint();
				nextPoint = sensorList.get(tspSequence.get(i + 1)).toPoint();
			}
			
			waypoints.add(currentPoint);

			// TODO remove this
			/*var obstacles = evader.crossedObstacles(currentPoint, nextPoint);
			if (!obstacles.isEmpty()) {
				System.out.println("Need to avoid");
				// TODO evasion algorithm
				// done by the evader
				// TODO wtf is this print
				System.out.println(String.format("Adding %d waypoints.", obstacles.size()));
				waypoints.addAll(evader.waypointsToAvoidAllObstacles(currentPoint, nextPoint));
			}*/
		}
		
		// loop back
		waypoints.add(waypoints.get(0));		
		return waypoints;
	}
	
	/** Generate a heuristically good sequence of vertices (sensors) to visit - Travelling Salesperson Problem
	 * @return Sequence of vertices identified by their index in this.sensorList.
	 * Guaranteed to start with vertex index NUMBER_OF_SENSORS (the starting point).
	 */
	private ArrayList<Integer> solveTSP() {
		
		// get initial sequence of vertices from Nearest Insert
		var sequence = nearestInsert();

		// optimize the obtained sequence by Swap + 2-Opt (in place)
		swap2opt(sequence);

		// need to rotate the sequence (i.e. cyclical shift) so that it starts at the starting location (vertex index NUMBER_OF_SENSORS)
		{
			// find the current index of the initial vertex (vertex with index NUMBER_OF_SENSORS) in the sequence
			int initialVertexIndex = 0;
			for (int i = 0; i < NUMBER_OF_SENSORS + 1; i++) {
				if (sequence.get(i) == NUMBER_OF_SENSORS) {
					initialVertexIndex = i;
					break;
				}
			}
			
			var rotatedSequence = new ArrayList<Integer>();
			for (int i = 0; i < NUMBER_OF_SENSORS + 1; i++)
				rotatedSequence.add(sequence.get((i + initialVertexIndex) % (NUMBER_OF_SENSORS + 1)));
			
			sequence = rotatedSequence;
		}
		return sequence;
	}
	
	/** Generate a TSP circuit (as a sequence of vertices) using the Nearest Insert heuristic
	 * @return Sequence of vertices identified by their index in this.sensorList
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
			// finding the nearest pair of sensors
			for (int i = 1; i < NUMBER_OF_SENSORS + 1; i++) {
				for (int j = i + 1; j < NUMBER_OF_SENSORS + 1; j++) {
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
			for (int i = 0; i < NUMBER_OF_SENSORS + 1; i++)
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
			
			// find an unused vertex that is nearest to some other vertex which is already in the sequence 
			for (int i = 0; i < sequence.size(); i++) {
				// i corresponds to index of a vertex already used in the sequence
				for (var u : unused) {
					// u is an unused vertex
					if (distances[sequence.get(i)][u] < min) {
						min = distances[sequence.get(i)][u];
						mini = i;
						minu = u;
					}
				}
			}
			
			// now need to decide on which side of mini to insert minu - before or after
			var distBefore = distances[(sequence.size() + mini - 1) % sequence.size()][minu];
			var distAfter  = distances[(mini + 1) % sequence.size()][minu];
			if (distBefore < distAfter) {
				// insert to sequence at index mini
				sequence.add(mini, minu);
			} else {
				// insert to sequence and index (mini + 1) mod size
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
		for (int i = 0; i < NUMBER_OF_SENSORS + 1; i++)
			for (int j = 0; j < NUMBER_OF_SENSORS + 1; j++) {
				if (i == j)
					continue;
				
				// Prepare vertices sequence[i-1] --- sequence[i] --- sequence[i+1] (mod (NUMBER_OF_SENSORS + 1))
				// same for sequence[j-1] --- sequence[j] --- sequence[j+1] (mod (NUMBER_OF_SENSORS + 1))
				var verticesAroundI = new int[4];
				var verticesAroundJ = new int[4];
				
				for (int k = -1; k < 3; k++) {
					verticesAroundI[k+1] = sequence.get(((NUMBER_OF_SENSORS + 1) + i + k) % (NUMBER_OF_SENSORS + 1));
					verticesAroundJ[k+1] = sequence.get(((NUMBER_OF_SENSORS + 1) + j + k) % (NUMBER_OF_SENSORS + 1));
				}
				
				// check if swapping vertices on the same line makes it better
				// this will compare path [i-1] --- [i] --- [i+1] --- [i+2] vs
				// the path               [i-1] --- [i+1] --- [i] --- [i+2]
				// where [a] means sequence.get(a)
				// this is a very local optimization
				{
					double originalLength = 0;
					var swappedLength = distances[verticesAroundI[2]][verticesAroundI[1]];
					for (int k = 0; k < verticesAroundI.length - 1; k++) {
						originalLength += distances[verticesAroundI[k]][verticesAroundI[k+1]];
						if (k < 2)
							swappedLength += distances[verticesAroundI[k]][verticesAroundI[k+2]];
					}
					
					// check if this produced a better overall path, if so update the sequence
					if (swappedLength < originalLength) {
						sequence.set(i, verticesAroundI[2]);
						sequence.set((i+1) % (NUMBER_OF_SENSORS + 1), verticesAroundI[1]);
					}
				}				
				
				// see if swapping endpoints of two different edges produces a shorter overall path length
				{						
					// use global path length (straight line distances of the entire circuit)
					double originalLength = 0;
					for (int k = 0; k < NUMBER_OF_SENSORS + 1; k++)
						originalLength += distances[sequence.get(k)][sequence.get((k+1) % (NUMBER_OF_SENSORS + 1))];
					
					// create a trial sequence by swapping the endpoints of two edges and compute the global path
					var trialSequence = new ArrayList<Integer>();
					for (int k = 0; k < (NUMBER_OF_SENSORS + 1); k++)
						trialSequence.add((int) sequence.get(k));
					trialSequence.set((i + 1) % (NUMBER_OF_SENSORS + 1), verticesAroundJ[2]);
					trialSequence.set((j + 1) % (NUMBER_OF_SENSORS + 1), verticesAroundI[2]);
					
					double trialLength = 0;
					for (int k = 0; k < (NUMBER_OF_SENSORS + 1); k++)
						trialLength += distances[trialSequence.get(k)][trialSequence.get((k+1) % (NUMBER_OF_SENSORS + 1))];
					
					// check if this produced a better overall path, if so update the sequence
					if (trialLength < originalLength) {
						sequence.set((i + 1) % (NUMBER_OF_SENSORS + 1), verticesAroundJ[2]);
						sequence.set((j + 1) % (NUMBER_OF_SENSORS + 1), verticesAroundI[2]);
					}
				}
			}
	}

}
