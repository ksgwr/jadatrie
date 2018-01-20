package jp.ksgwr.array;

import java.io.File;

import jp.ksgwr.array.index.InfoSegmentIndex;
import jp.ksgwr.array.index.TextIntegerIndexer;

public class DiskArrayListTest extends MemoryArrayTest {

	protected File directory = new File("target", "diskArrayTest");

	@Override
	protected ExArrayList<Integer> initExArrayList() throws Exception {
		return new DiskArrayList<Integer>(Integer.class, new InfoSegmentIndex<>(directory, "", new TextIntegerIndexer()), super.size);
	}

	@Override
	protected ExArrayList<Integer> initExArrayListDefaultZero() throws Exception {
		return new DiskArrayList<Integer>(Integer.class, new Integer(0), new InfoSegmentIndex<>(directory, "", new TextIntegerIndexer()), super.size);
	}
}
