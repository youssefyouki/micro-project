package components;

import java.util.Arrays;

/**
 * MemorySystem - Simulates a cache system with byte addressing
 * Uses Direct Mapping: Index = (Address / BlockSize) % NumBlocks
 * Implements Write-Through policy for stores
 */
public class MemorySystem {
    private byte[] mainMemory;
    private CacheBlock[] cache;
    private int blockSize;
    private int cacheSize;
    private int hitLatency;
    private int missPenalty;
    private int numberOfBlocks;

    // Statistics
    private int hits = 0;
    private int misses = 0;

    public MemorySystem(int memSizeInBytes, int cacheSize, int blockSize, int hitLatency, int missPenalty) {
        this.mainMemory = new byte[memSizeInBytes];
        this.cacheSize = cacheSize;
        this.blockSize = blockSize;
        this.hitLatency = hitLatency;
        this.missPenalty = missPenalty;

        // Initialize Cache
        this.numberOfBlocks = cacheSize / blockSize;
        this.cache = new CacheBlock[numberOfBlocks];
        for (int i = 0; i < numberOfBlocks; i++) {
            cache[i] = new CacheBlock(blockSize);
        }
        
        System.out.println("Memory System Initialized:");
        System.out.println("  Main Memory Size: " + memSizeInBytes + " bytes");
        System.out.println("  Cache Size: " + cacheSize + " bytes");
        System.out.println("  Block Size: " + blockSize + " bytes");
        System.out.println("  Number of Cache Blocks: " + numberOfBlocks);
        System.out.println("  Hit Latency: " + hitLatency + " cycles");
        System.out.println("  Miss Penalty: " + missPenalty + " cycles");
    }

    /**
     * Load data from memory with cache support
     * @param address The byte address to load from
     * @param bytesToRead Number of bytes to read (4 for int, 8 for double)
     * @return MemoryResponse containing the data and cycles taken
     */
    public MemoryResponse load(int address, int bytesToRead) {
        // Validate address
        if (address < 0 || address + bytesToRead > mainMemory.length) {
            throw new IllegalArgumentException("Address out of bounds: " + address);
        }

        // Calculate cache mapping using Direct Mapping
        // Block Number = Address / BlockSize
        int blockNumber = address / blockSize;
        
        // Index = BlockNumber % NumberOfBlocks
        int cacheIndex = blockNumber % numberOfBlocks;
        
        // Tag = BlockNumber / NumberOfBlocks
        int tag = blockNumber / numberOfBlocks;
        
        // Offset within block = Address % BlockSize
        int offsetInBlock = address % blockSize;

        System.out.println("\n[LOAD] Address: " + address + ", Bytes: " + bytesToRead);
        System.out.println("  Block Number: " + blockNumber);
        System.out.println("  Cache Index: " + cacheIndex);
        System.out.println("  Tag: " + tag);
        System.out.println("  Offset in Block: " + offsetInBlock);

        // Check for cache hit
        CacheBlock block = cache[cacheIndex];
        if (block.isValid() && block.getTag() == tag) {
            // CACHE HIT
            hits++;
            System.out.println("  Result: CACHE HIT!");
            
            // Calculate how many bytes we can actually read from this block
            int availableBytes = blockSize - offsetInBlock;
            int actualBytesToRead = Math.min(bytesToRead, availableBytes);
            
            // Extract the requested bytes from the cache block
            byte[] data = new byte[bytesToRead];
            System.arraycopy(block.getData(), offsetInBlock, data, 0, actualBytesToRead);
            
            // If we need more bytes than available in this block, read from main memory
            if (actualBytesToRead < bytesToRead) {
                System.out.println("  WARNING: Load spans multiple cache blocks, reading remaining " + 
                    (bytesToRead - actualBytesToRead) + " bytes from main memory");
                System.arraycopy(mainMemory, address + actualBytesToRead, data, actualBytesToRead, 
                    bytesToRead - actualBytesToRead);
            }
            
            return new MemoryResponse(data, hitLatency);
        } else {
            // CACHE MISS
            misses++;
            System.out.println("  Result: CACHE MISS!");
            
            // Calculate the starting address of the entire block in main memory
            int blockStartAddress = blockNumber * blockSize;
            
            // Fetch the entire block from main memory
            byte[] blockData = new byte[blockSize];
            System.arraycopy(mainMemory, blockStartAddress, blockData, 0, blockSize);
            
            // Store the block in the cache
            block.setValid(true);
            block.setTag(tag);
            block.setData(blockData);
            
            System.out.println("  Loaded block from memory [" + blockStartAddress + " to " + (blockStartAddress + blockSize - 1) + "]");
            
            // Calculate how many bytes we can actually read from this block
            int availableBytes = blockSize - offsetInBlock;
            int actualBytesToRead = Math.min(bytesToRead, availableBytes);
            
            // Extract the requested bytes from the newly loaded block
            byte[] data = new byte[bytesToRead];
            System.arraycopy(blockData, offsetInBlock, data, 0, actualBytesToRead);
            
            // If we need more bytes than available in this block, read from main memory
            if (actualBytesToRead < bytesToRead) {
                System.out.println("  WARNING: Load spans multiple cache blocks, reading remaining " + 
                    (bytesToRead - actualBytesToRead) + " bytes from main memory");
                System.arraycopy(mainMemory, address + actualBytesToRead, data, actualBytesToRead, 
                    bytesToRead - actualBytesToRead);
            }
            
            // Total latency = hitLatency + missPenalty
            return new MemoryResponse(data, hitLatency + missPenalty);
        }
    }

    /**
     * Store data to memory using Write-Through policy
     * Updates both cache and main memory
     * @param address The byte address to store to
     * @param data The byte array to store
     */
    public void store(int address, byte[] data) {
        // Validate address
        if (address < 0 || address + data.length > mainMemory.length) {
            throw new IllegalArgumentException("Address out of bounds: " + address);
        }

        System.out.println("\n[STORE] Address: " + address + ", Bytes: " + data.length);

        // Write to main memory (Write-Through)
        System.arraycopy(data, 0, mainMemory, address, data.length);
        
        // Calculate cache mapping
        int blockNumber = address / blockSize;
        int cacheIndex = blockNumber % numberOfBlocks;
        int tag = blockNumber / numberOfBlocks;
        int offsetInBlock = address % blockSize;

        // Update cache if the block is present
        CacheBlock block = cache[cacheIndex];
        if (block.isValid() && block.getTag() == tag) {
            // Calculate how many bytes we can actually write to this block
            int availableBytes = blockSize - offsetInBlock;
            int actualBytesToWrite = Math.min(data.length, availableBytes);
            
            // Update the cache block with what fits
            System.arraycopy(data, 0, block.getData(), offsetInBlock, actualBytesToWrite);
            
            if (actualBytesToWrite < data.length) {
                System.out.println("  Cache partially updated (Write-Through) - " + actualBytesToWrite + " of " + data.length + " bytes");
                System.out.println("  WARNING: Store spans multiple cache blocks");
            } else {
                System.out.println("  Cache updated (Write-Through)");
            }
        } else {
            System.out.println("  Cache not updated (block not present)");
        }
    }

    /**
     * Helper method to store an integer (4 bytes)
     */
    public void storeInt(int address, int value) {
        byte[] data = new byte[4];
        data[0] = (byte) ((value >> 24) & 0xFF);
        data[1] = (byte) ((value >> 16) & 0xFF);
        data[2] = (byte) ((value >> 8) & 0xFF);
        data[3] = (byte) (value & 0xFF);
        store(address, data);
    }

    /**
     * Helper method to store a double (8 bytes)
     */
    public void storeDouble(int address, double value) {
        long longBits = Double.doubleToLongBits(value);
        byte[] data = new byte[8];
        data[0] = (byte) ((longBits >> 56) & 0xFF);
        data[1] = (byte) ((longBits >> 48) & 0xFF);
        data[2] = (byte) ((longBits >> 40) & 0xFF);
        data[3] = (byte) ((longBits >> 32) & 0xFF);
        data[4] = (byte) ((longBits >> 24) & 0xFF);
        data[5] = (byte) ((longBits >> 16) & 0xFF);
        data[6] = (byte) ((longBits >> 8) & 0xFF);
        data[7] = (byte) (longBits & 0xFF);
        store(address, data);
    }

    /**
     * Helper method to view a byte from memory
     */
    public void setMemoryByte(int address, byte val) {
        if (address >= 0 && address < mainMemory.length) {
            mainMemory[address] = val;
        }
    }

    /**
     * Get cache statistics
     */
    public void printStatistics() {
        int totalAccesses = hits + misses;
        double hitRate = totalAccesses > 0 ? (hits * 100.0 / totalAccesses) : 0.0;
        
        System.out.println("\n=== Cache Statistics ===");
        System.out.println("Total Accesses: " + totalAccesses);
        System.out.println("Hits: " + hits);
        System.out.println("Misses: " + misses);
        System.out.println("Hit Rate: " + String.format("%.2f", hitRate) + "%");
    }

    /**
     * Reset statistics
     */
    public void resetStatistics() {
        hits = 0;
        misses = 0;
    }

    /**
     * Invalidate the entire cache (useful for testing)
     */
    public void invalidateCache() {
        for (CacheBlock block : cache) {
            block.invalidate();
        }
        System.out.println("Cache invalidated");
    }
    
    /**
     * Get cache blocks for UI display
     */
    public CacheBlock[] getCacheBlocks() {
        // Set indices for UI display
        for (int i = 0; i < cache.length; i++) {
            cache[i].setIndex(i);
        }
        return cache;
    }
    
    /**
     * Get main memory byte at address
     */
    public byte getMemoryByte(int address) {
        if (address >= 0 && address < mainMemory.length) {
            return mainMemory[address];
        }
        return 0;
    }
    
    /**
     * Store a single byte to main memory (for memory initialization)
     */
    public void storeByte(int address, byte value) {
        if (address >= 0 && address < mainMemory.length) {
            mainMemory[address] = value;
        }
    }
    
    /**
     * Get memory size
     */
    public int getMemorySize() {
        return mainMemory.length;
    }
    
    /**
     * Get cache statistics for UI
     */
    public int getHits() { return hits; }
    public int getMisses() { return misses; }
    public double getHitRate() {
        int total = hits + misses;
        return total > 0 ? (hits * 100.0 / total) : 0.0;
    }
    
    // ===== Adapter Methods for LoadBuffer/StoreBuffer Compatibility =====
    
    /**
     * Load data with DataSize enum support (for LoadBuffer compatibility)
     * Sets result directly in the LoadBuffer and returns latency
     */
    public int load(int address, LoadBuffer.DataSize size, LoadBuffer callingBuffer) {
        int bytesToRead;
        switch (size) {
            case WORD:
            case SINGLE:
                bytesToRead = 4;
                break;
            case DOUBLE:
            default:
                bytesToRead = 8;
                break;
        }
        
        try {
            MemoryResponse response = load(address, bytesToRead);
            
            // Convert bytes to appropriate type and store in buffer
            double result;
            switch (size) {
                case WORD:
                    result = (double) response.toInt();
                    break;
                case SINGLE:
                    result = (double) bytesToFloat(response.getData());
                    break;
                case DOUBLE:
                default:
                    result = response.toDouble();
                    break;
            }
            
            callingBuffer.result = result;
            return response.getCyclesTaken();
            
        } catch (IllegalArgumentException e) {
            System.err.println("Memory access error: " + e.getMessage());
            callingBuffer.result = Double.NaN;
            return 1;
        }
    }
    
    /**
     * Store data with DataSize enum support (for StoreBuffer compatibility)
     */
    public int store(int address, StoreBuffer.DataSize size, double value) {
        byte[] data;
        switch (size) {
            case WORD:
                data = intToBytes((int) value);
                break;
            case SINGLE:
                data = floatToBytes((float) value);
                break;
            case DOUBLE:
            default:
                data = doubleToBytes(value);
                break;
        }
        
        try {
            store(address, data);
            // Store operations take hit latency (write-through)
            return hitLatency;
        } catch (IllegalArgumentException e) {
            System.err.println("Memory access error: " + e.getMessage());
            return 1;
        }
    }
    
    // Helper conversion methods
    private float bytesToFloat(byte[] bytes) {
        if (bytes.length < 4) return 0.0f;
        int intBits = ((bytes[0] & 0xFF) << 24) |
                     ((bytes[1] & 0xFF) << 16) |
                     ((bytes[2] & 0xFF) << 8) |
                     (bytes[3] & 0xFF);
        return Float.intBitsToFloat(intBits);
    }
    
    private byte[] intToBytes(int value) {
        byte[] data = new byte[4];
        data[0] = (byte) ((value >> 24) & 0xFF);
        data[1] = (byte) ((value >> 16) & 0xFF);
        data[2] = (byte) ((value >> 8) & 0xFF);
        data[3] = (byte) (value & 0xFF);
        return data;
    }
    
    private byte[] floatToBytes(float value) {
        int intBits = Float.floatToIntBits(value);
        return intToBytes(intBits);
    }
    
    private byte[] doubleToBytes(double value) {
        long longBits = Double.doubleToLongBits(value);
        byte[] data = new byte[8];
        data[0] = (byte) ((longBits >> 56) & 0xFF);
        data[1] = (byte) ((longBits >> 48) & 0xFF);
        data[2] = (byte) ((longBits >> 40) & 0xFF);
        data[3] = (byte) ((longBits >> 32) & 0xFF);
        data[4] = (byte) ((longBits >> 24) & 0xFF);
        data[5] = (byte) ((longBits >> 16) & 0xFF);
        data[6] = (byte) ((longBits >> 8) & 0xFF);
        data[7] = (byte) (longBits & 0xFF);
        return data;
    }
}
