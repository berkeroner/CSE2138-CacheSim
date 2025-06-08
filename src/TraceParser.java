
import java.io.*;

public class TraceParser {
    private final Cache l1D, l1I, l2;
    private final Memory ram;

    public TraceParser(Cache l1D, Cache l1I, Cache l2, Memory ram) {
        this.l1D = l1D;
        this.l1I = l1I;
        this.l2 = l2;
        this.ram = ram;
    }

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
