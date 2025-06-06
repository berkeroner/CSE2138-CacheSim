
import java.util.*;

class CacheLine {
    long tag;
    long time;
    boolean valid;
    String data;

    public CacheLine(long tag, long time, boolean valid, String data) {
        this.tag = tag;
        this.time = time;
        this.valid = valid;
        this.data = data;
    }
}

public class Cache {
    private int s, E, b;
    private int S;
    private String name;
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