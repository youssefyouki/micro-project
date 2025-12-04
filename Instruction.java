public class Instruction {
    public enum OpType {
        DADDI, DSUBI, ADD_D, ADD_S, SUB_D, SUB_S, MUL_D, MUL_S, DIV_D, DIV_S,
        LW, LD, L_S, L_D, SW, SD, S_S, S_D, BNE, BEQ
    }

    public OpType op;
    public String dest;
    public String j;
    public String k;
    public int immediate;
    
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