package components;

public class Register {
    public String name;  // e.g., "F0"
    public double value; // The data
    public String Qi;    // The dependency (RS Name). If null/empty, value is valid.

    public Register(String name) {
        this.name = name;
        this.value = 0.0; // Default value (project says you can pre-load this)
        this.Qi = "";     // Empty string means "Ready"
    }

    // --- GETTERS FOR UI (JavaFX PropertyValueFactory needs these) ---
    public String getName() { return name; }
    public double getValue() { return value; }
    public String getQi() { return (Qi == null) ? "" : Qi; }

    // --- LOGIC FOR SIMULATOR ---
    
    // Called when an instruction is issued to dest register
    public void setBusy(String rsName) {
        this.Qi = rsName;
    }

    // Called when CDB broadcasts a result
    public void listenToCDB(String tag, double result) {
        // If this register is waiting for THIS tag (Qi == tag)
        if (this.Qi != null && this.Qi.equals(tag)) {
            this.value = result; // Update value
            this.Qi = "";        // Clear dependency (Ready)
        }
    }
}
