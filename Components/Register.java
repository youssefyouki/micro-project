package Components;

public class Register {
    public String name; // e.g., "F0"
    public double value; // Can store int or float value (as double)
    public String Qi; // The name of the RS producing the result (e.g., "ADD1"). Null if valid.
    
    public Register(String name) {
        this.name = name;
        this.Qi = null;
    }
}
