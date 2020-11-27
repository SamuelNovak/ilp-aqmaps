package uk.ac.ed.inf.aqmaps;

import com.mapbox.geojson.Point;

/**
 * Class for deserializing maps/.../air-quality-data.json using Gson
 *
 */
public class SensorReading {
	String location;
	double lat, lon;
	
	double battery;
	String reading;
	
	public Point toPoint() {
		return Point.fromLngLat(lon, lat);
	}
}
