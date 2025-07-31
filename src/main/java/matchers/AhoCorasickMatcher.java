package matchers;

import java.util.*;

public class AhoCorasickMatcher {

    private final Node root = new Node();

    public AhoCorasickMatcher(Collection<String> words) {
        buildTrie(words);
        buildFailureLinks();
    }

    private void buildTrie(Collection<String> words) {
        for (String pat : words) {
            String key = pat.toLowerCase();
            Node node = root;
            for (char character : key.toCharArray()) {
                node = node.getNext().computeIfAbsent(character, k -> new Node());
            }
            node.getOutputs().add(key);
        }
    }

    private void buildFailureLinks() {
        Queue<Node> deque = new ArrayDeque<>();
        for (Node child : root.getNext().values()) {
            child.setFail(root);
            deque.add(child);
        }
        while (!deque.isEmpty()) {
            Node curr = deque.poll();
            for (Map.Entry<Character,Node> e : curr.getNext().entrySet()) {
                char c = e.getKey();
                Node child = e.getValue();
                Node possibleFailedNode = curr.getFail();
                while (possibleFailedNode != null && !possibleFailedNode.getNext().containsKey(c)) {
                    possibleFailedNode = possibleFailedNode.getFail();
                }
                child.setFail((possibleFailedNode != null) ? possibleFailedNode.getNext().get(c) : root);
                child.getOutputs().addAll(child.getFail().getOutputs());
                deque.add(child);
            }
        }
    }

    public List<Match> search(String text) {
        List<Match> matches = new ArrayList<>();
        Node node = root;
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            while (node != root && !node.getNext().containsKey(character)) {
                node = node.getFail();
            }
            node = node.getNext().getOrDefault(character, root);
            if (!node.getOutputs().isEmpty()) {
                for (String pattern : node.getOutputs()) {
                    int start = i - pattern.length() + 1;
                    int end = i + 1;
                    if (isWholeWord(text, start, end)) {
                        matches.add(new Match(pattern, start, end));
                    }
                }
            }
        }
        return matches;
    }

    private boolean isWholeWord(String text, int start, int end) {
        // check char before and after match
        boolean leftOk = (start == 0) || !Character.isLetter(text.charAt(start - 1));
        boolean rightOk = (end >= text.length()) || !Character.isLetter(text.charAt(end));
        return leftOk && rightOk;
    }

}
