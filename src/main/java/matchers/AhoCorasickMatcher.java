package matchers;

import java.util.*;

public class AhoCorasickMatcher {

    private final Node root = new Node(); // Root node of the Trie (automaton)

    // Constructor: builds Trie and sets up failure links
    public AhoCorasickMatcher(Collection<String> words) {
        buildTrie(words);         // Build Trie structure with given words
        buildFailureLinks();      // Build failure (fallback) links for pattern matching
    }

    // Build the Trie structure by inserting all the patterns
    private void buildTrie(Collection<String> words) {
        for (String word : words) {                     // Iterate over all words to insert
            Node node = root;                          // Start from root for each word
            for (char character : word.toCharArray()) { // For each character in the word
                // Move to existing child node or create a new one
                node = node.getNext().computeIfAbsent(character, c -> new Node());
            }
            node.getOutputs().add(word);                // Mark end of word by storing pattern in outputs
        }
    }

    // Build failure links using BFS traversal of the Trie
    private void buildFailureLinks() {
        Queue<Node> deque = new ArrayDeque<>();        // Queue for BFS traversal

        // Initialize fail links of root's direct children to root itself
        for (Node child : root.getNext().values()) {
            child.setFail(root);                       // Direct children fallback to root
            deque.add(child);                          // Add them to queue for BFS
        }

        // BFS traversal to assign failure links for the rest of the nodes
        while (!deque.isEmpty()) {
            Node curr = deque.poll();                  // Dequeue current node to process

            // Iterate over all children of current node
            for (Map.Entry<Character,Node> row : curr.getNext().entrySet()) {
                char character = row.getKey();                   // Character of the edge
                Node child = row.getValue();             // Child node reached by 'character'
                Node possibleFailedNode = curr.getFail(); // Start fallback from current node's fail link

                // Traverse fail links until we find a node with a transition for 'character' or hit root
                while (possibleFailedNode != null && !possibleFailedNode.getNext().containsKey(character)) {
                    possibleFailedNode = possibleFailedNode.getFail(); // Follow fail link up the tree
                }

                // Set fail link of child:
                // - If fallback node has 'character' transition, use that node.
                // - Else, fallback to root.
                child.setFail((possibleFailedNode != null) ? possibleFailedNode.getNext().get(character) : root);

                // Merge output patterns from fail link into current child outputs
                child.getOutputs().addAll(child.getFail().getOutputs());

                // Add child to queue to process its children next
                deque.add(child);
            }
        }
    }

    // Search for patterns in the input text, considering line and global character offsets
    public List<Match> search(String text, int lineOffset, int globalCharOffset) {
        List<Match> matches = new ArrayList<>();       // List to collect found matches
        Node node = root;                              // Start from root

        for (int i = 0; i < text.length(); i++) {      // Process each character in text
            char character = text.charAt(i);

            // If no edge for character, follow fail links until we find one or return to root
            while (node != root && !node.getNext().containsKey(character)) {
                node = node.getFail();                 // Fallback through fail links
            }

            // Move to the next node via character transition, or fallback to root if none
            node = node.getNext().getOrDefault(character, root);

            // If current node has output patterns, we've found matches ending here
            if (!node.getOutputs().isEmpty()) {
                for (String pattern : node.getOutputs()) {
                    int start = i - pattern.length() + 1; // Compute start index of match
                    int end = i + 1;                      // End index is current position + 1
                    if (isWholeWord(text, start, end)) {  // Check if match is a whole word
                        matches.add(new Match(
                                pattern,
                                globalCharOffset + start, // Absolute character offset start
                                globalCharOffset + end,   // Absolute character offset end
                                lineOffset                // Line offset (for multi-line contexts)
                        ));
                    }
                }
            }
        }

        return matches; // Return all found matches
    }

    // Helper method to check if a found pattern is a "whole word" match
    private boolean isWholeWord(String text, int start, int end) {
        // Check if character before start is not a letter (or start is at text beginning)
        boolean leftOk = (start == 0) || !Character.isLetter(text.charAt(start - 1));

        // Check if character after end is not a letter (or end is at text end)
        boolean rightOk = (end >= text.length()) || !Character.isLetter(text.charAt(end));

        return leftOk && rightOk; // Return true only if both boundaries are clean
    }
}
