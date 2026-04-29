package cz.bliksoft.meshcore;

/**
 * Unchecked exception thrown when a MeshCore companion device returns an error
 * response or when a protocol-level error occurs during communication.
 */
public class CompanionErrorException extends RuntimeException {
	private static final long serialVersionUID = 500015173941924542L;

	/**
	 * Creates a new exception with the given error message.
	 *
	 * @param error the error message from the device or protocol layer
	 */
	public CompanionErrorException(String error) {
		super(error);
	}
}
