# NameMatcher

A high-performance Java application to search for a predefined list of names within a large text file using the Aho-Corasick string matching algorithm and Java 21 Virtual Threads.

## Overview

This project demonstrates an efficient multi-threaded name search pipeline that leverages:

* **Aho-Corasick algorithm** for multi-pattern matching.
* **Java 21 Virtual Threads** to process large files in parallel with minimal memory overhead.
* **Chunked file reading** to avoid loading large files entirely into memory.

The program outputs each found name along with its exact character offset and line offset in the text.

---

## 📂 Project Structure

```
src/
 ├── NameParser.java              # Entry point
 ├── managers/
 │   └── NameSearchManager.java
 │   └── impl/NameSearchManagerImpl.java
 ├── matchers/
 │   ├── AhoCorasickMatcher.java
 │   └── Match.java
 ├── aggregators/
 │   ├── MatchesAggregator.java
 │   ├── DefaultMatchesAggregator.java
 │   ├── Result.java
 │   └── ResultData.java
resources/
 ├── dictionary.txt               # List of names to search (one per line)
 └── big.txt                      # Large text file to search through
```

---

## ✅ How It Works

1. **Build the Trie:**
   The list of names is read from `dictionary.txt` and compiled into a case-insensitive Aho-Corasick Trie.

2. **Chunked File Reading:**
   The large file (`big.txt`) is read in chunks of 1000 lines at a time.

3. **Parallel Matching with Virtual Threads:**
   Each chunk is submitted to a virtual thread that uses the compiled matcher to find all name occurrences.

4. **Result Aggregation:**
   Results from each chunk are collected, deduplicated, and aggregated using `MatchesAggregator`.

5. **Output:**
   Each matched name is printed in the format:

   ```
   ```

Name --> \[\[lineOffset=..., charOffset=...], ...]

```

---

## 🧪 Example Output
```

James --> \[\[lineOffset=1200, charOffset=25480], \[lineOffset=3567, charOffset=75320]]
John --> \[\[lineOffset=134, charOffset=1987], \[lineOffset=856, charOffset=15234]]

````

---

## 🚀 How to Run

1. Place your `dictionary.txt` and `big.txt` in the `resources/` directory.

2. Compile and run using Java 21:
```bash
javac -d out $(find src -name "*.java")
java -cp out:resources NameParser
````

3. Output will be printed to `stdout`.

---

## 💡 Notes on Design

* Uses **Virtual Threads** (Java 21) to process many chunks concurrently with very low memory overhead.
* Designed to **avoid OutOfMemoryErrors** by never loading the entire file into memory.
* Custom **Aho-Corasick** implementation avoids dependency bloat.
* Output is sorted by match and location.

---

## 🛠 Technologies Used

* Java 21
* Aho-Corasick pattern matching
* Virtual Threads (Project Loom)
* Stream API, Futures, ExecutorService

---

## 📘 References

* [Aho-Corasick Algorithm](https://en.wikipedia.org/wiki/Aho%E2%80%93Corasick_algorithm)
* [Virtual Threads in Java 21](https://openjdk.org/jeps/444)


