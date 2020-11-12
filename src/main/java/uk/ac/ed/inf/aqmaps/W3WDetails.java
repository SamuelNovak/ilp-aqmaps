package uk.ac.ed.inf.aqmaps;

/**
 * Class used for deserializing words/.../details.json using Gson.
 *
 */
public class W3WDetails {
	String words;
	Coordinates coordinates;
	
	class Coordinates { // TODO rename a popisat co to je
		double lng, lat;
	}
}
