package components;

public class ReservationStation {
    public String name; // e.g., "ADD1", "MUL2"
    public boolean busy;
    public String op;   // Operation being performed
    public double Vj;   // Value of operand j
    public double Vk;   // Value of operand k
    public String Qj;   // Name of RS producing Vj
    public String Qk;   // Name of RS producing Vk
    public int timeLeft; // Cycles remaining for execution
    public double result; // The calculated result
    
    // NEW: For Load/Store operations
    public int address;        // Computed effective address
    public boolean addressReady; // True when address is computed

    public void listenToCDB(String tag, double value) {
        if (Qj != null && Qj.equals(tag)) {
            Vj = value;
            Qj = null;
        }
        if (Qk != null && Qk.equals(tag)) {
            Vk = value;
            Qk = null;
        }
    }
}