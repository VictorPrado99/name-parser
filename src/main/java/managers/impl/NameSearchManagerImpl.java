package managers.impl;

import aggregators.DefaultMatchesAggregator;
import aggregators.Result;
import aggregators.ResultData;
import lombok.AllArgsConstructor;
import managers.NameSearchManager;
import matchers.AhoCorasickMatcher;
import matchers.Match;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@AllArgsConstructor
public class NameSearchManagerImpl implements NameSearchManager {

    private final URI dictionaryUrl;
    private final URI fileToParseUrl;

    @Override
    public void execute() {
        List<String> names;
        try {
            names = Files.readAllLines(Path.of(dictionaryUrl));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        AhoCorasickMatcher ahoCorasickMatcher = new AhoCorasickMatcher(names);

        List<Future<List<Result>>> futures = new ArrayList<>();

        int chunkLines = 1000;
        try (BufferedReader bufferedReader = Files.newBufferedReader(Path.of(fileToParseUrl));
             //If we weren't using Java21 to be able to leverage Virtual Threads,
             // we would need to use a regular thread pool, and we would need to be a bit more concerned in how we would handle the cores
             ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()
        ) {
            List<String> chunk = new ArrayList<>(chunkLines);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                chunk.add(line);
                if (chunk.size() >= chunkLines) {
                    List<String> toProcess = List.copyOf(chunk);
                    futures.add(executorService.submit(() -> processChunk(toProcess, ahoCorasickMatcher)));
                    chunk.clear();
                }
            }
            if (!chunk.isEmpty()) {
                futures.add(executorService.submit(() -> processChunk(chunk, ahoCorasickMatcher)));
            }

            executorService.shutdown();
            executorService.awaitTermination(1, TimeUnit.MINUTES);

            List<List<Result>> rawResults = new ArrayList<>(futures.size());

            for (Future<List<Result>> future : futures) {
                rawResults.add(future.get());
            }

            Set<Result> aggregated = new DefaultMatchesAggregator().aggregate(rawResults);

            // Print
            aggregated.forEach(System.out::println);

        } catch (ExecutionException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    private List<Result> processChunk(List<String> lines, AhoCorasickMatcher aho) {
        String text = String.join("\n", lines);
        // Run the AC search that returns Match records including word and startIndex
        List<Match> matches = aho.search(text);

        // Group matches by keyword, mapping each to ResultData and collecting into sets
        Map<String, Set<ResultData>> grouped = matches.stream()
                .collect(Collectors.groupingBy(
                        Match::word,
                        Collectors.mapping(m -> new ResultData(m.startIndex(), m.startIndex()),  // use correct offset fields
                                Collectors.toCollection(LinkedHashSet::new))
                ));

        // Build and return a Result for each name
        return grouped.entrySet().stream()
                .map(e -> new Result(e.getKey(), e.getValue()))
                .toList();
    }

}
