package jp.ksgwr.array.util;

import java.util.List;

public class ArraySearchUtil {

    public static <T> int infimumBinarySearch(List<? extends Comparable<? super T>> list, T key) {
        int low = 0;
        int high = list.size() - 1;
        int preMid = -1;
        int preCmp = 0;

        while (low <= high) {
            if (low + 1 == high) {
                Comparable<? super T> highVal = list.get(high);
                return highVal.compareTo(key) > 0 ? low : high;
            }
            int mid = (low + high) >>> 1;
            Comparable<? super T> midVal = list.get(mid);
            // cmp < 0 == mid < key
            int cmp = midVal.compareTo(key);
            if (cmp == 0) {
                return mid;
            }
            if (cmp < 0) {
                low = mid;
                if (low == preMid) {
                    return mid;
                }
            } else {
                high = mid - 1;
                if (high == preMid && preCmp < 0) {
                    return mid - 1;
                }
            }
            preMid = mid;
            preCmp = cmp;
        }
        return preMid;
    }
}
