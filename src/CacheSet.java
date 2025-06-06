
import java.util.*;

public class CacheSet {
    LinkedList<CacheLine> lines;
    int E;

    public CacheSet(int E) {
        this.E = E;
        this.lines = new LinkedList<>();
    }

    public CacheLine findLine(long tag) {
        for (CacheLine line : lines) {
            if (line.valid && line.tag == tag) return line;
        }
        return null;
    }

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

