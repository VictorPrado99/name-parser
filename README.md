# NameMatcher

A high-performance Java application that searches for a predefined list of names within a large text file using the **Aho–Corasick** string-matching algorithm. It’s optimized for Java 21+ through **Virtual Threads**, chunked file processing, and efficient result aggregation.

---

## 🚀 Features

- Implements **Aho–Corasick algorithm** for multi-pattern search in linear time :contentReference[oaicite:1]{index=1}
- Processes **large text files chunk-by-chunk**, avoiding OutOfMemory risks
- Uses **Java 21 Virtual Threads** to submit each chunk concurrently with minimal overhead :contentReference[oaicite:2]{index=2}
- Thread-safe **aggregation of results** using clean, record-based data structures (`Result`, `ResultData`)

---

## ⚙️ How It Works

1. **Load dictionary** from `dictionary.txt`, build a case-insensitive trie via Aho–Corasick
2. **Read `big.txt` in chunks** (default 1000 lines)
3. **Submit each chunk** to a *virtual thread* via Java 21’s `newVirtualThreadPerTaskExecutor()`
4. **Each chunk yields matches** (word, global char offset, line number) using the customized matcher
5. **Aggregate all chunk results** with `MatchesAggregator`, merging match data sets per name
6. **Print final results**, e.g.:

Timothy --> [[lineOffset=13388, charOffset=1018975], [lineOffset=13752, charOffset=1041587]]

---

## 🖥️ Sample Output

Timothy --> [[lineOffset=13000, charOffset=19775], ...]
John --> [[lineOffset=14900, charOffset=18433], ...]

---

## 🧪 Running the Project

# run (with resources in classpath)
java -cp out:resources NameParser
Output is printed to stdout.

🔧 Design Highlights
Efficient & scalable—avoids loading full file in RAM

Chunked streaming—keeps memory bounded

Offset tracking—captures accurate lineOffset (line number) and charOffset (global position)

Aggregation—deduplicates and merges matches across chunks

Records usage—clean data modeling using Result and ResultData

🎯 Further Enhancements
Allow chunk size and parallelism tuning via command-line options

Support multiple input files simultaneously

Add context display (e.g. surrounding words) in output

Provide output in CSV or JSON format