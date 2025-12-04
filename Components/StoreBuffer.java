 package components;
 
 public class StoreBuffer {




   public String name;           // e.g., "STB1", "STB2"
    public boolean busy;
    public boolean addressReady;   // True once baseRegValue and offset are resolved.
    public boolean valueReady;     // True once the value to be stored is resolved.

    // --- Operand Tracking 1: Address Calculation ---
    
    public double baseRegValue;   // The value of the base register (e.g., R2 in SD F1, 100(R2)).
    public String Qj;             // The tag of the station that will produce the base register value. 
    public int offset;            // The immediate offset (e.g., 100).
    public int calculatedAddress;  // baseRegValue + offset.
    
    // --- Operand Tracking 2: Value to Store ---
    
    public double V_Value;        // The value from the source register (e.g., F1 in SD F1, 100(R2)).
    public String Qk;             // The tag of the station that will produce V_Value.
    
    // --- Execution Tracking ---
    
    public int timeLeft;           // Cycles remaining for Store (Cache/Memory latency).
    public String sourceRegister;   // The register whose value is being stored (e.g., "F1").
public enum DataSize {
    WORD,       // For LW, SW (e.g., 4 bytes)
    SINGLE,     // For L.S, S.S (e.g., 4 bytes, float)
    DOUBLE      // For LD, SD, L.D, S.D (e.g., 8 bytes, double)
}

public DataSize size; 
    // --- Constructor ---

    public StoreBuffer(String name) {
        this.name = name;
        this.busy = false;
        this.addressReady = false;
        this.valueReady = false;
        this.Qj = null;
        this.Qk = null;
        this.timeLeft = 0;
        this.calculatedAddress = 0;
    }

    // --- Core Logic ---

    /**
     * Called every cycle by the SimulatorEngine to broadcast results from the CDB.
     * This allows the Store Buffer to resolve its Qj (Address) and Qk (Value) dependencies.
     */
    public void listenToCDB(String tag, double value) {
        if (!busy) return;

        // 1. Check Address Dependency (Qj)
        if (Qj != null && Qj.equals(tag)) {
            this.baseRegValue = value;
            this.Qj = null; // Address dependency resolved
            this.addressReady = true;
        }

        // 2. Check Value Dependency (Qk)
        if (Qk != null && Qk.equals(tag)) {
            this.V_Value = value;
            this.Qk = null; // Value dependency resolved
            this.valueReady = true;
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
        this.Qk = null;
        this.timeLeft = 0;
        this.calculatedAddress = 0;
        this.baseRegValue = 0.0;
        this.V_Value = 0.0;
        this.offset = 0;
        this.sourceRegister = null;
    }
}
