package jp.ksgwr.array.exception;

import java.io.IOException;

public class DiskArrayListIOException extends RuntimeException {

	public DiskArrayListIOException(IOException e) {
		super(e);
	}

}
