package jp.ksgwr.array;

import java.io.File;

public class DiskArrayListTest extends MemoryArrayTest {

	protected File directory = new File("target", "diskArrayTest");

	@Override
	protected ExArrayList<Integer> initExArrayList() throws Exception {
		return new DiskArrayList<Integer>(Integer.class, new InfoSegmentIndex<>(directory, "", new TextIntegerIndexer()), super.size);
	}

}
