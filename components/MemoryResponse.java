package components;

/**
 * MemoryResponse - Encapsulates the result of a memory operation
 * Contains the data retrieved and the number of cycles taken
 */
public class MemoryResponse {
    private byte[] data;
    private int cyclesTaken;

    public MemoryResponse(byte[] data, int cyclesTaken) {
        this.data = data;
        this.cyclesTaken = cyclesTaken;
    }

    /**
     * Get the raw byte array data
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Get the number of cycles this operation took
     */
    public int getCyclesTaken() {
        return cyclesTaken;
    }

    /**
     * Convert the byte array to an integer (4 bytes, big-endian)
     * Used for LW (Load Word) instructions
     */
    public int toInt() {
        if (data.length < 4) {
            throw new IllegalStateException("Not enough bytes to convert to int (need 4, got " + data.length + ")");
        }
        return ((data[0] & 0xFF) << 24) |
               ((data[1] & 0xFF) << 16) |
               ((data[2] & 0xFF) << 8) |
               (data[3] & 0xFF);
    }

    /**
     * Convert the byte array to a double (8 bytes)
     * Used for L.D (Load Double) instructions
     */
    public double toDouble() {
        if (data.length < 8) {
            throw new IllegalStateException("Not enough bytes to convert to double (need 8, got " + data.length + ")");
        }
        long longBits = ((long)(data[0] & 0xFF) << 56) |
                       ((long)(data[1] & 0xFF) << 48) |
                       ((long)(data[2] & 0xFF) << 40) |
                       ((long)(data[3] & 0xFF) << 32) |
                       ((long)(data[4] & 0xFF) << 24) |
                       ((long)(data[5] & 0xFF) << 16) |
                       ((long)(data[6] & 0xFF) << 8) |
                       ((long)(data[7] & 0xFF));
        return Double.longBitsToDouble(longBits);
    }

    @Override
    public String toString() {
        return "MemoryResponse{cyclesTaken=" + cyclesTaken + ", dataLength=" + data.length + " bytes}";
    }
}
