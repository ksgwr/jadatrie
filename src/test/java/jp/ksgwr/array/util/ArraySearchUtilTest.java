package jp.ksgwr.array.util;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArraySearchUtilTest {

    @Test
    public void infimumBinarySearchTest() {
        List<Integer> ary = new ArrayList<>(Arrays.asList(new Integer[] {
                1,
                10,
                20,
                30
        }));

        int actual;

        actual = ArraySearchUtil.infimumBinarySearch(ary, 0);
        assertEquals(0, actual);

        actual = ArraySearchUtil.infimumBinarySearch(ary, 1);
        assertEquals(0, actual);

        actual = ArraySearchUtil.infimumBinarySearch(ary, 5);
        assertEquals(0, actual);

        actual = ArraySearchUtil.infimumBinarySearch(ary, 25);
        assertEquals(2, actual);

        actual = ArraySearchUtil.infimumBinarySearch(ary, 30);
        assertEquals(3, actual);

        actual = ArraySearchUtil.infimumBinarySearch(ary, 35);
        assertEquals(3, actual);
    }
}
