package uk.ac.ed.inf.aqmaps;

public class WebClientException extends Exception {
	// required by Java
	private static final long serialVersionUID = 1L;
	
	private final String comment;
	// for the case when the error is a general one, and it is not easily identified
	private final Exception originalException;
	
	public WebClientException(String comment, Exception originalException) {
		this.comment = comment;
		this.originalException = originalException;
	}
	
	public void printStackTrace() {
		System.err.print(this.getClass().getName());
		
		if (comment != null)
			System.err.println(": " + comment);
		else
			System.err.println();
		
		if (originalException != null) {
			System.err.println("Original stack trace:");
			originalException.printStackTrace(System.err);
		}
	}
}
