package aggregators;

import java.util.Set;

public record Result(
        String name,
        Set<ResultData> matchesData
) {

    @Override
    public String toString(){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(name).append(" --> [");
        boolean first = true;
        for (ResultData resultData : matchesData) {
            if (!first) stringBuilder.append(", ");
            stringBuilder.append(resultData);
            first = false;
        }
        stringBuilder.append("]");
        return stringBuilder.toString();
    }
}
