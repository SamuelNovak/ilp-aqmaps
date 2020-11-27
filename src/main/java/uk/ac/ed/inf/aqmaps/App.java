package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;

import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;

/**
 *
 */
public class App 
{	
    public static void main(String[] args)
    {
    	// get arguments
        if (args.length != 7) {
        	System.err.println("Invalid number of arguments.\n"
        			+ "Arguments: <day> <month> <year> <latitude> <longitude> "
        			+ "<randomness-seed> <server-port>");
        	System.exit(1);
        }
        	
        
        // parse input values
        final int day, month, year, port;
        final double start_lat, start_lon;
        
        day       =   Integer.parseInt(args[0]);
        month     =   Integer.parseInt(args[1]);
        year      =   Integer.parseInt(args[2]);
        start_lat = Double.parseDouble(args[3]);
        start_lon = Double.parseDouble(args[4]);
        // skipping argument 5: this would be the randomness seed (int)
        port      =   Integer.parseInt(args[6]);
        
        
        // setup the map (list of sensor locations; in this stage of development with their readings as well)
        final ArrayList<SensorReading> map;
        final FeatureCollection noFlyZones;
        
        
        // Load data from server
        var client = new WebClient(port);
        try {
			map = client.loadMap(year, month, day);
			noFlyZones = client.loadNoFlyZones();
		} catch (WebClientException e) {
			e.printStackTrace();
			System.exit(2);
			return; // so Java doesn't complain TODO
		}
        
        
        var solver = new PathPlanner(map, noFlyZones);
        ArrayList<Point> waypoints = solver.findPath(start_lat, start_lon);
        
        var mp = new DroneController("flightpath-DD-MM-YYYY.txt", "readings-DD-MM-YYYY.geojson");
        mp.executePathPlan(waypoints);
    }
}
