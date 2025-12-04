import Components.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal SimulatorEngine integration: initializes registers and reservation stations,
 * ticks stations each cycle, arbitrates a single CDB publisher, and updates registers/RSs.
 */
public class SimulatorEngine {
    
    // --- Hardware Components ---
    private MemoryUnit memory;
    private CommonDataBus cdb; // MEMBER 6: Required for Write Result Stage
    
    // MEMBER 6: Register Arrays (Required for Renaming/Issue)
    private Register[] floatRegs = new Register[32]; // Example size
    private Register[] intRegs = new Register[32];   // Example size
    
    // MEMBER 4: Buffers
    private List<LoadBuffer> loadBuffers;
    private List<StoreBuffer> storeBuffers;
    
    // MEMBER 3: Reservation Stations
    private List<ReservationStation> addStations;
    private List<ReservationStation> mulStations;

    // MEMBER 2: Instruction Queue (Required for Issue)
    private List<Instruction> instructionQueue = new ArrayList<>(); 
    
    private int currentCycle = 0;

    public SimulatorEngine() {
        this.memory = new MemoryUnit(65536, 16);
        this.cdb = CommonDataBus.getInstance(); // Initialize CDB singleton

        // Initialize Load/Store Buffers
        this.loadBuffers = new ArrayList<>();
        this.loadBuffers.add(new LoadBuffer("LDB1"));
        this.loadBuffers.add(new LoadBuffer("LDB2"));

        this.storeBuffers = new ArrayList<>();
        this.storeBuffers.add(new StoreBuffer("STB1"));
        this.storeBuffers.add(new StoreBuffer("STB2"));
        
        // Initialize Registers (Placeholder)
        // for (int i = 0; i < 32; i++) { this.floatRegs[i] = new Register("F" + i); }
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
    
    // Helper method required by Issue Stage (must be implemented)
    private Register getRegister(String name, Register[] regArray) {
        // Find and return the Register object based on its name (e.g., "R1" or "F2")
        // This is a crucial link to Member 6's data structure.
        return null; // Placeholder
    }
    
    // Helper methods required by Issue Stage (must be implemented)
    private void issueLoadInstruction(Instruction inst, LoadBuffer lb) { 
        lb.busy = true;
    lb.offset = inst.immediate; // 100
    lb.destRegister = inst.dest; // F1
    // lb.size = ... set based on OpType (LW, LD, L.S, L.D) ...
    // lb.instruction = inst; // Link instruction object

    // 2. Rename the Base Register (R2, which is inst.j) (Member 6 Logic)
    // Loads use an Integer Register (R2) for the address.
    Register baseReg = getRegister(inst.j, this.intRegs); 
    
    if (baseReg.Qi == null) {
        // Base Register is Ready (Value is available)
        lb.baseRegValue = baseReg.value;
        lb.Qj = null;
    } else {
        // Base Register is Busy (Value is pending from 'ADD1', etc.)
        lb.Qj = baseReg.Qi;
    }
    
    // 3. Rename the Destination Register (F1) (Member 6 Logic)
    // The Load Buffer will produce the result for F1.
    Register destReg = getRegister(inst.dest, this.floatRegs); // F1 is a float/double register
    destReg.Qi = lb.name; // F1 is now waiting on this buffer (e.g., "LDB1")
    
    // 4. Update Instruction Status
    inst.issueCycle = currentCycle;
     }
    
  private void issueStoreInstruction(Instruction inst, StoreBuffer sb) {
    // 1. Set Buffer State and Instruction Info
    sb.busy = true;
    sb.offset = inst.immediate; // 100
    sb.sourceRegister = inst.dest; // F1 (The value register, as per the parser)
    // sb.size = ... set based on OpType (SW, SD, S.S, S.D) ...
    // sb.instruction = inst; // Link instruction object

    // 2. Rename the Base Register (R2, which is inst.j) (Member 6 Logic)
    Register baseReg = getRegister(inst.j, this.intRegs); 
    
    if (baseReg.Qi == null) {
        // Address Register is Ready
        sb.baseRegValue = baseReg.value;
        sb.Qj = null;
        sb.addressReady = true; // Address component is instantly ready
    } else {
        // Address Register is Busy
        sb.Qj = baseReg.Qi;
    }
    
    // 3. Rename the Value Register (F1, which is inst.dest) (Member 6 Logic)
    Register valueReg = getRegister(inst.dest, this.floatRegs); 
    
    if (valueReg.Qi == null) {
        // Value Register is Ready
        sb.V_Value = valueReg.value;
        sb.Qk = null;
        sb.valueReady = true; // Value component is instantly ready
    } else {
        // Value Register is Busy
        sb.Qk = valueReg.Qi;
    }
    
    // 4. Update Instruction Status
    inst.issueCycle = currentCycle;
}
     

    // =========================================================================

    /**
     * Advance the simulation by one cycle. This minimal implementation:
     * - ticks all reservation stations (they decrement timers when operands ready)
     * - finds the first ready result and attempts to publish it on the CDB
     * - if publish succeeds, updates registers and notifies other RSs
     */
    public void nextCycle() {
        // --- 1. WRITE RESULT STAGE (CDB Arbitration & Broadcast) ---
        boolean cdbIsBusy = false;
        
        // Arbitrate Load Buffers (Only Loads use CDB)
        for (LoadBuffer lb : this.loadBuffers) {
            if (lb.busy && lb.valueReady && lb.timeLeft == 0 && !cdbIsBusy) { 
                
                // Attempt to publish to CDB
                boolean published = cdb.publish(lb.name, lb.result); 
                
                if (published) {
                    // Success: Record cycle, clear buffer, and lock the bus.
                    // lb.getInstruction().writeResultCycle = currentCycle;
                    lb.clear(); 
                    cdbIsBusy = true; 
                    // No break here is fine, but in a real system, only one wins the bus.
                }
            }
        }
        
        // TODO: Arbitrate Reservation Stations here if cdbIsBusy is false
        
        // --- 2. EXECUTION STAGE (Load/Store Logic) ---
        
        // A. Load Buffer Execution
        for (LoadBuffer lb : loadBuffers) {
            if (lb.busy) {
                // 1. Address Calculation (Qj == null)
                if (lb.Qj == null && !lb.addressReady) {
                    lb.calculatedAddress = (int)lb.baseRegValue + lb.offset;
                    lb.addressReady = true;
                    boolean clashFound = false;

                    // 2. HAZARD CHECK (Store-Load Forwarding)
                    for (StoreBuffer sb : this.storeBuffers) {
                        if (sb.busy && sb.addressReady && sb.calculatedAddress == lb.calculatedAddress) {
                            if (sb.valueReady) {
                                lb.result = sb.V_Value;
                                lb.valueReady = true; 
                                lb.timeLeft = 0;
                            } else {
                                lb.addressReady = false; // Stall execution
                            }
                            clashFound = true;
                            break; 
                        }
                    }

                    // 3. Send Request to MemoryUnit if NO hazard
                    if (!clashFound) {
                        // CORRECTED CALL: Pass all three required arguments
                        lb.timeLeft = this.memory.load(lb.calculatedAddress, lb.size, lb);
                    }
                }

                // 4. Execution Countdown
                if (lb.addressReady && lb.timeLeft > 0) {
                    lb.timeLeft--;
                    if (lb.timeLeft == 0) {
                        lb.valueReady = true;
                    }
                }
            }
        }
        
        // B. Store Buffer Execution
        for (StoreBuffer sb : storeBuffers) {
            if (sb.busy) {
                // 1. Check if both dependencies are resolved
                if (sb.Qj == null && sb.Qk == null && !sb.addressReady) {
                    sb.calculatedAddress = (int)sb.baseRegValue + sb.offset;
                    sb.addressReady = true;
                    sb.valueReady = true;

                    // CORRECTED CALL: Pass all three required arguments
                    sb.timeLeft = this.memory.store(sb.calculatedAddress, sb.size, sb.V_Value); 
                }

                // 2. Execution Countdown
                if (sb.addressReady && sb.valueReady && sb.timeLeft > 0) {
                    sb.timeLeft--;
                    
                    if (sb.timeLeft == 0) {
                        // Store completes (updates memory/cache, clears buffer)
                        // This usually happens in a separate Commit/Retire stage, 
                        // but for a simple Tomasulo model, clearing the buffer is fine.
                        sb.clear(); // Store is done
                    }
                }
            }
        }
        
        // TODO: Reservation Station Execution logic here
        
        // --- 3. ISSUE STAGE (Load/Store Allocation) ---
        
        if (!instructionQueue.isEmpty()) { 
            Instruction nextInstruction = instructionQueue.get(0);
            boolean issued = false;

            // Check for Load Instructions
            if (nextInstruction.op.toString().startsWith("L")) {
                for (LoadBuffer lb : this.loadBuffers) {
                    if (!lb.busy) {
                        issueLoadInstruction(nextInstruction, lb); // Use helper function
                        instructionQueue.remove(0);
                        issued = true;
                        break;
                    }
                }
            } 
            // Check for Store Instructions
            else if (nextInstruction.op.toString().startsWith("S")) {
                for (StoreBuffer sb : this.storeBuffers) {
                    if (!sb.busy) {
                        issueStoreInstruction(nextInstruction, sb); // Use helper function
                        instructionQueue.remove(0);
                        issued = true;
                        break;
                    }
                }
            }
            
            // TODO: Add logic here to issue ALU instructions (ADD.D, MUL.D, etc.)
        }

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
