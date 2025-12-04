package components;

import components.LoadBuffer.DataSize; // Import the nested enum from LoadBuffer

public class MemoryUnit {
    private static final int MEM_SIZE = 65536; // Example size
    private static final int BLOCK_SIZE = 16;  // Bytes per cache block
    private static final int CACHE_HIT_LATENCY = 1; // Cycles
    private static final int CACHE_MISS_LATENCY = 10; // Cycles (Penalty)

    private byte[] mainMemory;
    private int blockSize;

    // TODO: Member 5 needs to add a proper Cache structure here (e.g., CacheLine[] cache)

    public MemoryUnit(int memSize, int blockSize) {
        this.mainMemory = new byte[memSize];
        this.blockSize = blockSize;
        // Initialize main memory to zero/some default value
        // TODO: Initialize the Cache structure here
    }
    
    // --- Helper Methods to Handle Byte Conversion (Crucial for DataSize) ---
    
    // Helper to convert 8 bytes (double) to a long for storage/retrieval
    private double bytesToDouble(byte[] bytes, int start) {
        long value = 0;
        for (int i = 0; i < 8; i++) {
            value |= ((long) bytes[start + i] & 0xFF) << (8 * i);
        }
        return Double.longBitsToDouble(value);
    }
    
    private void doubleToBytes(double value, byte[] bytes, int start) {
        long l = Double.doubleToRawLongBits(value);
        for (int i = 0; i < 8; i++) {
            bytes[start + i] = (byte) ((l >> (8 * i)) & 0xFF);
        }
    }
    
    // Helper to convert 4 bytes to int (little-endian)
    private int bytesToInt(byte[] bytes, int start) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            value |= ((int) bytes[start + i] & 0xFF) << (8 * i);
        }
        return value;
    }
    
    private void intToBytes(int value, byte[] bytes, int start) {
        for (int i = 0; i < 4; i++) {
            bytes[start + i] = (byte) ((value >> (8 * i)) & 0xFF);
        }
    }
    
    // Helper to convert 4 bytes to float (little-endian)
    private float bytesToFloat(byte[] bytes, int start) {
        int intBits = 0;
        for (int i = 0; i < 4; i++) {
            intBits |= ((int) bytes[start + i] & 0xFF) << (8 * i);
        }
        return Float.intBitsToFloat(intBits);
    }
    
    private void floatToBytes(float value, byte[] bytes, int start) {
        int intBits = Float.floatToIntBits(value);
        for (int i = 0; i < 4; i++) {
            bytes[start + i] = (byte) ((intBits >> (8 * i)) & 0xFF);
        }
    }

    // --- 1. Load Implementation (LW, LD, L.S, L.D) ---
    
    /**
     * Handles Load instruction execution. Checks cache, simulates latency, 
     * and sets the result directly into the calling LoadBuffer.
     * * @param address The calculated memory address.
     * @param size The data size (WORD/DOUBLE/SINGLE).
     * @param callingBuffer The buffer requesting the load (for result write-back).
     * @return The latency in cycles.
     */
    public int load(int address, DataSize size, LoadBuffer callingBuffer) {
        // Step 1: Simulate Cache Access (Simplified for now)
        // TODO: Implement actual cache lookup logic here
        
        int latency = CACHE_MISS_LATENCY; // Default to a miss
        // if (checkCache(address, size) == HIT) latency = CACHE_HIT_LATENCY;

        // Step 2: Read Data from main memory (regardless of cache status for simplicity)
        // In a real simulator, miss penalty includes fetch time.
        
        // This address MUST be checked for bounds before accessing the array!
        if (address < 0 || address >= mainMemory.length) {
            System.err.println("Memory access violation at address: " + address);
            callingBuffer.result = Double.NaN;
            return 1; // Minimal penalty for error
        }
        
        double data = 0.0;
        // The load operation always reads the required bytes and converts them.
        switch (size) {
            case DOUBLE:
                // LD, L.D: Read 8 bytes as double
                if (address + 8 > mainMemory.length) {
                    System.err.println("Memory access violation: not enough bytes for DOUBLE at address: " + address);
                    callingBuffer.result = Double.NaN;
                    return 1;
                }
                data = bytesToDouble(mainMemory, address);
                break;
            case WORD:
                // LW: Read 4 bytes as integer, convert to double for register storage
                if (address + 4 > mainMemory.length) {
                    System.err.println("Memory access violation: not enough bytes for WORD at address: " + address);
                    callingBuffer.result = Double.NaN;
                    return 1;
                }
                data = (double) bytesToInt(mainMemory, address);
                break;
            case SINGLE:
                // L.S: Read 4 bytes as float, convert to double for register storage
                if (address + 4 > mainMemory.length) {
                    System.err.println("Memory access violation: not enough bytes for SINGLE at address: " + address);
                    callingBuffer.result = Double.NaN;
                    return 1;
                }
                data = (double) bytesToFloat(mainMemory, address);
                break;
        }

        // Step 3: Write the result directly back to the calling buffer (The Contract!)
        callingBuffer.result = data;
        
        // Step 4: Return the latency for the SimulatorEngine to count down
        return latency;
    }

    // --- 2. Store Implementation (SW, SD, S.S, S.D) ---

    /**
     * Handles Store instruction execution. Simulates latency and writes to memory.
     * * @param address The calculated memory address.
     * @param value The double value to be stored.
     * @param size The data size (WORD/DOUBLE/SINGLE).
     * @return The latency in cycles.
     */
    public int store(int address, double value, DataSize size) {
        // Step 1: Simulate Cache Access
        // TODO: Implement cache write-through/write-back logic here
        
        int latency = CACHE_MISS_LATENCY; 

        // Step 2: Write Data to main memory
        if (address < 0 || address >= mainMemory.length) {
            System.err.println("Memory access violation at address: " + address);
            return 1;
        }

        // Write the required bytes based on size
        switch (size) {
            case DOUBLE:
                // SD, S.D: Write 8 bytes as double
                if (address + 8 > mainMemory.length) {
                    System.err.println("Memory access violation: not enough space for DOUBLE at address: " + address);
                    return 1;
                }
                doubleToBytes(value, mainMemory, address);
                break;
            case WORD:
                // SW: Write 4 bytes as integer (truncate double to int)
                if (address + 4 > mainMemory.length) {
                    System.err.println("Memory access violation: not enough space for WORD at address: " + address);
                    return 1;
                }
                intToBytes((int) value, mainMemory, address);
                break;
            case SINGLE:
                // S.S: Write 4 bytes as float (cast double to float)
                if (address + 4 > mainMemory.length) {
                    System.err.println("Memory access violation: not enough space for SINGLE at address: " + address);
                    return 1;
                }
                floatToBytes((float) value, mainMemory, address);
                break;
        }

        // Step 3: Return the latency
        return latency;
    }

    // Overload to handle StoreBuffer.DataSize (same as LoadBuffer.DataSize)
    public int store(int address, components.StoreBuffer.DataSize size, double value) {
        // Convert StoreBuffer.DataSize to LoadBuffer.DataSize
        DataSize loadSize;
        switch (size) {
            case WORD:
                loadSize = DataSize.WORD;
                break;
            case SINGLE:
                loadSize = DataSize.SINGLE;
                break;
            case DOUBLE:
                loadSize = DataSize.DOUBLE;
                break;
            default:
                loadSize = DataSize.DOUBLE;
        }
        return store(address, value, loadSize);
    }
}
