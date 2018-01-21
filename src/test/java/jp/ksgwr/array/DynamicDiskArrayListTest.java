package jp.ksgwr.array;

import jp.ksgwr.array.index.InfoDynamicSegmentIndex;
import jp.ksgwr.array.index.InfoFixSeparateSegmentIndex;
import jp.ksgwr.array.index.TextIntegerIndexer;
import org.junit.BeforeClass;

import java.io.File;

public class DynamicDiskArrayListTest extends MemoryArrayTest {

    protected static final File directory = new File("target", "dynamicDiskArrayTest");

    @BeforeClass
    public static void initOnce() {
        directory.mkdir();
    }

    @Override
    protected ExArrayList<Integer> initExArrayList() throws Exception {
        return new DiskArrayList<>(Integer.class, new InfoDynamicSegmentIndex<>(directory, "", new TextIntegerIndexer()), super.size);
    }
}
