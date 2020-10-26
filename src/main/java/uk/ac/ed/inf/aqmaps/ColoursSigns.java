package uk.ac.ed.inf.aqmaps;

public class ColoursSigns {

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
		
		/* Function to get an enumerated colour from pollution level.
		 * Assumes valid pollution value (0 <= pollution < 256) */
		public static Colour getFromPollution(int pollution) {
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

		// getter for the value (hex code)
		public String getValue() {
			return value;
		}
	}
	
	public enum Sign {
		
	}
}
