package uk.ac.ed.inf.aqmaps;

/** Class used for deserializing words/.../details.json using Gson.
 */
public class W3WDetails {
	String words;
	CoordinatesObject coordinates;
	
	/** Represents a nested object containing coordinates in the JSON.
	 */
	class CoordinatesObject {
		double lng, lat;
	}
}
