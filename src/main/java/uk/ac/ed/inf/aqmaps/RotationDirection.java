package uk.ac.ed.inf.aqmaps;

enum RotationDirection {
	None(0),
	Positive(1),
	Negative(-1);

	private int value;
	RotationDirection(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}
	
	public boolean equals(RotationDirection direction) {
		return getValue() == direction.getValue();
	}
}