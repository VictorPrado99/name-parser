package aggregators;

import java.util.*;
import java.util.stream.Collectors;

public interface MatchesAggregator {

    // Default method to aggregate results from multiple chunks into a single Set<Result>
    default Set<Result> aggregate(List<List<Result>> rawResults) {
        Map<String, Set<ResultData>> aggregation = new HashMap<>(); // Map to hold aggregated results keyed by matched word

        // Iterate over all chunks of results
        for (List<Result> chunkResults : rawResults) {
            // Iterate over each individual result in the chunk
            for (Result result : chunkResults) {
                // Merge ResultData of the current result into the aggregation map
                aggregation.merge(
                        result.name(),                                // Key: matched word
                        new LinkedHashSet<>(result.matchesData()),    // Value: Set of ResultData (line/position info)
                        (oldSet, newSet) -> {                         // Merge function in case key already exists
                            oldSet.addAll(newSet);                    // Add all new ResultData to existing set
                            return oldSet;                            // Return merged set
                        }
                );
            }
        }

        // Transform the aggregated map entries back into Result objects
        return aggregation.entrySet().stream()
                .map(row -> new Result(row.getKey(), row.getValue())) // Create Result for each unique word with all its ResultData
                .collect(Collectors.toSet());                         // Collect all results into a Set (to ensure uniqueness)
    }

}
