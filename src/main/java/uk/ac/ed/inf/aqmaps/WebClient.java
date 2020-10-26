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

public class WebClient {

	private final String host = "http://localhost";
	private final int port;
	
	private HttpClient client;
	
	public WebClient(int port) {
		this.port = port;
		client = HttpClient.newHttpClient();
	}
	
	private String load(String path) throws WebClientException {
		var uri = URI.create(String.format(host + ":%d/%s", port, path));
		System.out.println("Loading: " + uri.toString());
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
	
	public ArrayList<SensorReading> load_map(int year, int month, int day) throws WebClientException {
		var response = load(String.format("maps/%d/%02d/%02d/air-quality-data.json", year, month, day));
												// ^^^^ ^^^^ this will insert an integer and pad it with 0s to get 2 digits
		
		// deserialize the JSON received
		Type sensorListType = new TypeToken<ArrayList<SensorReading>>() {}.getType();
		ArrayList<SensorReading> sensorList = new Gson().fromJson(response, sensorListType);
		return sensorList;
	}
}
