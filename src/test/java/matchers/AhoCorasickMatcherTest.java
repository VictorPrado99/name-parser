package matchers;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AhoCorasickMatcherTest {

    @Test
    void testSingleMatchCorrectOffsets() {
        AhoCorasickMatcher matcher = new AhoCorasickMatcher(Set.of("Alice"));
        String line = "Alice is present.";
        int globalOffset = 100;
        int lineOffset = 5;

        List<Match> matches = matcher.search(line, lineOffset, globalOffset);

        assertEquals(1, matches.size());
        Match match = matches.get(0);
        assertEquals("Alice", match.word());
        assertEquals(100, match.startIndex()); // 0 + 100
        assertEquals(105, match.endIndex());   // 5 + 100
        assertEquals(5, match.lineOffset());
    }

    @Test
    void testMultipleMatchesDifferentWords() {
        AhoCorasickMatcher matcher = new AhoCorasickMatcher(Set.of("Bob", "Alice"));
        String line = "Bob and Alice are here";
        int globalOffset = 50;
        int lineOffset = 2;

        List<Match> matches = matcher.search(line, lineOffset, globalOffset);

        assertEquals(2, matches.size());

        Match bob = matches.stream().filter(m -> m.word().equals("Bob")).findFirst().orElseThrow();
        Match alice = matches.stream().filter(m -> m.word().equals("Alice")).findFirst().orElseThrow();

        assertEquals(50, bob.startIndex());
        assertEquals(53, bob.endIndex());

        assertEquals(58, alice.startIndex());
        assertEquals(63, alice.endIndex());

        assertEquals(2, bob.lineOffset());
        assertEquals(2, alice.lineOffset());
    }

    @Test
    void testWordInMiddleNotWholeWord() {
        AhoCorasickMatcher matcher = new AhoCorasickMatcher(Set.of("Bob"));
        String line = "foobar";
        int globalOffset = 0;
        int lineOffset = 1;

        List<Match> matches = matcher.search(line, lineOffset, globalOffset);
        assertTrue(matches.isEmpty(), "Should not match part of another word");
    }

    @Test
    void testMatchAtEndOfLine() {
        AhoCorasickMatcher matcher = new AhoCorasickMatcher(Set.of("here"));
        String line = "We are here";
        int globalOffset = 20;
        int lineOffset = 7;

        List<Match> matches = matcher.search(line, lineOffset, globalOffset);
        assertEquals(1, matches.size());

        Match m = matches.get(0);
        assertEquals("here", m.word());
        assertEquals(27, m.startIndex()); // "here" starts at 10 + 20
        assertEquals(31, m.endIndex());
        assertEquals(7, m.lineOffset());
    }

    @Test
    void testMultipleMatchesSameWord() {
        AhoCorasickMatcher matcher = new AhoCorasickMatcher(Set.of("Bob"));
        String line = "Bob, Bob, and Bob.";
        int globalOffset = 100;
        int lineOffset = 4;

        List<Match> matches = matcher.search(line, lineOffset, globalOffset);
        assertEquals(3, matches.size());

        for (Match match : matches) {
            assertEquals("Bob", match.word());
            assertEquals(4, match.lineOffset());
        }

        int[] expectedStarts = {100, 105, 114};
        int[] expectedEnds = {103, 108, 117};

        for (int i = 0; i < 3; i++) {
            assertEquals(expectedStarts[i], matches.get(i).startIndex());
            assertEquals(expectedEnds[i], matches.get(i).endIndex());
        }
    }
}
