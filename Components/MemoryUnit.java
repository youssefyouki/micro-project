package components;

public class MemoryUnit {
    private byte[] mainMemory; [cite_start]// The "Array of bytes" [cite: 64]
    private int blockSize;
    
    public MemoryUnit(int memSize, int blockSize) {
        this.mainMemory = new byte[memSize];
        this.blockSize = blockSize;
    }

    public double load(int address) {
        // TODO (Member 5): Check Cache -> If Hit return data -> If Miss, fetch block & penalty
        return 0.0;
    }

    public void store(int address, double value) {
        // TODO (Member 5): Write to Cache/Memory
    }
}