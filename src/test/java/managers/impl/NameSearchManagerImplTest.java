package managers.impl;

import managers.NameSearchManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class NameSearchManagerImplTest {

    private Path dictionaryFile;
    private Path inputFile;
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private PrintStream originalOut;

    @BeforeEach
    void setUp() throws IOException {
        dictionaryFile = Files.createTempFile("dictionary", ".txt");
        inputFile = Files.createTempFile("input", ".txt");
        originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
    }

    @AfterEach
    void tearDown() throws IOException {
        System.setOut(originalOut);
        Files.deleteIfExists(dictionaryFile);
        Files.deleteIfExists(inputFile);
    }

    @Test
    void testExecute_withMultipleMatches() throws IOException {
        // Prepare test dictionary and input
        Files.writeString(dictionaryFile, "Alice\nBob\nTimothy\n");
        Files.writeString(inputFile, String.join("\n",
                "Timothy is here.",
                "Alice went to the store.",
                "Bob and Timothy are friends.",
                "Nobody knows Xyz."
        ));

        NameSearchManager manager = new NameSearchManagerImpl(
                dictionaryFile.toUri(),
                inputFile.toUri()
        );

        // Execute
        manager.execute();
        String output = outputStream.toString();

        // Validate expected names are found
        assertTrue(output.contains("Timothy"), "Should contain 'Timothy'");
        assertTrue(output.contains("Alice"), "Should contain 'Alice'");
        assertTrue(output.contains("Bob"), "Should contain 'Bob'");

        // Validate line references
        assertTrue(output.matches("(?s).*lineOffset=1.*"), "Should contain lineOffset=1");
        assertTrue(output.matches("(?s).*lineOffset=2.*"), "Should contain lineOffset=2");
        assertTrue(output.matches("(?s).*lineOffset=3.*"), "Should contain lineOffset=3");

        // Ensure unexpected terms are not present
        assertFalse(output.contains("Xyz"), "Should not match 'Xyz'");
    }

    @Test
    void testExecute_withNoMatches() throws IOException {
        Files.writeString(dictionaryFile, "Zelda\nLink\n");
        Files.writeString(inputFile, String.join("\n",
                "This line has no matching names.",
                "Nor does this one."
        ));

        NameSearchManager manager = new NameSearchManagerImpl(
                dictionaryFile.toUri(),
                inputFile.toUri()
        );

        manager.execute();
        String output = outputStream.toString();

        assertTrue(output.trim().isEmpty(), "Output should be empty for no matches");
    }

    @Test
    void testExecute_withEmptyDictionary() throws IOException {
        Files.writeString(dictionaryFile, "");
        Files.writeString(inputFile, "Just some normal text.");

        NameSearchManager manager = new NameSearchManagerImpl(
                dictionaryFile.toUri(),
                inputFile.toUri()
        );

        manager.execute();
        String output = outputStream.toString();

        assertTrue(output.trim().isEmpty(), "Output should be empty with no dictionary entries");
    }
}
