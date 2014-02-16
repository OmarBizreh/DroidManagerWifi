package droidmanager.wifi;

public class LowMemoryException extends Exception {

	private String message;

	public LowMemoryException() {
		super();
		this.message = "Not enough memory space available";
	}

	@Override
	public String toString() {
		return message;
	}

	@Override
	public String getMessage() {
		return message;
	}

}
