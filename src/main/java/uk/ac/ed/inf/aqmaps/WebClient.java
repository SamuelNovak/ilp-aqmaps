package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;

public class WebClient {

	private final String host = "http://localhost";
	private final int port;
	
	private HttpClient client;
	
	public WebClient(int port) {
		this.port = port;
		client = HttpClient.newHttpClient();
	}
	
	public void load_map(int year, int month, int day) {
		var uri = URI.create(String.format(host + ":%d/%d/%d/%d/air-quality-data.json",
							 port, year, month, day));
		var request = HttpRequest.newBuilder().uri(uri).build();
		
		// TODO mozno tu iba try-catch vypise ze co sa deje a vysle novu chybu aby hlavna aplikacia sa nejak za to postavila
		try {
			var response = client.send(request, BodyHandlers.ofString());
		} catch (ConnectException e) {
			System.err.println(String.format("Unable to connect to the server at %s:%d.", host, port));
			System.exit(2);
		} catch (InterruptedException e) {
			System.err.println(String.format("InterruptedException occured when loading %s. Stack trace below:", uri.toString()));
			e.printStackTrace();
			System.exit(2);
		} catch (IOException e) {
			System.err.println(String.format("Unrecognized IOException occured when loading %s. Stack trace below:", uri.toString()));
			e.printStackTrace();
			System.exit(2);
		}
		
		
	}
}
