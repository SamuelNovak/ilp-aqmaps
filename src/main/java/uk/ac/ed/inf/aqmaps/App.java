package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;

import com.mapbox.geojson.FeatureCollection;

/**
 *
 */
public class App 
{
	
	public static double lat_max = 55.946233; // latitude
	public static double lat_min = 55.942617;
	public static double lon_max = -3.184319; // longitude
	public static double lon_min = -3.192473;
	public static double move_length = 0.0003;
	
    public static void main(String[] args)
    {
    	// get arguments
        if (args.length != 7) {
        	System.err.println("Invalid number of arguments.\n"
        			+ "Arguments: <day> <month> <year> <latitude> <longitude> "
        			+ "<randomness-seed> <server-port>");
        	System.exit(1);
        }
        
        int day, month, year, rand_seed, port;
        double start_lat, start_lon;
        
        day       =   Integer.parseInt(args[0]);
        month     =   Integer.parseInt(args[1]);
        year      =   Integer.parseInt(args[2]);
        start_lat = Double.parseDouble(args[3]);
        start_lon = Double.parseDouble(args[4]);
        rand_seed =   Integer.parseInt(args[5]);
        port      =   Integer.parseInt(args[6]);
        
        
        ArrayList<SensorReading> map;
        FeatureCollection noFlyZones;
        
        
        // Loading data from webserver
        WebClient client = new WebClient(port);
        try {
			map = client.loadMap(year, month, day);
			noFlyZones = client.loadNoFlyZones();
		} catch (WebClientException e) {
			e.printStackTrace();
			System.exit(2);
			return; // so Java doesn't complain TODO
		}
        
        
        PathSolver solver = new PathSolver(map, noFlyZones, rand_seed);
        ArrayList<Double> moves = solver.findPath(start_lat, start_lon);
        
    }
}
