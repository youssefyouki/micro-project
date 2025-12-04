package Components;

public class ReservationStation {
    public String name; 
    public boolean busy;
    public String op;   
    public double Vj;   
    public double Vk;   
    public String Qj;   
    public String Qk;   
    public int timeLeft; 
    public double result; 
    
    private int originalLatency;
    private boolean resultReady;
    private String destRegister;
    
    public ReservationStation(String name) {
        this.name = name;
        this.busy = false;
        this.resultReady = false;
    }
    
    public void issue(String operation, String dest, double vj, double vk, 
                     String qj, String qk, int latency) {
        this.busy = true;
        this.op = operation;
        this.destRegister = dest;
        this.Vj = vj;
        this.Vk = vk;
        this.Qj = qj;
        this.Qk = qk;
        this.originalLatency = latency;
        this.timeLeft = latency;
        this.resultReady = false;
        this.result = 0.0;
    }
    
    public boolean isReadyToExecute() {
        return busy && Qj == null && Qk == null && !resultReady;
    }
    
    public void tick() {
        if (!busy) return;
        
        if (isReadyToExecute() && timeLeft > 0) {
            timeLeft--;
            
            if (timeLeft == 0) {
                computeResult();
                resultReady = true;
            }
        }
    }
    
    private void computeResult() {
        if (op == null) return;
        
        String opUpper = op.toUpperCase();
        
        switch (opUpper) {
            case "ADD.D":
            case "ADD.S":
            case "FADD":
            case "ADD_D":
            case "ADD_S":
                result = Vj + Vk;
                break;
                
            case "SUB.D":
            case "SUB.S":
            case "FSUB":
            case "SUB_D":
            case "SUB_S":
                result = Vj - Vk;
                break;
                
            case "MUL.D":
            case "MUL.S":
            case "FMUL":
            case "MUL_D":
            case "MUL_S":
                result = Vj * Vk;
                break;
                
            case "DIV.D":
            case "DIV.S":
            case "FDIV":
            case "DIV_D":
            case "DIV_S":
                if (Vk != 0) {
                    result = Vj / Vk;
                } else {
                    result = Double.MAX_VALUE;
                    System.err.println("Warning: Division by zero in " + name);
                }
                break;
                
            case "DADDI":
            case "ADDI":
                result = Vj + Vk;
                break;
                
            case "DSUBI":
            case "SUBI":
                result = Vj - Vk;
                break;
                
            default:
                System.err.println("Unknown operation: " + op);
                result = 0.0;
        }
        
        System.out.println(name + " computed result: " + result + " (op=" + op + ")");
    }
    
    public void listenToCDB(String tag, double value) {
        if (Qj != null && Qj.equals(tag)) {
            Vj = value;
            Qj = null;
            System.out.println(name + " received Vj=" + value + " from " + tag + " (RAW resolved)");
        }
        
        if (Qk != null && Qk.equals(tag)) {
            Vk = value;
            Qk = null;
            System.out.println(name + " received Vk=" + value + " from " + tag + " (RAW resolved)");
        }
    }
    
    public boolean isResultReady() {
        return busy && resultReady && timeLeft == 0;
    }
    
    public double getResult() {
        return result;
    }
    
    public void commitPublish() {
        clear();
    }
    
    public void clear() {
        this.busy = false;
        this.op = null;
        this.Vj = 0.0;
        this.Vk = 0.0;
        this.Qj = null;
        this.Qk = null;
        this.timeLeft = 0;
        this.originalLatency = 0;
        this.result = 0.0;
        this.resultReady = false;
        this.destRegister = null;
    }
    
    public String getDestRegister() {
        return destRegister;
    }
    
    @Override
    public String toString() {
        return String.format("RS[%s]: Busy=%b, Op=%s, Vj=%.2f, Vk=%.2f, Qj=%s, Qk=%s, Time=%d, Ready=%b",
            name, busy, op, Vj, Vk, 
            Qj != null ? Qj : "-", 
            Qk != null ? Qk : "-", 
            timeLeft, resultReady);
    }
}
