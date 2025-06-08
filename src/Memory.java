
import java.io.*;
import java.util.*;

public class Memory {
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