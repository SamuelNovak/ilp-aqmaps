package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.List;

import com.mapbox.geojson.Point;

public class Obstacle extends ArrayList<Point>
{
	// not using serialization for this class, so only the default value is here so that Java is happy
	// if in future you want to use serialization for this class, this needs to be modified
	private static final long serialVersionUID = 1L;
	
	public static Obstacle fromList(List<Point> list) {
		var obs = new Obstacle();
		for (var point : list)
			obs.add(point);
		return obs;
	}
}
