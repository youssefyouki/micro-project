public class Instruction {
    public enum OpType {
        DADDI, DSUBI, ADD_D, ADD_S, SUB_D, SUB_S, MUL_D, MUL_S, DIV_D, DIV_S,
        LW, LD, L_S, L_D, SW, SD, S_S, S_D, BNE, BEQ
    }

    public OpType op;
    public String dest; // e.g., "F1", "R2"
    public String j;    // Source 1
    public String k;    // Source 2 or Immediate
    public int immediate; // For DADDI, offsets, etc.
    
    // Status tracking for the GUI table
    public int issueCycle;
    public int executionStartCycle;
    public int executionEndCycle;
    public int writeResultCycle;

    public Instruction(OpType op, String dest, String j, String k, int imm) {
        this.op = op;
        this.dest = dest;
        this.j = j;
        this.k = k;
        this.immediate = imm;
    }
}