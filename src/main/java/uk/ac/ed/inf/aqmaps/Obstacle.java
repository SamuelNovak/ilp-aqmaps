package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.List;

import com.mapbox.geojson.Point;

@SuppressWarnings("serial") // we do not need this to be serializable
public class Obstacle extends ArrayList<Point>
{	
	public static Obstacle fromList(List<Point> list) {
		var obs = new Obstacle();
		for (var point : list)
			obs.add(point);
		return obs;
	}
}
