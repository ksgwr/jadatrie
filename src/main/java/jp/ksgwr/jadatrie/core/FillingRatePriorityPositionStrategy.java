package jp.ksgwr.jadatrie.core;

import java.util.List;

public class FillingRatePriorityPositionStrategy implements PositionStrategy {

    private float targetFillingRate;

    private int notNullPostion;

    public FillingRatePriorityPositionStrategy() {
        this(0.9f);
    }

    public FillingRatePriorityPositionStrategy(float targetFillingRate) {
        this.targetFillingRate = targetFillingRate;
    }

    @Override
    public int startPosition(int currentPos, int maxCode, List<Unit> units, List<String> keys, List<Node> siblings) {
        int preNotNullPosition = notNullPostion;
        while(units.get(notNullPostion) != null) {
            notNullPostion++;
        }
        if (preNotNullPosition == notNullPostion) {
            int cnt = 0;
            int maxPosition = currentPos + maxCode;
            if (maxPosition < units.size()) {
                for (int i = notNullPostion + 1; i <= maxPosition; i++) {
                    if (units.get(i) != null) {
                        cnt++;
                    }
                }
                if (targetFillingRate < (double) cnt / maxCode) {
                    notNullPostion++;
                    while (units.get(notNullPostion) != null) {
                        notNullPostion++;
                    }
                }
            }
        }
        return notNullPostion;
    }
}
