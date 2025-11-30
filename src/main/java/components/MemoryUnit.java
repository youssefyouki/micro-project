package components;

import java.util.ArrayList;
import java.util.List;

public class MemoryUnit {
    private List<CacheBlock> cache;
    private int cacheSize;
    private int blockSize;

    public MemoryUnit(int cacheSize, int blockSize, int numBlocks) {
        this.cacheSize = cacheSize;
        this.blockSize = blockSize;
        this.cache = new ArrayList<>();
        for (int i = 0; i < numBlocks; i++) {
            cache.add(new CacheBlock(i, blockSize));
        }
    }

    public List<CacheBlock> getCacheData() {
        return cache;
    }

    public CacheBlock read(int address) {
        // Simple implementation
        for (CacheBlock block : cache) {
            if (block.containsAddress(address)) {
                return block;
            }
        }
        return null;
    }

    public void write(int address, int data) {
        for (CacheBlock block : cache) {
            if (block.containsAddress(address)) {
                block.write(address, data);
                return;
            }
        }
    }
}
