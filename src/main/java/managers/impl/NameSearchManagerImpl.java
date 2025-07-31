package managers.impl;

import aggregators.DefaultMatchesAggregator;
import aggregators.MatchesAggregator;
import aggregators.Result;
import aggregators.ResultData;
import lombok.AllArgsConstructor;
import managers.LineReference;
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

    private final MatchesAggregator matchesAggregator = new DefaultMatchesAggregator();

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
            List<LineReference> chunk = new ArrayList<>(chunkLines);
            String line;
            int lineOffset = 0;
            int globalCharOffset = 0;

            while ((line = bufferedReader.readLine()) != null) {
                globalCharOffset += line.length() + 1; // +1 for the newline character
                lineOffset++;

                chunk.add(new LineReference(lineOffset, line, globalCharOffset));
                if (chunk.size() >= chunkLines) {
                    List<LineReference> toProcess = List.copyOf(chunk);
                    futures.add(executorService.submit(() -> processChunk(toProcess, ahoCorasickMatcher)));
                    chunk.clear();
                }
            }
            if (!chunk.isEmpty()) {
                futures.add(executorService.submit(() -> processChunk(chunk, ahoCorasickMatcher)));
            }

            executorService.shutdown();
            if (!executorService.awaitTermination(5, TimeUnit.MINUTES)) {
                throw new RuntimeException("Executor timed out before completing tasks");
            }

            List<List<Result>> rawResults = futures.stream()
                    .map(Future::resultNow)
                    .toList();


            Set<Result> aggregated = matchesAggregator.aggregate(rawResults);

            // Print
            aggregated.forEach(System.out::println);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    private List<Result> processChunk(List<LineReference> lines, AhoCorasickMatcher aho) {
        return lines.stream()
                .map(lineReference -> aho.search(lineReference.text(), lineReference.lineOffset(), lineReference.globalOffset()))
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(
                        Match::word,
                        Collectors.mapping(match -> new ResultData(match.startIndex(), match.lineOffset()),
                                Collectors.toCollection(LinkedHashSet::new))
                ))
                .entrySet().stream()
                .map(row -> new Result(row.getKey(), row.getValue()))
                .toList();
    }

}
