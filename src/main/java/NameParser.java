import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

public class NameParser {

    public static void main(String[] args) throws IOException, URISyntaxException {
        URL dictionaryUrl = NameParser.class.getClassLoader().getResource("dictionary.txt");
        URL bigUrl = NameParser.class.getClassLoader().getResource("big.txt");

        assert dictionaryUrl != null;
        assert bigUrl != null;

        List<String> names = Files.readAllLines(Path.of(dictionaryUrl.toURI()));

        Trie trie = Trie.builder()
                .ignoreCase()
                .onlyWholeWords()
                .addKeywords(names)
                .build();

        List<Future<Map<String, Integer>>> futures = new ArrayList<>();

        int chunkLines = 1000;
        try (BufferedReader br = Files.newBufferedReader(Path.of(bigUrl.toURI()));
             //If we weren't using Java21 to be able to leverage Virtual Threads,
             // we would need to use a regular thread pool, and we would need to be a bit more concerned in how we would handle the cores
             ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()
        ) {
            List<String> chunk = new ArrayList<>(chunkLines);
            String line;
            while ((line = br.readLine()) != null) {
                chunk.add(line);
                if (chunk.size() >= chunkLines) {
                    List<String> toProcess = List.copyOf(chunk);
                    futures.add(executorService.submit(() -> processChunk(toProcess, trie)));
                    chunk.clear();
                }
            }
            if (!chunk.isEmpty()) {
                futures.add(executorService.submit(() -> processChunk(chunk, trie)));
            }

            executorService.shutdown();
            executorService.awaitTermination(2, TimeUnit.MINUTES);

            // Aggregate results
            Map<String, Integer> finalCounts = new HashMap<>();
            for (Future<Map<String, Integer>> fut : futures) {
                Map<String, Integer> m = fut.get();
                for (String name : m.keySet()) {
                    finalCounts.merge(name, m.get(name), Integer::sum);
                }
            }

            // Print
            finalCounts.forEach((name, count) ->
                    System.out.println(name + " -> " + count));

        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    private static Map<String, Integer> processChunk(List<String> lines, Trie trie){
        String text = String.join("\n", lines);
        Collection<Emit> emits = trie.parseText(text);
        Map<String, Integer> counts = new HashMap<>();
        for (Emit emit : emits) {
            String keyword = emit.getKeyword();
            Integer count = counts.getOrDefault(keyword, 0) + 1;
            counts.put(keyword, count);
        }
        return counts;
    }

}
