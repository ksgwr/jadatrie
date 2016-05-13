package jp.ksgwr.jadatrie;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class DoubleArrayTrieTest {

	private KeyDoubleArrayTrie jadatrie;

	@Before
	public void setup() {
		jadatrie = new KeyDoubleArrayTrie();
		String[] keys = new String[] {
				"aaa",
				"ab",
				"abc",
				"abcdef",
				"abef",
				"ac",
				"bcd"
			};
		jadatrie.build(keys);
	}

	@Test
	public void commonPrefixSearchTest() {
		SearchResult actual = jadatrie.commonPrefixSearch("abc");

		assertThat(actual.ids, hasItems(1, 2));
	}

	@Test
	public void exactMatchTest() {
		SearchResult actual = jadatrie.exactMatch("ab");

		assertThat(actual.ids, hasItems(1));
	}

	@Test
	public void predictiveSearchTest() {
		List<Integer> actual = jadatrie.predictiveSearch("ab");

		assertThat(actual, hasItems(1, 2, 4, 3));
	}

	@Test
	public void exactSpellerMatchTest() {
		// TODO: codeLengthのfor文は重いので可能なかぎり統合する
				// canDelete, canReplaceあたりは統合できる

		assertThat(jadatrie.exactSpellerMatch("bed").ids, hasItems(6));
		assertThat(jadatrie.exactSpellerMatch("bcd").ids, hasItems(6));
		assertThat(jadatrie.exactSpellerMatch("bce").ids, hasItems(6));

		assertThat(jadatrie.exactSpellerMatch("a").ids, hasItems(1, 5));
		assertThat(jadatrie.exactSpellerMatch("bc").ids, hasItems(2, 5, 6));
		assertThat(jadatrie.exactSpellerMatch("abcde").ids, hasItems(3));
		assertThat(jadatrie.exactSpellerMatch("ab").ids, hasItems(5, 2, 1));

		assertThat(jadatrie.exactSpellerMatch("abdc").ids, hasItems(2));
		assertThat(jadatrie.exactSpellerMatch("cabc").ids, hasItems(2));
		assertThat(jadatrie.exactSpellerMatch("abe").ids, hasItems(1, 2, 4));
		assertThat(jadatrie.exactSpellerMatch("abf").ids, hasItems(4, 1, 2));
	}

	@Test
	public void traverseNodeTest() {
		assertThat(jadatrie.traverseNode(9), is("ab#"));
	}

	@Test
	public void walkNode() {
		assertThat(jadatrie.walkNode("a"), is(1));
	}

	@Test
	public void fileTest() throws IOException {
		File file = new File("target", "test.idx");
		jadatrie.save(file);
		jadatrie.load(file);

		SearchResult actual = jadatrie.commonPrefixSearch("abc");

		assertThat(actual.ids, hasItems(1, 2));
	}
}
