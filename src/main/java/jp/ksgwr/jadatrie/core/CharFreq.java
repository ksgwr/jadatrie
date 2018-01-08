package jp.ksgwr.jadatrie.core;

public class CharFreq {

	public int code;
	public int count;

	public CharFreq(int code) {
		this.code = code;
	}

	public String toString() {
		return "c:" + code + "(" + (char) code + ")," + count;
	}
}
