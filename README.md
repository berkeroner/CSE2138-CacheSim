# CSE2138_pro3

# Java Cache Simulator

This project is a Java-based simulation of a multi-level memory system with L1 instruction cache (L1I), L1 data cache (L1D), and a unified L2 cache. It processes memory access trace files and simulates cache hits, misses, and memory accesses.

## Project Structure

- `src/`: Java source files (`Cache.java`, `TraceParser.java`, etc.)
- `data/`: Input files such as `RAM.dat` and `test1.trace`
- `output/`: Output files generated after simulation (`L1D.txt`, `L2.txt`, etc.)
- `.gitignore`: Specifies files/folders Git should ignore
- `README.md`: Project description and usage instructions

## Features

- Instruction fetch (`I`), load (`L`), store (`S`), and modify (`M`) trace support
- Separate L1 instruction and data caches, shared L2
- FIFO replacement policy (assumed unless specified)
- Tracks hits and misses for each level
- Logs output per level into separate files (optional)

## How to Compile and Run

First, compile all `.java` files inside the `src/` directory:

```bash
javac src/*.java
```

Then, run the simulator with command-line arguments:

```bash
java -cp src CacheSimulator -L1s 0 -L1E 2 -L1b 3 -L2s 1 -L2E 2 -L2b 3 -t data/*.trace
```

### Command-line Arguments

| Argument | Description                  |
|----------|------------------------------|
| `-L1s`   | Number of set index bits for L1 cache         |
| `-L1E`   | Associativity (lines per set) for L1           |
| `-L1b`   | Block offset bits for L1                       |
| `-L2s`   | Set index bits for L2                          |
| `-L2E`   | Associativity for L2                           |
| `-L2b`   | Block offset bits for L2                       |
| `-t`     | Path to trace file (e.g., `data/test1.trace`)  |

## Example Input

Place your `RAM.dat` and `.trace` files under the `data/` folder. A sample file like `test.trace` may contain lines such as:

```
L 0x1234,4
S 0xABCD,2
M 0x8888,8
I 0x5678,4
```

These will be parsed and trigger the corresponding cache behavior.

## Output

The simulator may write outputs to the `output/` folder, e.g.:

- `L1D.txt`: L1 Data cache behavior
- `L1I.txt`: L1 Instruction cache behavior
- `L2.txt`: L2 cache behavior

Make sure `output/` exists before running the simulation, or the program should create it automatically.

## Version Control (.gitignore)

A `.gitignore` is used to exclude:

```
*.class
*.txt
output/
*.log
*.swp
.vscode/
.idea/
```
