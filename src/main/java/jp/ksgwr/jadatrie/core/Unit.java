package jp.ksgwr.jadatrie.core;

import java.io.Serializable;

/**
 *
 * Suppose that there are n nodes in the trie, and the alphabet is of size m.
 * The size of the double-array structure would be n + cm, where c is a coefficient which is dependent on the characteristic of the trie.
 * And the time complexity of the brute force algorithm would be O(nm + cm2).
 *
 * @author ksgwr
 *
 */
public class Unit implements Serializable {

	public int base;
	public int check;

	public Unit(int base,int check) {
		this.base = base;
		this.check = check;
	}

	public String toString() {
		return "Unit(" + base + "," + check + ")";
	}
}
