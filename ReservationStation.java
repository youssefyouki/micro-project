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

    public void listenToCDB(String tag, double value) {
        // TODO (Member 3): If tag matches Qj or Qk, update Vj/Vk and clear Qj/Qk
    }
}