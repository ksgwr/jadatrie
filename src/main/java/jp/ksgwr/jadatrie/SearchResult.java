package jp.ksgwr.jadatrie;

import java.util.List;

public class SearchResult {

	public List<Integer> ids;

	public int base;

	public int i;

	public SearchResult(List<Integer> ids, int base, int i) {
		this.ids = ids;
		this.base = base;
		this.i = i;
	}
}
