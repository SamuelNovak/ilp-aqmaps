package uk.ac.ed.inf.aqmaps;

/**
 * Class for deserializing maps/.../air-quality-data.json using Gson
 *
 */
public class SensorReading {
	String location;
	double lat, lon;
	
	double battery;
	String reading;
}
