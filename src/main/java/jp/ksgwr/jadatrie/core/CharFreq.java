package jp.ksgwr.jadatrie.core;

public class CharFreq {

	public final int code;
	public int count;

	public CharFreq(int code) {
		this.code = code;
	}

	public String toString() {
		return "c:" + code + "(" + (char) code + ")," + count;
	}
}
