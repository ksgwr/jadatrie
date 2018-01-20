package jp.ksgwr.array;


public class CachedMemoryArrayTest extends MemoryArrayTest {

	@Override
	protected ExArrayList<Integer> initExArrayList() throws Exception {
		return new CachedMemoryArrayList<Integer>(Integer.class, super.size);
	}

	@Override
	protected ExArrayList<Integer> initExArrayListDefaultZero() throws Exception {
		return new CachedMemoryArrayList<Integer>(Integer.class, new Integer(0), size);
	}
}
