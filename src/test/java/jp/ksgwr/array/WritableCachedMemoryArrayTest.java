package jp.ksgwr.array;

import jp.ksgwr.array.index.InfoDynamicSegmentIndex;
import jp.ksgwr.array.index.InfoFixSeparateSegmentIndex;
import jp.ksgwr.array.index.SeparableIndex;
import jp.ksgwr.array.index.TextIntegerIndexer;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class WritableCachedMemoryArrayTest extends MemoryArrayTest {

    protected static final File directory = new File("target", "writableCachedMemoryArrayTest");

    @BeforeClass
    public static void initOnce() {
        directory.mkdir();
    }

    @Override
    protected ExArrayList<Integer> initExArrayList() throws Exception {
        return new WritableCachedMemoryArrayList<Integer>(Integer.class, super.size);
    }

    @Override
    protected ExArrayList<Integer> initExArrayListDefaultZero() throws Exception {
        return new WritableCachedMemoryArrayList<Integer>(Integer.class, new Integer(0), size);
    }

    @Test
    public void saveAndLoadTest() throws Exception {
        WritableCachedMemoryArrayList<Integer> iary = (WritableCachedMemoryArrayList<Integer>) ary;
        SeparableIndex<Integer> outIndex = new InfoDynamicSegmentIndex<>(directory, "out", new TextIntegerIndexer());

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
