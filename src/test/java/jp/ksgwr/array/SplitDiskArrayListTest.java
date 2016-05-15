package jp.ksgwr.array;

import java.io.File;

public class SplitDiskArrayListTest extends MemoryArrayTest {

	protected File directory = new File("target", "splitDiskArrayTest");

	protected int splitSize = 10;

	@Override
	protected ExArrayList<Integer> initExArrayList() throws Exception {
		return new DiskArrayList<Integer>(Integer.class, directory, super.size, splitSize);
	}

}
