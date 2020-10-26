package uk.ac.ed.inf.aqmaps;

@SuppressWarnings("serial") // TODO je to problem?
public class WebClientException extends Exception {

	private String comment;
	private Exception original; // for the case when the error is a general one, and it is not easily identifies
	
	// TODO naco mi to je, ale to asi pojde do reportu
	// Akoze aby boli uniformne vypadky z klienta
	
	public WebClientException(String comment, Exception original_exc) {
		this.comment = comment;
		this.original = original_exc;
	}
	
	public void printStackTrace() {
		// TODO pretty print
		if (comment != null)
			System.err.print(comment + " ");
		if (original != null) {
			System.err.println("Original stack trace:");
			original.printStackTrace(System.err);
		} else
			System.err.println();
	}
}
