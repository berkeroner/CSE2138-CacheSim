
// CSE2138 Systems Programming - Project 3
// Cache Simulator

/* Authors:
    Berker ÖNER 150122018
    Miray PİYADE 150122026
    Dilek İrem ÇILDIR 150123075
*/

import java.io.*;
import java.util.*;


// Contains data to be held in each row
class CacheLine {
    long tag;
    long time;
    boolean valid;
    String data;

    public CacheLine(long tag, long time, boolean valid, String data) {
        this.tag = tag;
        this.time = time+1;
        this.valid = valid;
        this.data = data;
    }
}

// Represents the L1 or L2 cache structure
class Cache {
    private final int s, E, b;
    private final int S;
    private final String name;
    private long timeCounter = 0;

    private Map<Integer, CacheSet> sets;

    public int hits = 0;
    public int misses = 0;
    public int evictions = 0;

    public Cache(int s, int E, int b, String name) {
        this.s = s;
        this.E = E;
        this.b = b;
        this.name = name;
        this.S = 1 << s;
        this.sets = new HashMap<>();
        for (int i = 0; i < S; i++) {
            sets.put(i, new CacheSet(E));
        }
    }

    // Cache access for read or write operation
    public void access(long address, int size, boolean isStore, boolean isModify, Cache l2, Memory ram) {
        long tag = address >> (s + b);
        int setIndex = (int)((address >> b) & ((1 << s) - 1));
        CacheSet set = sets.get(setIndex);

        CacheLine line = set.findLine(tag);
        if (line != null) {
            hits++;

            if (isStore) {
                String newData = ram.extractWrite(line.data, address, size);
                line.data = newData;

                if (name.equals("L1D") && l2 != null) {
                    l2.updateData(address, size, ram);
                }

                ram.write(address, size);
            }
            return;
        }

        misses++;

        if (isStore && name.startsWith("L1") && !isModify) {
            System.out.println("  (write-through, no-allocate)");
            ram.write(address, size);
            return;
        }

        if (name.startsWith("L1")) {
            l2.access(address, size, false, false, null, ram);
        }

        // Fetch data from memory
        String data = ram.read(address, size);
        CacheLine newline = new CacheLine(tag, timeCounter++, true, data);

        if (set.getLines().size() >= E) evictions++;
        set.insertLine(newline);

        if (isStore) {
            newline.data = ram.extractWrite(data, address, size);
            if (name.equals("L1D") && l2 != null) {
                l2.access(address, size, true, false, null, ram);
            }
            ram.write(address, size);
        }
    }

    // Update cache content with data from L2
    public void updateData(long address, int size, Memory ram) {
        long tag = address >> (s + b);
        int setIndex = (int)((address >> b) & ((1 << s) - 1));
        CacheSet set = sets.get(setIndex);
        CacheLine line = set.findLine(tag);
        if (line != null) {
            line.data = ram.extractWrite(line.data, address, size);
        }
    }

    public Map<Integer, CacheSet> getAllSets() {
        return sets;
    }
}

// Contains cache lines in each set
class CacheSet {
    LinkedList<CacheLine> lines;
    int E;

    public CacheSet(int E) {
        this.E = E;
        this.lines = new LinkedList<>();
    }

    // Find a cache line by tag
    public CacheLine findLine(long tag) {
        for (CacheLine line : lines) {
            if (line.valid && line.tag == tag) return line;
        }
        return null;
    }

    // Insert a new line
    public void insertLine(CacheLine newline) {
        if (lines.size() >= E) {
            lines.removeFirst();
        }
        lines.addLast(newline);
    }

    public LinkedList<CacheLine> getLines() {
        return lines;
    }


}

// Read or write RAM.dat contents
class Memory {
    private final Map<Long, String> memory;

    public Memory(String filename) throws IOException {
        memory = new HashMap<>();
        try (DataInputStream dis = new DataInputStream(new FileInputStream(filename))) {
            long address = 0;
            while (dis.available() >= 8) {
                long value = Long.reverseBytes(dis.readLong());
                memory.put(address, String.format("%016x", value));
                address += 8;
            }
        }
    }

    public String read(long address, int size) {
        long base = address & ~7L;
        return memory.getOrDefault(base, "0000000000000000");
    }

    public void write(long address, int size) {
        long base = address & ~7L;
        String oldData = memory.getOrDefault(base, "0000000000000000");
        String newData = extractWrite(oldData, address, size);
        memory.put(base, newData);
    }

    public String extractWrite(String originalData, long address, int size) {
        int offset = (int)(address % 8) * 2;
        StringBuilder sb = new StringBuilder(originalData);
        for (int i = 0; i < size * 2 && offset + i < sb.length(); i++) {
            sb.setCharAt(offset + i, (i % 2 == 0) ? 'a' : 'b');
        }
        return sb.toString();
    }

    // Write results to txt file
    public static void writeCacheToFile(String filename, Map<Integer, CacheSet> sets) throws IOException {
        try (PrintWriter pw = new PrintWriter(filename)) {
            for (Map.Entry<Integer, CacheSet> entry : sets.entrySet()) {
                int setIndex = entry.getKey();
                for (CacheLine line : entry.getValue().getLines()) {
                    if (line.valid) {
                        pw.printf("Set %d: Tag=%07x Time=%d Valid=%b Data=%s\n",
                                setIndex, line.tag, line.time, line.valid, line.data);
                    }
                }
            }
        }
    }
}

// Read trace file line by line and execute operations
class TraceParser {
    private final Cache l1D, l1I, l2;
    private final Memory ram;

    public TraceParser(Cache l1D, Cache l1I, Cache l2, Memory ram) {
        this.l1D = l1D;
        this.l1I = l1I;
        this.l2 = l2;
        this.ram = ram;
    }

    // Parse trace file
    public void parse(String traceFile) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(traceFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty()) continue;
                String[] tokens = line.split("[ ,]+");
                String type = tokens[0];
                long address = Long.decode("0x" + tokens[1]);
                int size = Integer.parseInt(tokens[2], 10);
                boolean isModify = type.equals("M");

                System.out.println(line);
                StringBuilder result = new StringBuilder();

                if (type.equals("I")) {
                    int prevL1Hits = l1I.hits;
                    int prevL2Hits = l2.hits;

                    l1I.access(address, size, false, false, l2, ram);
                    boolean l1Hit = l1I.hits > prevL1Hits;
                    boolean l2Hit = l2.hits > prevL2Hits;

                    result.append("  L1I ").append(l1Hit ? "hit" : "miss").append(", ").append(l2Hit ? "L2 hit" : "L2 miss");
                    if (!l1Hit && !l2Hit) {
                        result.append("\n  Place in L2 set 0, L1I");
                    }
                } else if (type.equals("L") || type.equals("M") || type.equals("S")) {
                    boolean isStore = type.equals("S") || type.equals("M");

                    int prevL1Hits = l1D.hits;
                    int prevL2Hits = l2.hits;

                    if (isStore) {
                        l2.access(address, size, true, false, null, ram);
                    }

                    l1D.access(address, size, isStore, isModify, l2, ram);
                    boolean l1Hit = l1D.hits > prevL1Hits;
                    boolean l2Hit = l2.hits > prevL2Hits;

                    result.append("  L1D ").append(l1Hit ? "hit" : "miss").append(", ").append(l2Hit ? "L2 hit" : "L2 miss");

                    if (isStore && l1Hit && l2Hit) {
                        result.append("\n  Store in L1D, L2, RAM");
                    } else if (!isStore && !l1Hit && !l2Hit) {
                        result.append("\n  Place in L2 set 0, L1D");
                    }
                }

                System.out.println(result.toString());
            }
        }
    }
}

// Main class
public class Main {
    public static void main(String[] args) throws IOException {
        int l1s = 0, l1e = 2, l1b = 3;
        int l2s = 1, l2e = 2, l2b = 3;
        String traceFile = "";

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-L1s": l1s = Integer.parseInt(args[++i]); break;
                case "-L1E": l1e = Integer.parseInt(args[++i]); break;
                case "-L1b": l1b = Integer.parseInt(args[++i]); break;
                case "-L2s": l2s = Integer.parseInt(args[++i]); break;
                case "-L2E": l2e = Integer.parseInt(args[++i]); break;
                case "-L2b": l2b = Integer.parseInt(args[++i]); break;
                case "-t": traceFile = args[++i]; break;
            }
        }

        Cache l1D = new Cache(l1s, l1e, l1b, "L1D");
        Cache l1I = new Cache(l1s, l1e, l1b, "L1I");
        Cache l2 = new Cache(l2s, l2e, l2b, "L2");

        Memory ram = new Memory("RAM.dat");

        TraceParser parser = new TraceParser(l1D, l1I, l2, ram);
        parser.parse(traceFile);

        System.out.printf("\n\tL1I-hits:%d L1I-misses:%d L1I-evictions:%d\n", l1I.hits, l1I.misses, l1I.evictions);
        System.out.printf("\tL1D-hits:%d L1D-misses:%d L1D-evictions:%d\n", l1D.hits, l1D.misses, l1D.evictions);
        System.out.printf("\tL2-hits:%d L2-misses:%d L2-evictions:%d\n", l2.hits, l2.misses, l2.evictions);

        Memory.writeCacheToFile("L1I.txt", l1I.getAllSets());
        Memory.writeCacheToFile("L1D.txt", l1D.getAllSets());
        Memory.writeCacheToFile("L2.txt", l2.getAllSets());
    }
}

