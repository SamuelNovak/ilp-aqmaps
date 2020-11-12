package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;

public class DroneController {

	private String flightpathFilename;
	private String readingsMapFilename;
	
	public DroneController(String flightpathFilename, String readingsMapFilename) {
		this.flightpathFilename = flightpathFilename;
		this.readingsMapFilename = readingsMapFilename;
	}

	public void export(ArrayList<Move> moves) {
		
	}

}
