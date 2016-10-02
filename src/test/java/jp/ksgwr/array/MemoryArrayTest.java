package jp.ksgwr.array;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.ListIterator;

import org.junit.Before;
import org.junit.Test;

/**
 * cache機構をそれぞれ持っているのでMTSafeではない
 * MTセーフ対応版を作るなら常に全部読み込むのではなく、何番目を読み込むなどの実装が必要そう
 *
 * @author ksgwr
 *
 */
public class MemoryArrayTest {

	protected int size;

	protected ExArrayList<Integer> ary;

	@Before
	public void init() throws Exception {
		size = 20;
		ary = initExArrayList();
		for(int i=0;i<size;i++) {
			ary.set(i, i);
		}
	}

	protected ExArrayList<Integer> initExArrayList() throws Exception {
		return new MemoryArrayList<Integer>(Integer.class, size);
	}

	@Test
	public void getTest() {
		for (Integer i = 0; i < ary.size(); i++) {
			assertEquals(i, ary.get(i));
		}
	}

	@Test
	public void iteratorTest() {
		Iterator<Integer> ite = ary.iterator();
		int i=0;
		while (ite.hasNext()) {
			Integer actual = ite.next();
			assertEquals(new Integer(i++), actual);
		}
	}

	@Test
	public void listIteratorTest() {
		int i = 11;
		ListIterator<Integer> ite = ary.listIterator(i);
		while (ite.hasPrevious()) {
			Integer actual = ite.previous();
			assertEquals(new Integer(i--), actual);
		}
	}


	@Test
	public void resizeExpandTest() {
		ary.resize(25);
		for (int i=20;i<25;i++) {
			ary.set(i, i);
		}

		for (Integer i = 0; i < ary.size(); i++) {
			assertEquals(i, ary.get(i));
		}
	}

	@Test
	public void resizeReduceTest() {
		ary.resize(15);

		assertEquals(15, ary.size());
		for (Integer i = 0; i < ary.size(); i++) {
			assertEquals(i, ary.get(i));
		}
	}

	@Test
	public void compressTest() {
		for (int i=15;i<20;i++) {
			ary.set(i, null);
		}
		ary.compress();

		assertEquals(15, ary.size());

		ary.resize(25);
		ary.compress();

		assertEquals(15, ary.size());
	}

	@Test
	public void addTest() {
		for (int i = 20; i < 45; i++) {
			ary.add(i);
		}

		assertEquals(45, ary.size());
		for (Integer i = 0; i < ary.size(); i++) {
			assertEquals(i, ary.get(i));
		}
	}
}
