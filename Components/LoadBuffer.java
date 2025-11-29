package components;

public class LoadBuffer {
    public String name;           // e.g., "LDB1", "LDB2"
    public boolean busy;
    public boolean addressReady;   // True once baseRegValue and offset are resolved.
    public boolean valueReady;     // True once the data is returned from MemoryUnit.

    // --- Operand Tracking (For Address Calculation) ---
    
    // The value of the base register (e.g., R2 in LD F1, 100(R2)).
    public double baseRegValue; 
    
    // The tag of the station that will produce the base register value. 
    // Null if baseRegValue is already valid.
    public String Qj;             
    
    public int offset;            // The immediate offset (e.g., 100 in 100(R2)).
    
    // --- Execution Tracking ---
    
    public int calculatedAddress;  // The final address: baseRegValue + offset.
    public int timeLeft;           // Cycles remaining for Load (Cache/Memory latency).
    public double result;         // The final loaded data.
    public String destRegister;    // The register this load is writing to (e.g., "F1").

    // --- Constructor ---

    public loadBuffer(String name) {
        this.name = name;
        this.busy = false;
        this.addressReady = false;
        this.valueReady = false;
        this.Qj = null;
        this.timeLeft = 0;
        this.calculatedAddress = 0;
    }
    public void listenToCDB(String tag, double value) {
        if (!busy) return;

        // If the tag published on the CDB matches the tag we are waiting for (Qj), 
        // we grab the value and clear the dependency.
        if (Qj != null && Qj.equals(tag)) {
            this.baseRegValue = value;
            this.Qj = null; // Dependency resolved!
            // The address is now ready to be calculated next cycle (base + offset)
            // or is ready if it was calculated in the same cycle.
            this.addressReady = true; 
        }
    }
    
    /**
     * Resets the buffer for a new instruction.
     */
    public void clear() {
        this.busy = false;
        this.addressReady = false;
        this.valueReady = false;
        this.Qj = null;
        this.timeLeft = 0;
        this.calculatedAddress = 0;
        this.baseRegValue = 0.0;
        this.offset = 0;
        this.result = 0.0;
        this.destRegister = null;
    }
}