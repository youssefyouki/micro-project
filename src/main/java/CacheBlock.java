public class CacheBlock {
    private int tag;
    private boolean valid;
    private int[] data; // We store data as integers/bytes
    private int index;  // Which cache index this is

    public CacheBlock(int index, int blockSize) {
        this.index = index;
        this.valid = false;
        this.tag = -1;
        this.data = new int[blockSize];
    }

    // --- LOGIC ---
    public boolean isValid() { return valid; }
    public int getTag() { return tag; }
    
    public void update(int tag, int[] newData) {
        this.tag = tag;
        this.valid = true;
        this.data = newData;
    }

    // --- GETTERS FOR UI TABLE ---
    public int getIndex() { return index; }
    public String getTagStr() { return valid ? String.valueOf(tag) : "-"; }
    public String getDataStr() {
        if (!valid) return "Empty";
        StringBuilder sb = new StringBuilder("[");
        for (int d : data) sb.append(d).append(",");
        return sb.substring(0, sb.length()-1) + "]";
    }

    public boolean containsAddress(int address) {
        return valid && (address / data.length) == tag;
    }

    public void write(int address, int value) {
        if (valid) {
            int offset = address % data.length;
            if (offset < data.length) {
                data[offset] = value;
            }
        }
    }
}