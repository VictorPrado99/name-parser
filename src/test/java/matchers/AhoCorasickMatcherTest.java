package matchers;

import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AhoCorasickMatcherTest {

    @Test
    void testSingleMatch() {
        AhoCorasickMatcher ac = new AhoCorasickMatcher(List.of("apple"));
        List<Match> matches = ac.search("I ate an apple.");
        assertEquals(1, matches.size());
        Match match = matches.get(0);
        assertEquals("apple", match.word());
        assertEquals(9, match.startIndex());
        assertEquals(14, match.endIndex());
    }

    @Test
    void testMultipleMatches() {
        AhoCorasickMatcher ac = new AhoCorasickMatcher(List.of("cat", "dog"));
        List<Match> matches = ac.search("A cat and a dog.");
        assertEquals(2, matches.size());
        assertTrue(matches.contains(new Match("cat", 2, 5)));
        assertTrue(matches.contains(new Match("dog", 12, 15)));
    }

    @Test
    void testNoMatch() {
        AhoCorasickMatcher ac = new AhoCorasickMatcher(List.of("grape"));
        List<Match> matches = ac.search("There is no fruit here.");
        assertTrue(matches.isEmpty());
    }

    @Test
    void testWholeWordOnly() {
        AhoCorasickMatcher ac = new AhoCorasickMatcher(List.of("he"));
        List<Match> matches = ac.search("the hero ran");
        // Should only match "he" as a word (not inside "the" or "hero")
        assertTrue(matches.isEmpty());
    }

    @Test
    void testOverlappingPatterns() {
        AhoCorasickMatcher ac = new AhoCorasickMatcher(List.of("hers", "her", "he"));
        List<Match> matches = ac.search("hers");
        // Only match "hers" as a whole word
        assertEquals(1, matches.size());
        assertEquals("hers", matches.get(0).word());
    }

    @Test
    void testMultipleOccurrencesOfSameWord() {
        AhoCorasickMatcher ac = new AhoCorasickMatcher(List.of("test"));
        List<Match> matches = ac.search("This is a test. Another test!");
        assertEquals(2, matches.size());
        assertEquals("test", matches.get(0).word());
        assertEquals("test", matches.get(1).word());
    }
}
