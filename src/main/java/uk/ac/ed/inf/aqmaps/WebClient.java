package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;

/**
 * Web client used for loading (and parsing) relevant data from the HTTP server.
 *
 */
public class WebClient {

	private final String host = "http://localhost";
	private final int port;
	
	private HttpClient client;
	
	public WebClient(int port) {
		this.port = port;
		// Client created once, reused for all requests
		client = HttpClient.newHttpClient();
	}
	
	private String load(String path) throws WebClientException {
		var uri = URI.create(String.format(host + ":%d/%s", port, path));
		// System.out.println("Loading: " + uri.toString());
		var request = HttpRequest.newBuilder().uri(uri).build();
		
		HttpResponse<String> response = null;
		
		// TODO mozno tu iba try-catch vypise ze co sa deje a vysle novu chybu aby hlavna aplikacia sa nejak za to postavila
		try {
			response = client.send(request, BodyHandlers.ofString());
		} catch (ConnectException e) {
			throw new WebClientException(String.format("Unable to connect to the server at %s:%d.", host, port), null);
		} catch (InterruptedException e) {
			System.err.println(String.format("InterruptedException occured when loading %s. Stack trace below:", uri.toString()));
			e.printStackTrace();
			System.exit(2);
		} catch (IOException e) {
			System.err.println(String.format("Unrecognized IOException occured when loading %s. Stack trace below:", uri.toString()));
			e.printStackTrace();
			System.exit(2);
		}
		
		if (response.statusCode() == 200) {
			// Success - we have the data
			return response.body();
		} else {
			throw new WebClientException("404 Not found: " + path, null); // TODO rename
		}
	}
	
	public ArrayList<SensorReading> loadMap(int year, int month, int day) throws WebClientException {
		System.out.println("Loading sensor map.");
		var response = load(String.format("maps/%d/%02d/%02d/air-quality-data.json", year, month, day));
												// ^^^^ ^^^^ this will insert an integer and pad it with 0s to get 2 digits
		
		// deserialize the JSON received
		Type sensorListType = new TypeToken<ArrayList<SensorReading>>() {}.getType();
		ArrayList<SensorReading> sensorList = new Gson().fromJson(response, sensorListType);
		
		System.out.println("  Translating What3Words locations.");
		for (int i = 0; i < sensorList.size(); i++) {
			SensorReading reading = sensorList.get(i);
			Point coords = loadPointFromWords(reading.location);
			reading.lat = coords.latitude();
			reading.lon = coords.longitude();
		}
		
		return sensorList;
	}
	
	public Point loadPointFromWords(String words) throws WebClientException {
		// Function for loading coordinates from W3W
		var wordsArray = words.split("\\."); // TODO viac komentarov
		var response = load(String.format("words/%s/%s/%s/details.json", wordsArray[0], wordsArray[1], wordsArray[2]));
		
		W3WDetails details = new Gson().fromJson(response, W3WDetails.class);
		return Point.fromLngLat(details.coordinates.lng, details.coordinates.lat);
	}
	
	public FeatureCollection loadNoFlyZones() throws WebClientException {
		System.out.println("Loading no fly zones.");
		var response = load("buildings/no-fly-zones.geojson");
		return FeatureCollection.fromJson(response);
	}
}
