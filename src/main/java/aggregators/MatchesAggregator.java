package aggregators;

import java.util.*;
import java.util.stream.Collectors;

public interface MatchesAggregator {

    default Set<Result> aggregate(List<List<Result>> rawResults) {
        Map<String, Set<ResultData>> aggregation = new HashMap<>();

        for (List<Result> chunkResults : rawResults) {
            for (Result result : chunkResults) {
                aggregation.merge(
                        result.name(),
                        new LinkedHashSet<>(result.matchesData()),
                        (oldSet, newSet) -> {
                            oldSet.addAll(newSet);
                            return oldSet;
                        }
                );
            }
        }

        return aggregation.entrySet().stream()
                .map(row -> new Result(row.getKey(), row.getValue()))
                .collect(Collectors.toSet());
    }

}
