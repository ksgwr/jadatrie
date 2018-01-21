package jp.ksgwr.array;

import static org.junit.Assert.*;

import java.io.File;

import jp.ksgwr.array.index.InfoFixSeparateSegmentIndex;
import jp.ksgwr.array.index.TextIntegerIndexer;

import org.junit.Test;

public class SplitDiskArrayListTest extends MemoryArrayTest {

	protected final File directory = new File("target", "splitDiskArrayTest");

	protected final int separateSize = 10;

	@Override
	protected ExArrayList<Integer> initExArrayList() throws Exception {
		return new DiskArrayList<Integer>(Integer.class, new InfoFixSeparateSegmentIndex<>(directory, "", separateSize, new TextIntegerIndexer()), super.size);
	}

	@Test
	public void loadNewSegmentTest() throws Exception {
		super.size = 35;
		ary = initExArrayList();
		for(int i=0;i<size;i++) {
			ary.set(i, i);
		}
		// cache 20~29(update:true), second cache 10~19(update:true)
		// execute save second cache and swap cache
		assertEquals(new Integer(0), ary.get(0));
		// cache 0~9(update:false), second cache 20~29(update:true)
		assertEquals(new Integer(10), ary.get(10));
		// cache 10~19(update:false), second cache 20~29(update:true)
		assertEquals(new Integer(0), ary.get(0));
		assertEquals(new Integer(10), ary.get(10));
		assertEquals(new Integer(20), ary.get(20));
		assertEquals(new Integer(30), ary.get(30));
	}
}
