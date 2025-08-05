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

@AllArgsConstructor // Generates constructor with all fields as parameters (dictionaryUrl, fileToParseUrl)
public class NameSearchManagerImpl implements NameSearchManager {

    private final URI dictionaryUrl;    // URL to the dictionary file (list of names to search for)
    private final URI fileToParseUrl;   // URL to the file where we will search names

    private final MatchesAggregator matchesAggregator = new DefaultMatchesAggregator(); // Aggregates results (e.g., de-duplicates)

    @Override
    public void execute() {
        List<String> names;
        try {
            // Read all lines from dictionary file into a list of strings
            names = Files.readAllLines(Path.of(dictionaryUrl));
        } catch (IOException e) {
            throw new RuntimeException(e); // Wrap checked exception as unchecked
        }

        // Build the Aho-Corasick matcher with all names
        AhoCorasickMatcher ahoCorasickMatcher = new AhoCorasickMatcher(names);

        List<Future<List<Result>>> futures = new ArrayList<>(); // Store futures for async chunk processing

        int chunkLines = 1000; // Process file in chunks of 1000 lines
        try (BufferedReader bufferedReader = Files.newBufferedReader(Path.of(fileToParseUrl));
             // Using Java 21 Virtual Threads to handle chunk tasks concurrently
             ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()
        ) {
            List<LineReference> chunk = new ArrayList<>(chunkLines); // Holds current chunk of lines
            String line;
            int lineOffset = 0;        // Tracks current line number
            int globalCharOffset = 0;  // Tracks character offset from start of file

            // Read file line by line
            while ((line = bufferedReader.readLine()) != null) {
                // Wrap line in a LineReference object with its position info
                chunk.add(new LineReference(++lineOffset, line, globalCharOffset));
                globalCharOffset += line.length() + 1; // Account for newline char (+1)

                // If chunk is full, process it asynchronously
                if (chunk.size() >= chunkLines) {
                    List<LineReference> toProcess = List.copyOf(chunk); // Immutable snapshot of chunk
                    futures.add(executorService.submit(() -> processChunk(toProcess, ahoCorasickMatcher))); // Submit task
                    chunk.clear(); // Clear chunk to collect next lines
                }
            }

            // Process remaining lines if any left (last partial chunk)
            if (!chunk.isEmpty()) {
                futures.add(executorService.submit(() -> processChunk(chunk, ahoCorasickMatcher)));
            }

            executorService.shutdown(); // Signal that no more tasks will be submitted

            // Wait for all tasks to finish or timeout after 5 minutes
            if (!executorService.awaitTermination(5, TimeUnit.MINUTES)) {
                throw new RuntimeException("Executor timed out before completing tasks");
            }

            // Retrieve results from futures (non-blocking since tasks are done)
            List<List<Result>> rawResults = futures.stream()
                    .map(Future::resultNow)
                    .toList();

            // Aggregate (e.g., de-duplicate) all partial results into a final result set
            Set<Result> aggregated = matchesAggregator.aggregate(rawResults);

            // Output aggregated results
            aggregated.forEach(System.out::println);

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    // Processes a chunk of lines and returns all matches found in that chunk
    private List<Result> processChunk(List<LineReference> lines, AhoCorasickMatcher aho) {
        return lines.stream()
                // For each line, perform Aho-Corasick search and get list of matches
                .map(lineReference -> aho.search(lineReference.text(), lineReference.lineOffset(), lineReference.globalOffset()))
                .flatMap(Collection::stream) // Flatten nested lists of matches
                .collect(Collectors.groupingBy(
                        Match::word, // Group matches by the matched word
                        Collectors.mapping(
                                match -> new ResultData(match.lineOffset(), match.startIndex()), // Extract match position data
                                Collectors.toCollection(LinkedHashSet::new)) // Use LinkedHashSet to avoid duplicates and preserve order
                ))
                .entrySet().stream() // Convert grouped map entries into Result objects
                .map(row -> new Result(row.getKey(), row.getValue()))
                .toList(); // Collect results into a list
    }

}
