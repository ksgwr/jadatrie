package jp.ksgwr.array;

import java.io.File;

import jp.ksgwr.array.index.InfoFixSeparateSegmentIndex;
import jp.ksgwr.array.index.TextIntegerIndexer;

public class SplitDiskArrayListTest extends MemoryArrayTest {

	protected File directory = new File("target", "splitDiskArrayTest");

	protected int separateSize = 10;

	@Override
	protected ExArrayList<Integer> initExArrayList() throws Exception {
		return new DiskArrayList<Integer>(Integer.class, new InfoFixSeparateSegmentIndex<>(directory, "", separateSize, new TextIntegerIndexer()), super.size);
	}

}
