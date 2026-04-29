package cz.bliksoft.meshcore;

public class CompanionErrorException extends RuntimeException {
	private static final long serialVersionUID = 500015173941924542L;

	public CompanionErrorException(String error) {
		super(error);
	}
}
