public class ReservationStation {
    // --- YOUR FIELDS ---
    public String name; 
    public boolean busy;
    public String op;   
    public double Vj;   
    public double Vk;   
    public String Qj;   
    public String Qk;   
    public int timeLeft; 
    public double result;
    public String A; // Needed for Load/Store address (from Project Requirements)
    public String tag; // For CDB tagging
    public boolean resultWritten;

    public ReservationStation(String name, String type) {
        this.name = name;
        this.op = type; // Default type until instruction issued
        this.busy = false;
        this.A = "";
        this.Qj = "";
        this.Qk = "";
        this.tag = name;
        this.result = 0.0;
        this.resultWritten = false;
    }

    // --- REQUIRED FOR JAVA FX UI (The Tables will be empty without these!) ---
    public String getName() { return name; }
    public boolean isBusy() { return busy; }
    public String getOp() { return op; }
    public double getVj() { return Vj; }
    public double getVk() { return Vk; }
    public String getQj() { return Qj != null ? Qj : ""; }
    public String getQk() { return Qk != null ? Qk : ""; }
    public String getA() { return A; }
    public int getTimeLeft() { return timeLeft; }

    // --- REQUIRED FOR SIMULATOR ENGINE (The Controller needs these to drive logic) ---
    
    // Checks if the station is currently working on something
    public boolean isBusyState() { return busy; }

    // Checks if operands are ready (no dependencies waiting)
    public boolean isReadyToExecute() {
        return (Qj == null || Qj.isEmpty()) && (Qk == null || Qk.isEmpty());
    }

    public void decrementTimer() {
        if (timeLeft > 0) timeLeft--;
    }

    public void clear() {
        this.busy = false;
        this.Qj = "";
        this.Qk = "";
        this.A = "";
        this.op = "";
    }

    // This is called by the Engine to put an instruction here
    public void issue(String op, String qj, String qk, double vj, double vk, int latency) {
        this.busy = true;
        this.op = op;
        this.Qj = qj;
        this.Qk = qk;
        this.Vj = vj;
        this.Vk = vk;
        this.timeLeft = latency;
    }
    
    // (Member 3 will fill this logic later, but keep the method here so Engine compiles)
    public void listenToCDB(String tag, double value) {
        if (this.busy) {
            if (this.Qj.equals(tag)) {
                this.Vj = value;
                this.Qj = ""; // Clear dependency
            }
            if (this.Qk.equals(tag)) {
                this.Vk = value;
                this.Qk = ""; // Clear dependency
            }
        }
    }

    // Additional methods needed by SimulatorEngine
    public double getResult() { return result; }
    public String getTag() { return tag; }
    public boolean hasWrittenResult() { return resultWritten; }
    
    public void receiveCDB(String tag, double value) {
        listenToCDB(tag, value);
    }

    public void issue(Instruction instr, RegisterFile floatRegFile, RegisterFile intRegFile, int latency) {
        this.busy = true;
        this.op = instr.getOpcode();
        this.Qj = "";
        this.Qk = "";
        this.Vj = 0;
        this.Vk = 0;
        this.timeLeft = latency;
        this.resultWritten = false;
    }
}