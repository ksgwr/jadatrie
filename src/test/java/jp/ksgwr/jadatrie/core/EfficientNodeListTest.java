package jp.ksgwr.jadatrie.core;

import static org.junit.Assert.*;

import java.util.Iterator;

import org.junit.Test;

public class EfficientNodeListTest {

	@Test
	public void simpleTest() {
		EfficientNodeList list = new EfficientNodeList();

		list.clear(10);

		Node node = new Node();
		node.code = 1;
		list.add(node);
		node.code = 2;
		list.add(node);

		Iterator<Node> ite = list.iterator();
		assertEquals(1, ite.next().code);
		assertEquals(2, ite.next().code);
		assertEquals(false, ite.hasNext());

		list.clear();

		ite = list.iterator();
		assertEquals(false, ite.hasNext());

	}
}
