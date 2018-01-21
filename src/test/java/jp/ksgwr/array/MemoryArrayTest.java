package jp.ksgwr.array;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * cache機構をそれぞれ持っているのでMTSafeではない
 * MTセーフ対応版を作るなら常に全部読み込むのではなく、何番目を読み込むなどの実装が必要そう
 * => compressで組み直せばMTセーフになるので問題にはなりにくい(CachedMemoryArrayListに実装を移した)
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

	protected ExArrayList<Integer> initExArrayListDefaultZero() throws Exception {
		return new MemoryArrayList<Integer>(Integer.class, new Integer(0), size);
	}


	@After
	public void close() throws IOException {
		ary.close();
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

	@Test
	public void addIndexTest() {
		int oldSize = ary.size();
		Integer newVal = -1;

		ary.add(1, newVal);

		assertEquals(oldSize + 1, ary.size());
		assertEquals(new Integer(0), ary.get(0));
		assertEquals(newVal, ary.get(1));
		for (Integer i = 2; i < ary.size(); i++) {
			assertEquals(new Integer(i - 1), ary.get(i));
		}
	}

	@Test
	public void addIndex2Test() {
		int oldSize = ary.size();
		Integer newVal = -1;

		ary.add(15, newVal);

		assertEquals(oldSize + 1, ary.size());
		for (Integer i = 0; i < 15; i++) {
			assertEquals(new Integer(i), ary.get(i));
		}
		assertEquals(newVal, ary.get(15));
		for (Integer i = 16; i < ary.size(); i++) {
			assertEquals(new Integer(i - 1), ary.get(i));
		}
	}

	@Test
	public void addAllTest() {
		List<Integer> newVals = Arrays.asList(new Integer[]{20, 21, 22, 23, 24});

		ary.addAll(newVals);

		assertEquals(25, ary.size());
		for (Integer i = 0; i < ary.size(); i++) {
			assertEquals(i, ary.get(i));
		}
	}

	@Test
	public void addAll2Test() {
		Integer[] newVals = new Integer[]{21, 22, 23, 24};

		ary.add(20);
		ary.addAll(newVals);

		assertEquals(25, ary.size());
		for (Integer i = 0; i < 25; i++) {
			assertEquals(i, ary.get(i));
		}
	}

	@Test
	public void addAllIndexTest() {
		List<Integer> newVals = Arrays.asList(new Integer[]{-1, -2, -3, -4, -5});

		ary.addAll(0, newVals);

		assertEquals(25, ary.size());
		for (Integer i = 0; i < 5; i++) {
			assertEquals(new Integer(-1 * (i + 1)), ary.get(i));
		}
		for (Integer i = 5; i < ary.size(); i++) {
			assertEquals(new Integer(i - 5), ary.get(i));
		}
	}

	@Test
	public void addAllIndex2Test() {
		Integer[] newVals = new Integer[]{-5, -6, -7, -8, -9};

		ary.addAll(5, newVals);

		assertEquals(25, ary.size());
		for (Integer i = 0; i < 5; i++) {
			assertEquals(i, ary.get(i));
		}
		for (Integer i = 5; i < 10; i++) {
			assertEquals(new Integer(-1 * i), ary.get(i));
		}
		for (Integer i = 10; i < ary.size(); i++) {
			assertEquals(new Integer(i - 5), ary.get(i));
		}
	}

	@Test
	public void removeAllTest() {
		// remove 0,5,10,15
		List<Integer> removeCol = Arrays.asList(new Integer[]{20, 15, 10, 5, 0});

		ary.removeAll(removeCol);

		assertEquals(16, ary.size());
		for (Integer val = 0, i = 0; val < 20; val++) {
			if (val % 5 != 0) {
				assertEquals(val, ary.get(i++));
			}
		}
	}

	@Test
	public void retainAllTest() {
		List<Integer> retainCol = Arrays.asList(new Integer[]{0, 5, 10, 15, 20});

		ary.add(20);
		ary.retainAll(retainCol);

		assertEquals(retainCol.size(), ary.size());
		for (Integer i = 0; i < ary.size(); i++) {
			assertEquals(retainCol.get(i), ary.get(i));
		}
	}

	@Test
	public void removeIndexTest() {
		int oldSize = ary.size();

		Integer removeVal = ary.remove(1);

		assertEquals(oldSize - 1, ary.size());
		assertEquals(new Integer(1), removeVal);
		for (Integer i = 1; i < ary.size(); i++) {
			assertEquals(new Integer(i + 1), ary.get(i));
		}
	}

	@Test
	public void removeIndex2Test() {
		int oldSize = ary.size();

		Integer removeVal = ary.remove(15);

		assertEquals(oldSize - 1, ary.size());
		for (Integer i = 0; i < 15; i++) {
			assertEquals(new Integer(i), ary.get(i));
		}
		for (Integer i = 15; i < ary.size(); i++) {
			assertEquals(new Integer(i + 1), ary.get(i));
		}
	}

	@Test
	public void clearTest() {
		int size = ary.size();
		ary.clear();
		assertEquals(size, ary.size());
		assertNull(ary.get(0));
		Integer newVal = 1;
		ary.set(1, newVal);
		assertEquals(newVal, ary.get(1));
	}

	@Test
	public void indexOfTest() {
		assertEquals(2, ary.indexOf(2));
	}

	@Test
	public void lastIndexOfTest() {
		assertEquals(2, ary.lastIndexOf(2));
	}

	@Test
	public void subListTest() {
		List<Integer> subList = ary.subList(9, 11);

		assertEquals(3, subList.size());
		assertEquals(new Integer(9), subList.get(0));
		assertEquals(new Integer(10), subList.get(1));
		assertEquals(new Integer(11), subList.get(2));
	}

	@Test
	public void createEmptyIndexTest() {
		ary.resize(0);
		assertEquals(0, ary.size());
		ary.resize(10);
		assertEquals(10, ary.size());
		for (Integer i = 0; i < ary.size(); i++) {
			assertNull(ary.get(i));
		}
	}

	@Test
	public void getDefaultValueTest() throws Exception {
		ary = initExArrayListDefaultZero();
		assertEquals(size, ary.size());
		for (Integer i = 0; i < ary.size(); i++) {
			assertEquals(new Integer(0), ary.get(i));
		}
	}
}
