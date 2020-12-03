package uk.ac.ed.inf.aqmaps;

enum RotationDirection {
	Both(0),
	Positive(1),
	Negative(-1);

	private int value;
	RotationDirection(int value) {
		this.value = value;
	}
	
	private int getValue() {
		return value;
	}
	
	public boolean allowsPositive() {
		return (value >= 0);
	}
	
	public boolean allowsNegative() {
		return (value <= 0);
	}
	
	public boolean equals(RotationDirection dir) {
		return (value == dir.getValue());
	}
}