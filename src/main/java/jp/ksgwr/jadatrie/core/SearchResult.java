package jp.ksgwr.jadatrie.core;

import java.util.List;

public class SearchResult {

	public final List<Integer> ids;

	public final int base;

	public final int i;

	public SearchResult(List<Integer> ids, int base, int i) {
		this.ids = ids;
		this.base = base;
		this.i = i;
	}
}
