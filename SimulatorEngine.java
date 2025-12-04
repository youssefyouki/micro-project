import Components.*;

import java.util.ArrayList;
import java.util.List;

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
        this.cdb = new CommonDataBus(); // Initialize CDB

        // Initialize Load/Store Buffers
        this.loadBuffers = new ArrayList<>();
        this.loadBuffers.add(new LoadBuffer("LDB1"));
        this.loadBuffers.add(new LoadBuffer("LDB2"));

        this.storeBuffers = new ArrayList<>();
        this.storeBuffers.add(new StoreBuffer("STB1"));
        this.storeBuffers.add(new StoreBuffer("STB2"));
        
        // Initialize Registers (Placeholder)
        // for (int i = 0; i < 32; i++) { this.floatRegs[i] = new Register("F" + i); }
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
    }
}