package jp.ksgwr.array;

import static org.junit.Assert.*;

import java.io.File;

import jp.ksgwr.array.index.InfoFixSeparateSegmentIndex;
import jp.ksgwr.array.index.SeparableIndex;
import jp.ksgwr.array.index.TextIntegerIndexer;

import org.junit.BeforeClass;
import org.junit.Test;

public class SplitDiskArrayListTest extends MemoryArrayTest {

	protected static final File directory = new File("target", "splitDiskArrayTest");

	protected static final int separateSize = 10;

	@BeforeClass
	public static void initOnce() {
		directory.mkdir();
	}

	@Override
	protected ExArrayList<Integer> initExArrayList() throws Exception {
		return new DiskArrayList<>(Integer.class, new InfoFixSeparateSegmentIndex<>(directory, "", separateSize, new TextIntegerIndexer()), super.size);
	}

	@Test
	public void saveAndLoadTest() throws Exception {
		DiskArrayList<Integer> iary = (DiskArrayList<Integer>) ary;
		SeparableIndex<Integer> outIndex = new InfoFixSeparateSegmentIndex<>(directory, "out", 5, new TextIntegerIndexer());

		iary.save(outIndex);

		ary.clear();
		for (int i = 0; i < ary.size(); i++) {
			assertNull(ary.get(i));
		}

		iary.load(outIndex);
		for (Integer i = 0; i < ary.size(); i++) {
			assertEquals(i, ary.get(i));
		}
	}
}
