package jp.ksgwr.array.exception;

public class DiskArrayListClassNotFoundException extends RuntimeException {

	public DiskArrayListClassNotFoundException(ClassNotFoundException e) {
		super(e);
	}

}
