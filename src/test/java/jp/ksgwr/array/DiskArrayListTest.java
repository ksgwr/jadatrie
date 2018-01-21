package jp.ksgwr.array;

import java.io.File;

import jp.ksgwr.array.index.InfoSegmentIndex;
import jp.ksgwr.array.index.TextIntegerIndexer;
import org.junit.BeforeClass;

public class DiskArrayListTest extends MemoryArrayTest {

	protected static File directory = new File("target", "diskArrayTest");

	@BeforeClass
	public static void initOnce() {
		directory.mkdir();
	}

	@Override
	protected ExArrayList<Integer> initExArrayList() throws Exception {
		return new DiskArrayList<>(Integer.class, new InfoSegmentIndex<>(directory, "", new TextIntegerIndexer()), super.size);
	}

	@Override
	protected ExArrayList<Integer> initExArrayListDefaultZero() throws Exception {
		return new DiskArrayList<>(Integer.class, 0, new InfoSegmentIndex<>(directory, "", new TextIntegerIndexer()), super.size);
	}
}
