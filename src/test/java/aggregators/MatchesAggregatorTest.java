package aggregators;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DefaultMatchesAggregatorTest {

    private final MatchesAggregator aggregator = new DefaultMatchesAggregator();

    @Test
    void mergesSameNameAcrossChunks() {
        // Simulated raw chunk results
        Result r1 = new Result("Alice", Set.of(
                new ResultData(1, 5),
                new ResultData(1, 10)
        ));
        Result r2 = new Result("Alice", Set.of(
                new ResultData(1, 15),
                new ResultData(1, 10) // duplicate charOffset in same line
        ));

        List<List<Result>> raw = List.of(List.of(r1), List.of(r2));
        Set<Result> aggregated = aggregator.aggregate(raw);

        assertEquals(1, aggregated.size());
        Result result = aggregated.iterator().next();
        assertEquals("Alice", result.name());

        Set<ResultData> data = result.matchesData();
        assertEquals(3, data.size());
        assertTrue(data.contains(new ResultData(1,5)));
        assertTrue(data.contains(new ResultData(1,10)));
        assertTrue(data.contains(new ResultData(1,15)));
    }

    @Test
    void keepsDifferentNamesSeparate() {
        Result ra = new Result("Alice", Set.of(new ResultData(1,5)));
        Result rb = new Result("Bob", Set.of(new ResultData(2,6)));

        List<List<Result>> raw = List.of(List.of(ra, rb));
        Set<Result> out = aggregator.aggregate(raw);

        assertEquals(2, out.size());
        Map<String, Result> byName = new HashMap<>();
        for (Result r : out) {
            byName.put(r.name(), r);
        }
        assertTrue(byName.containsKey("Alice"));
        assertTrue(byName.containsKey("Bob"));
    }

    @Test
    void emptyInputReturnsEmptySet() {
        Set<Result> ag = aggregator.aggregate(Collections.emptyList());
        assertTrue(ag.isEmpty());
    }

}
