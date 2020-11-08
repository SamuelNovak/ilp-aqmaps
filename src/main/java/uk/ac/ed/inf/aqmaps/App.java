package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;

import com.mapbox.geojson.FeatureCollection;

/**
 *
 */
public class App 
{
	
	// Flight boundaries
	public static final double LAT_MAX = 55.946233; // latitude
	public static final double LAT_MIN = 55.942617;
	public static final double LON_MAX = -3.184319; // longitude
	public static final double LON_MIN = -3.192473;
	public static final double MOVE_LENGTH = 0.0003; // length of EVERY drone step in degrees
	
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
        final int day, month, year, rand_seed, port;
        final double start_lat, start_lon;
        
        day       =   Integer.parseInt(args[0]);
        month     =   Integer.parseInt(args[1]);
        year      =   Integer.parseInt(args[2]);
        start_lat = Double.parseDouble(args[3]);
        start_lon = Double.parseDouble(args[4]);
        rand_seed =   Integer.parseInt(args[5]);
        port      =   Integer.parseInt(args[6]);
        
        
        // setup the map (list of sensor locations - in this stage of development with their readings as well)
        ArrayList<SensorReading> map;
        FeatureCollection noFlyZones;
        
        
        // Loading data from webserver
        var client = new WebClient(port);
        try {
			map = client.loadMap(year, month, day);
			noFlyZones = client.loadNoFlyZones();
		} catch (WebClientException e) {
			e.printStackTrace();
			System.exit(2);
			return; // so Java doesn't complain TODO
		}
        
        
        var solver = new PathSolver(map, noFlyZones, rand_seed);
        ArrayList<Move> moves = solver.findPath(start_lat, start_lon); // TODO typ?
        
        var mp = new MovePrinter("flightpath-DD-MM-YYYY.txt", "readings-DD-MM-YYYY.geojson");
        mp.export(moves);
    }
}
