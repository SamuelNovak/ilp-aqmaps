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
	
	public boolean equal(RotationDirection direction) {
		return getValue() == direction.getValue();
	}

	public RotationDirection invert() {
		if (value == 0)
			return RotationDirection.None;
		else if (value == 1)
			return RotationDirection.Negative;
		else
			return RotationDirection.Positive;
	}
}