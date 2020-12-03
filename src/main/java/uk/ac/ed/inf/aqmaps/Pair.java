package uk.ac.ed.inf.aqmaps;

/** Arbitrary pair (2-tuple) of possibly different types. Elements are final and directly accessible.
 * Meant to be viewed as a pure data type.
 */
public class Pair<Left, Right> {
	public final Left left;
	public final Right right;
	
	public Pair(Left left, Right right) {
		this.left = left;
		this.right = right;
	}
	
	public Pair<Left, Right> clone() {
		return new Pair<Left, Right>(left, right);
	}
}
