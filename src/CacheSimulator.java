
import java.io.*;

public class CacheSimulator {
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

