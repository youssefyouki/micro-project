public class Instruction {
    public String op;
    public String rd; // Destination
    public String rs; // Source 1
    public String rt; // Source 2
    public int immediate; 

    public Instruction(String op, String rd, String rs, String rt) {
        this.op = op;
        this.rd = rd;
        this.rs = rs;
        this.rt = rt;
    }
    
    // Getters for the Engine
    public String getOpcode() { return op; }
}