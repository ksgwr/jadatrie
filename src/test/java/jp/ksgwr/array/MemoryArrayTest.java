package jp.ksgwr.array;

import static org.junit.Assert.*;

import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

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
		for (int i = 0; i < ary.size(); i++) {
			assertEquals(i, (int) ary.get(i));
		}
	}

	@Test
	public void iteratorTest() {
		Iterator<Integer> ite = ary.iterator();
		int i=0;
		while (ite.hasNext()) {
			int actual = ite.next();
			assertEquals(i++, actual);
		}
	}

}
