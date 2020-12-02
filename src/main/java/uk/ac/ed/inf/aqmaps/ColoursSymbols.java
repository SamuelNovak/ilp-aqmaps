package uk.ac.ed.inf.aqmaps;

public class ColoursSymbols {

	/*
	 * Enum of all colours used, with their respective colour codes.
	 */
	public enum Colour {
		Green("#00ff00"),
		MediumGreen("#40ff00"),
		LightGreen("#80ff00"),
		LimeGreen("#c0ff00"),
		Gold("#ffc000"),
		Orange("#ff8000"),
		RedOrange("#ff4000"),
		Red("#ff0000"),
		Black("#000000"),
		Gray("#aaaaaa");

		// Internal value, and constructor so that we can have enum elements that contain a value
		private String value;
		Colour(String colour) {
			this.value = colour;
		}
		
		/** Function to get an enumerated colour from pollution level.
		 * @param pollution Assumed to be valid (0 <= pollution < 256)
		 */
		public static Colour getFromPollution(double pollution) {
			if (pollution < 32)
				return Green;
			else if (pollution < 64)
				return MediumGreen;
			else if (pollution < 96)
				return LightGreen;
			else if (pollution < 128)
				return LimeGreen;
			else if (pollution < 160)
				return Gold;
			else if (pollution < 192)
				return Orange;
			else if (pollution < 224)
				return RedOrange;
			else // assumes (pollution < 256)
				return Red;
		}

		/** Get hex colour code.
		 */
		public String getValue() {
			return value;
		}
	}
	
	public enum Symbol {
		Lighthouse("lighthouse"),
		Danger("danger"),
		Cross("cross"),
		None(null);
		
		private String value;
		private Symbol(String symbolString) {
			value = symbolString;
		}
		
		/** Get marker symbol string.
		 */
		public String getValue() {
			return value;
		}
		
		/** Function to get enumerated marker symbol from pollution value.
		 * @param pollution Assumed to be valid (0 <= pollution < 256)
		 */
		public static Symbol getFromPollution(double pollution) {
			if (pollution < 128)
				return Lighthouse;
			else
				return Danger;
		}
	}
	
	public static Pair<Colour,Symbol> getColourSymbol(String reading, double battery) {
		if (battery < 10)
			// Sensor cannot be trusted
			return new Pair<Colour,Symbol>(Colour.Black, Symbol.Cross);
		
		// otherwise, get correct colour and symbol
		var pollution = Double.parseDouble(reading);
		return new Pair<Colour,Symbol>(Colour.getFromPollution(pollution), Symbol.getFromPollution(pollution));
	}
	
	// constant value for unused sensor
	private final static Pair<Colour, Symbol> notVisited = new Pair<Colour,Symbol>(Colour.Gray, Symbol.None);
	
	public static Pair<Colour,Symbol> getNotVisited() {
		return notVisited;
	}
}
