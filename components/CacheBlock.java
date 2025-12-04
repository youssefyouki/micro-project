package components;

/**
 * CacheBlock - Represents a single block in the cache
 * Uses direct mapping with tag, valid bit, and data storage
 */
public class CacheBlock {
    private boolean valid; // Is this block occupied?
    private int tag;       // The tag to identify the address
    private byte[] data;   // The actual block of data

    public CacheBlock(int blockSize) {
        this.valid = false;
        this.tag = -1;
        this.data = new byte[blockSize]; // Size determined by user input
    }

    /**
     * Check if this cache block is valid
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Set the valid bit
     */
    public void setValid(boolean valid) {
        this.valid = valid;
    }

    /**
     * Get the tag of this cache block
     */
    public int getTag() {
        return tag;
    }

    /**
     * Set the tag of this cache block
     */
    public void setTag(int tag) {
        this.tag = tag;
    }

    /**
     * Get the data stored in this cache block
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Set the data for this cache block
     */
    public void setData(byte[] data) {
        if (data.length == this.data.length) {
            System.arraycopy(data, 0, this.data, 0, data.length);
        } else {
            throw new IllegalArgumentException("Data size mismatch. Expected " + this.data.length + " bytes, got " + data.length);
        }
    }

    /**
     * Get the block size
     */
    public int getBlockSize() {
        return data.length;
    }

    /**
     * Invalidate this cache block
     */
    public void invalidate() {
        this.valid = false;
        this.tag = -1;
    }

    @Override
    public String toString() {
        return "CacheBlock{valid=" + valid + ", tag=" + tag + ", blockSize=" + data.length + "}";
    }
}
