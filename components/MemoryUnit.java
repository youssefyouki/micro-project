package Components;

import Components.LoadBuffer.DataSize; // Import the nested enum from LoadBuffer

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
    
    // TODO: Add similar helpers for bytesToFloat, floatToBytes, bytesToInt, intToBytes

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
                // Assuming address alignment and size is 8 bytes
                data = bytesToDouble(mainMemory, address);
                break;
            case WORD:
            case SINGLE:
                // TODO: Implement proper 4-byte read/conversion (e.g., integer or float)
                // For simplicity, reading as a double placeholder:
                data = bytesToDouble(mainMemory, address); 
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
                doubleToBytes(value, mainMemory, address);
                break;
            case WORD:
            case SINGLE:
                // TODO: Implement conversion for 4-byte writes (int/float)
                doubleToBytes(value, mainMemory, address); // Using double helper as placeholder
                break;
        }

        // Step 3: Return the latency
        return latency;
    }

    public int store(int calculatedAddress, Components.StoreBuffer.DataSize size, double v_Value) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'store'");
    }
}
