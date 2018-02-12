package jp.ksgwr.array;

import java.io.File;

import jp.ksgwr.array.index.InfoFixSeparateSegmentIndex;
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
		WritableExArrayList<Integer> iary = (WritableExArrayList<Integer>) ary;

	}
}
