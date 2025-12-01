public class MemoryUnit {
    private byte[] mainMemory;
    private int blockSize;

    public MemoryUnit(int memSize, int blockSize) {
        this.mainMemory = new byte[memSize];
        this.blockSize = blockSize;
    }

    public double load(int address) {
        return 0.0;
    }

    public void store(int address, double value) {
    }
}