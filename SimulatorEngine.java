import java.util.ArrayList;
import java.util.List;
import Components.*;
/**
 * Minimal SimulatorEngine integration: initializes registers and reservation stations,
 * ticks stations each cycle, arbitrates a single CDB publisher, and updates registers/RSs.
 */
public class SimulatorEngine {
    // Hardware Components
    private MemoryUnit memory;
    private Register[] floatRegs;
    private Register[] intRegs;
    private List<ReservationStation> addStations;
    private List<ReservationStation> mulStations;
    private int currentCycle = 0;

    private CommonDataBus cdb = CommonDataBus.getInstance();

    public SimulatorEngine() {
        // Basic defaults
        this.memory = new MemoryUnit(1024, 16); // 1KB memory, 16-byte blocks

        // Create register files (8 each by default)
        this.floatRegs = new Register[8];
        this.intRegs = new Register[8];
        for (int i = 0; i < 8; i++) {
            this.floatRegs[i] = new Register("F" + i);
            this.intRegs[i] = new Register("R" + i);
        }

        // Create reservation stations: defaults (can be configured later)
        this.addStations = new ArrayList<>();
        this.mulStations = new ArrayList<>();

        // Default sizes: 3 add, 2 mul
        for (int i = 1; i <= 3; i++) addStations.add(new ReservationStation("ADD" + i));
        for (int i = 1; i <= 2; i++) mulStations.add(new ReservationStation("MUL" + i));
    }

    /**
     * Advance the simulation by one cycle. This minimal implementation:
     * - ticks all reservation stations (they decrement timers when operands ready)
     * - finds the first ready result and attempts to publish it on the CDB
     * - if publish succeeds, updates registers and notifies other RSs
     */
    public void nextCycle() {
        currentCycle++;

        // 1. Execute Stage: tick all stations
        for (ReservationStation rs : addStations) rs.tick();
        for (ReservationStation rs : mulStations) rs.tick();

        // 2. Collect ready stations (simple priority: mul then add)
        ReservationStation toPublish = null;
        for (ReservationStation rs : mulStations) {
            if (rs.isResultReady()) { toPublish = rs; break; }
        }
        if (toPublish == null) {
            for (ReservationStation rs : addStations) {
                if (rs.isResultReady()) { toPublish = rs; break; }
            }
        }

        // 3. Publish one result to CDB
        if (toPublish != null) {
            String tag = toPublish.name;
            double value = toPublish.getResult();
            boolean ok = cdb.publish(tag, value);
            if (ok) {
                // Update registers that were waiting for this tag
                for (Register r : floatRegs) {
                    if (tag.equals(r.Qi)) {
                        r.value = value;
                        r.Qi = null;
                    }
                }
                for (Register r : intRegs) {
                    if (tag.equals(r.Qi)) {
                        r.value = value;
                        r.Qi = null;
                    }
                }

                // Notify all other RSs listening to CDB
                for (ReservationStation rs : addStations) if (rs != toPublish) rs.listenToCDB(tag, value);
                for (ReservationStation rs : mulStations) if (rs != toPublish) rs.listenToCDB(tag, value);

                // Commit publish on the publishing station
                toPublish.commitPublish();
            }
        }

        // 4. Clear CDB at end of cycle
        cdb.clear();
    }





/**
     * Issue an instruction to an available reservation station
     * Returns true if successfully issued, false if structural hazard (no free station)
     */
    public boolean issueInstruction(Instruction inst) {
        // Determine which station list to use
        List<ReservationStation> targetStations = null;
        int latency = 1; // Default latency
        
        switch (inst.op) {
            case ADD_D:
            case ADD_S:
            case SUB_D:
            case SUB_S:
                targetStations = addStations;
                latency = 2; // ADD/SUB takes 2 cycles
                break;
                
            case MUL_D:
            case MUL_S:
                targetStations = mulStations;
                latency = 10; // MUL takes 10 cycles
                break;
                
            case DIV_D:
            case DIV_S:
                targetStations = mulStations; // Can use mul stations for div
                latency = 40; // DIV takes 40 cycles
                break;
                
            case DADDI:
            case DSUBI:
                targetStations = addStations; // Integer ops use add stations
                latency = 1; // Integer ops take 1 cycle
                break;
                
            default:
                System.err.println("Unsupported operation for issue: " + inst.op);
                return false;
        }
        
        // Find available station
        ReservationStation freeStation = null;
        for (ReservationStation rs : targetStations) {
            if (!rs.busy) {
                freeStation = rs;
                break;
            }
        }
        
        if (freeStation == null) {
            // Structural hazard - no free station
            return false;
        }
        
        // Get operand values or queue tags (check for RAW hazards)
        double vj = 0.0;
        double vk = 0.0;
        String qj = null;
        String qk = null;
        
        // Check source register j
        Register srcJ = getRegister(inst.j);
        if (srcJ != null) {
            if (srcJ.Qi == null) {
                vj = srcJ.value; // Value ready
            } else {
                qj = srcJ.Qi; // RAW hazard - wait for this RS
            }
        }
        
        // Check source register k (or immediate value)
        if (inst.op == Instruction.OpType.DADDI || inst.op == Instruction.OpType.DSUBI) {
            vk = inst.immediate; // Immediate value
        } else {
            Register srcK = getRegister(inst.k);
            if (srcK != null) {
                if (srcK.Qi == null) {
                    vk = srcK.value; // Value ready
                } else {
                    qk = srcK.Qi; // RAW hazard - wait for this RS
                }
            }
        }
        
        // Issue to reservation station
        freeStation.issue(inst.op.name(), inst.dest, vj, vk, qj, qk, latency);
        
        // Mark destination register as being written by this station
        Register destReg = getRegister(inst.dest);
        if (destReg != null) {
            destReg.Qi = freeStation.name;
        }
        
        System.out.println("Issued " + inst.op + " to " + freeStation.name + 
                         " (Qj=" + qj + ", Qk=" + qk + ")");
        
        return true;
    }
    
    /**
     * Helper method to get a register by name
     */
    public Register getRegister(String name) {
        if (name == null) return null;
        
        if (name.startsWith("F")) {
            int idx = Integer.parseInt(name.substring(1));
            if (idx >= 0 && idx < floatRegs.length) return floatRegs[idx];
        } else if (name.startsWith("R")) {
            int idx = Integer.parseInt(name.substring(1));
            if (idx >= 0 && idx < intRegs.length) return intRegs[idx];
        }
        
        return null;
    }
    
    /**
     * Get current cycle number
     */
    public int getCurrentCycle() {
        return currentCycle;
    }
    
    /**
     * Print status of all reservation stations (for debugging)
     */
    public void printStatus() {
        System.out.println("\n=== Cycle " + currentCycle + " ===");
        System.out.println("Add Stations:");
        for (ReservationStation rs : addStations) {
            if (rs.busy) System.out.println("  " + rs);
        }
        System.out.println("Mul Stations:");
        for (ReservationStation rs : mulStations) {
            if (rs.busy) System.out.println("  " + rs);
        }
    }





}
