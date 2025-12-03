package simulation;

import components.*; 
import model.*;
 import java.util.*;

public class SimulatorEngine { 
    // Instruction unit fields
private List<Instruction> program;
private Queue<Instruction> instructionQueue;
private int pc = 0;
private boolean branchStalled = false;

public SimulatorEngine() {
    // Basic initializations to support the Instruction Unit
    this.memory = new MemoryUnit(1024 * 4, 16); // 4KB mem, 16B block default
    this.floatRegs = new Register[32];
    this.intRegs = new Register[32];
    for (int i = 0; i < 32; i++) {
        this.floatRegs[i] = new Register("F" + i);
        this.intRegs[i] = new Register("R" + i);
    }

    // Create a few reservation stations (defaults)
    this.addStations = new ArrayList<>();
    this.mulStations = new ArrayList<>();
    // 3 add stations
    for (int i = 1; i <= 3; i++) {
        ReservationStation rs = new ReservationStation();
        rs.name = "ADD" + i;
        rs.busy = false;
        this.addStations.add(rs);
    }
    // 2 mul stations
    for (int i = 1; i <= 2; i++) {
        ReservationStation rs = new ReservationStation();
        rs.name = "MUL" + i;
        rs.busy = false;
        this.mulStations.add(rs);
    }

    // ADD THIS: 3 load buffers
    this.loadBuffers = new ArrayList<>();
    for (int i = 1; i <= 3; i++) {
        ReservationStation lb = new ReservationStation();
        lb.name = "LOAD" + i;
        lb.busy = false;
        this.loadBuffers.add(lb);
    }

    // 3 store buffers
    this.storeBuffers = new ArrayList<>();
    for (int i = 1; i <= 3; i++) {
        ReservationStation sb = new ReservationStation();
        sb.name = "STORE" + i;
        sb.busy = false;
        this.storeBuffers.add(sb);
    }

    // ADD THIS: Branch station
    this.branchStation = new ReservationStation();
    this.branchStation.name = "BRANCH";
    this.branchStation.busy = false;

    // ADD THIS: Initialize default latencies
    this.instructionLatencies = new HashMap<>();
    instructionLatencies.put("ADD_D", 2);
    instructionLatencies.put("ADD_S", 2);
    instructionLatencies.put("SUB_D", 2);
    instructionLatencies.put("SUB_S", 2);
    instructionLatencies.put("MUL_D", 10);
    instructionLatencies.put("MUL_S", 10);
    instructionLatencies.put("DIV_D", 40);
    instructionLatencies.put("DIV_S", 40);
    instructionLatencies.put("DADDI", 1);
    instructionLatencies.put("DSUBI", 1);
    instructionLatencies.put("LOAD", 2);
    instructionLatencies.put("STORE", 2);
    instructionLatencies.put("BRANCH", 1);

    this.program = new ArrayList<>();
    this.instructionQueue = new LinkedList<>();
}

// Load a program (assembly text) into the engine's instruction queue
public void loadProgram(String asmText) {
    this.program = Instruction.parseProgram(asmText);
    this.instructionQueue.clear();
    for (Instruction ins : program) {
        instructionQueue.add(ins);
    }
    this.pc = 0;
    this.branchStalled = false;
}

public void nextCycle() {
    currentCycle++;

    // 1. Issue Stage (Member 2's logic)
    if (!branchStalled && !instructionQueue.isEmpty()) {
        Instruction next = instructionQueue.peek();
        boolean issued = tryIssue(next);
        if (issued) {
            instructionQueue.poll();
        } else {
            // stall: no free reservation station/buffer
        }
    }

    // 2. Execute Stage (Member 3 & 4's logic)
    executeStage();

    // 3. Write Result Stage (Member 6's logic)
    writeBackStage();

    // 4. Update Tables (Member 1)
}

// Attempt to issue an instruction. Return true if issued (removed from queue), false if stall.
private boolean tryIssue(Instruction ins) {
    switch (ins.op) {
        case ADD_D:
        case ADD_S:
        case SUB_D:
        case SUB_S:
        case DADDI:
        case DSUBI:
            // use addStations for these
            ReservationStation freeAdd = findFree(addStations);
            if (freeAdd == null) return false;
            allocateToStation(freeAdd, ins);
            // Mark branchStalled behavior: integer ops don't cause branch stalls
            return true;

        case MUL_D:
        case MUL_S:
        case DIV_D:
        case DIV_S:
            ReservationStation freeMul = findFree(mulStations);
            if (freeMul == null) return false;
            allocateToStation(freeMul, ins);
            return true;

        case LW:
        case LD:
        case L_S:
        case L_D:
            // Use load buffers
            ReservationStation freeLoad = findFree(loadBuffers);
            if (freeLoad == null) return false;
            allocateLoadStore(freeLoad, ins, true);
            return true;
            
        case SW:
        case SD:
        case S_S:
        case S_D:
            // Use store buffers
            ReservationStation freeStore = findFree(storeBuffers);
            if (freeStore == null) return false;
            allocateLoadStore(freeStore, ins, false);
            return true;

        case BNE:
        case BEQ:
            // Use branch station
            if (branchStation.busy) return false; // Branch station occupied
            
            // Allocate branch to station
            branchStation.busy = true;
            branchStation.op = ins.op.name();
            branchStation.timeLeft = 1; // Branch resolves in 1 cycle
            
            // Read source registers (j and k)
            if (ins.j != null) {
                Register regJ = getRegisterByName(ins.j);
                if (regJ != null) {
                    if (regJ.Qi != null) {
                        branchStation.Qj = regJ.Qi;
                    } else {
                        branchStation.Vj = regJ.value;
                        branchStation.Qj = null;
                    }
                }
            }
            
            if (ins.k != null) {
                Register regK = getRegisterByName(ins.k);
                if (regK != null) {
                    if (regK.Qi != null) {
                        branchStation.Qk = regK.Qi;
                    } else {
                        branchStation.Vk = regK.value;
                        branchStation.Qk = null;
                    }
                }
            }
            
            // Store branch target address in result field (reuse)
            branchStation.result = ins.immediate; // Target instruction index
            
            ins.issueCycle = currentCycle;
            branchStalled = true;
            return true;

        default:
            // Unknown op - try to issue with addStations
            ReservationStation any = findFree(addStations);
            if (any == null) return false;
            allocateToStation(any, ins);
            return true;
    }
}

private ReservationStation findFree(List<ReservationStation> stations) {
    if (stations == null) return null;
    for (ReservationStation rs : stations) {
        if (!rs.busy) return rs;
    }
    return null;
}

private void allocateToStation(ReservationStation rs, Instruction ins) {
    rs.busy = true;
    rs.op = ins.op.name();
    // USE CONFIGURABLE LATENCY
    rs.timeLeft = instructionLatencies.getOrDefault(rs.op, 1);
    // Source 1 (j)
    if (ins.j != null) {
        Register regJ = getRegisterByName(ins.j);
        if (regJ != null) {
            if (regJ.Qi != null) {
                rs.Qj = regJ.Qi;
            } else {
                rs.Vj = regJ.value;
                rs.Qj = null;
            }
        } else {
            // immediate or unknown, try parse int
            try {
                rs.Vj = Double.parseDouble(ins.j);
                rs.Qj = null;
            } catch (Exception e) {
                rs.Qj = null;
            }
        }
    }
    // Source 2 (k) or immediate
    if (ins.k != null) {
        // k may be register name or immediate (or, for branch temporarily dest stored)
        Register regK = getRegisterByName(ins.k);
        if (regK != null) {
            if (regK.Qi != null) {
                rs.Qk = regK.Qi;
            } else {
                rs.Vk = regK.value;
                rs.Qk = null;
            }
        } else {
            // try if it is a numeric immediate
            try {
                int imm = Integer.parseInt(ins.k);
                rs.Vk = imm;
                rs.Qk = null;
            } catch (Exception e) {
                // not a numeric register/imm: leave as waiting (could be label), clear Qk
                rs.Qk = null;
            }
        }
    } else {
        // if instruction used immediate field (DADDI etc.)
        if (ins.immediate != 0) {
            rs.Vk = ins.immediate;
            rs.Qk = null;
        }
    }

    // Set destination register Qi to this reservation station name
    if (ins.dest != null) {
        Register destReg = getRegisterByName(ins.dest);
        if (destReg != null) destReg.Qi = rs.name;
    }

    ins.issueCycle = currentCycle;
}

private void allocateLoadStore(ReservationStation rs, Instruction ins, boolean isLoad) {
    rs.busy = true;
    rs.op = ins.op.name();
    // USE CONFIGURABLE LATENCY
    rs.timeLeft = instructionLatencies.getOrDefault("LOAD", 2);
    rs.addressReady = false;
    
    if (isLoad) {
        // Load: dest = MEM[offset + base]
        // Set destination register Qi
        if (ins.dest != null) {
            Register destReg = getRegisterByName(ins.dest);
            if (destReg != null) destReg.Qi = rs.name;
        }
        
        // Base register (j)
        if (ins.j != null) {
            Register baseReg = getRegisterByName(ins.j);
            if (baseReg != null) {
                if (baseReg.Qi != null) {
                    rs.Qj = baseReg.Qi;
                } else {
                    rs.Vj = baseReg.value;
                    rs.Qj = null;
                    // Compute address immediately
                    rs.address = (int)rs.Vj + ins.immediate;
                    rs.addressReady = true;
                }
            }
        }
    } else {
        // Store: MEM[offset + base] = source
        // Source register (j) - value to store
        if (ins.j != null) {
            Register srcReg = getRegisterByName(ins.j);
            if (srcReg != null) {
                if (srcReg.Qi != null) {
                    rs.Qj = srcReg.Qi;
                } else {
                    rs.Vj = srcReg.value;
                    rs.Qj = null;
                }
            }
        }
        
        // Base register (k)
        if (ins.k != null) {
            Register baseReg = getRegisterByName(ins.k);
            if (baseReg != null) {
                if (baseReg.Qi != null) {
                    rs.Qk = baseReg.Qi;
                } else {
                    rs.Vk = baseReg.value;
                    rs.Qk = null;
                    // Compute address immediately
                    rs.address = (int)rs.Vk + ins.immediate;
                    rs.addressReady = true;
                }
            }
        }
    }
    
    ins.issueCycle = currentCycle;
}

private Register getRegisterByName(String name) {
    if (name == null) return null;
    name = name.trim().toUpperCase();
    if (name.length() == 0) return null;
    if (name.charAt(0) == 'F') {
        try {
            int idx = Integer.parseInt(name.substring(1));
            if (idx >= 0 && idx < floatRegs.length) return floatRegs[idx];
        } catch (NumberFormatException e) {
            return null;
        }
    } else if (name.charAt(0) == 'R') {
        try {
            int idx = Integer.parseInt(name.substring(1));
            if (idx >= 0 && idx < intRegs.length) return floatRegs[idx];
        } catch (NumberFormatException e) {
            return null;
        }
    }
    return null;
}

// Expose small getters to allow UI/tests to inspect state
public int getCurrentCycle() { return currentCycle; }
public Queue<Instruction> getInstructionQueue() { return instructionQueue; }
public List<ReservationStation> getAddStations() { return addStations; }
public List<ReservationStation> getMulStations() { return mulStations; }
public Register[] getFloatRegs() { return floatRegs; }
public Register[] getIntRegs() { return intRegs; }
public boolean isBranchStalled() { return branchStalled; }
public List<ReservationStation> getLoadBuffers() { return loadBuffers; }
public List<ReservationStation> getStoreBuffers() { return storeBuffers; }

// ADD THESE: Methods to configure latencies
public void setInstructionLatency(String opType, int cycles) {
    instructionLatencies.put(opType, cycles);
}

public int getInstructionLatency(String opType) {
    return instructionLatencies.getOrDefault(opType, 1);
}

public Map<String, Integer> getAllLatencies() {
    return new HashMap<>(instructionLatencies);
}

// ADD THIS
public ReservationStation getBranchStation() {
    return branchStation;
}

// Hardware Components
private MemoryUnit memory;
private Register[] floatRegs;
private Register[] intRegs;
private List<ReservationStation> addStations;
private List<ReservationStation> mulStations;
private List<ReservationStation> loadBuffers;   // ADD THIS
private List<ReservationStation> storeBuffers;  // ADD THIS
private ReservationStation branchStation;  // ADD THIS - single branch RS
private int currentCycle = 0;

// ADD THIS: Configurable latencies (default values)
private Map<String, Integer> instructionLatencies;

private void executeStage() {
    // Execute ALU operations (ADD/MUL stations)
    for (ReservationStation rs : addStations) {
        if (rs.busy && rs.Qj == null && rs.Qk == null && rs.timeLeft > 0) {
            if (rs.timeLeft == rs.timeLeft) { // First execution cycle
                // Mark execution start
                // Find instruction and set executionStartCycle
            }
            rs.timeLeft--;
            if (rs.timeLeft == 0) {
                // Compute result
                rs.result = computeResult(rs);
            }
        }
    }
    
    for (ReservationStation rs : mulStations) {
        if (rs.busy && rs.Qj == null && rs.Qk == null && rs.timeLeft > 0) {
            rs.timeLeft--;
            if (rs.timeLeft == 0) {
                rs.result = computeResult(rs);
            }
        }
    }
    
    // Execute Load operations
    for (ReservationStation lb : loadBuffers) {
        if (lb.busy && lb.addressReady && lb.timeLeft > 0) {
            lb.timeLeft--;
            if (lb.timeLeft == 0) {
                // Load from memory
                lb.result = memory.load(lb.address);
            }
        } else if (lb.busy && !lb.addressReady && lb.Qj == null) {
            // Address dependencies resolved, compute address
            lb.address = (int)lb.Vj; // simplified
            lb.addressReady = true;
        }
    }
    
    // Execute Store operations (similar to loads)
    for (ReservationStation sb : storeBuffers) {
        if (sb.busy && sb.addressReady && sb.Qj == null && sb.timeLeft > 0) {
            sb.timeLeft--;
            if (sb.timeLeft == 0) {
                memory.store(sb.address, sb.Vj);
                sb.busy = false; // Store completes without CDB
            }
        } else if (sb.busy && !sb.addressReady && sb.Qk == null) {
            sb.address = (int)sb.Vk;
            sb.addressReady = true;
        }
    }

    // ADD THIS: Execute Branch
    if (branchStation.busy && branchStation.Qj == null && branchStation.Qk == null && branchStation.timeLeft > 0) {
        branchStation.timeLeft--;
        if (branchStation.timeLeft == 0) {
            // Evaluate branch condition
            boolean taken = false;
            if (branchStation.op.equals("BEQ")) {
                taken = (branchStation.Vj == branchStation.Vk);
            } else if (branchStation.op.equals("BNE")) {
                taken = (branchStation.Vj != branchStation.Vk);
            }
            
            if (taken) {
                // Branch taken: flush instruction queue and jump to target
                int targetIdx = (int) branchStation.result;
                instructionQueue.clear();
                
                // Reload instructions from target address onward
                if (targetIdx >= 0 && targetIdx < program.size()) {
                    for (int i = targetIdx; i < program.size(); i++) {
                        instructionQueue.add(program.get(i));
                    }
                    pc = targetIdx;
                }
            }
            // If not taken, continue with next instruction (already in queue)
            
            // Clear branch stall and free station
            branchStalled = false;
            branchStation.busy = false;
        }
    }
}

private double computeResult(ReservationStation rs) {
    switch (rs.op) {
        case "ADD_D": case "ADD_S": return rs.Vj + rs.Vk;
        case "SUB_D": case "SUB_S": return rs.Vj - rs.Vk;
        case "MUL_D": case "MUL_S": return rs.Vj * rs.Vk;
        case "DIV_D": case "DIV_S": return rs.Vj / rs.Vk;
        case "DADDI": return rs.Vj + rs.Vk;
        case "DSUBI": return rs.Vj - rs.Vk;
        default: return 0.0;
    }
}

private void writeBackStage() {
    // Collect all stations ready to write back
    List<ReservationStation> readyStations = new ArrayList<>();
    
    for (ReservationStation rs : addStations) {
        if (rs.busy && rs.timeLeft == 0) readyStations.add(rs);
    }
    for (ReservationStation rs : mulStations) {
        if (rs.busy && rs.timeLeft == 0) readyStations.add(rs);
    }
    for (ReservationStation lb : loadBuffers) {
        if (lb.busy && lb.timeLeft == 0) readyStations.add(lb);
    }
    
    // CDB Arbitration: only one can publish per cycle
    if (!readyStations.isEmpty()) {
        ReservationStation winner = readyStations.get(0); // Simple: first one wins
        
        // Broadcast on CDB
        String tag = winner.name;
        double value = winner.result;
        
        // Update all waiting stations
        for (ReservationStation rs : addStations) {
            rs.listenToCDB(tag, value);
        }
        for (ReservationStation rs : mulStations) {
            rs.listenToCDB(tag, value);
        }
        for (ReservationStation lb : loadBuffers) {
            lb.listenToCDB(tag, value);
        }
        for (ReservationStation sb : storeBuffers) {
            sb.listenToCDB(tag, value);
        }

        // ADD THIS: Update branch station
        if (branchStation.busy) {
            branchStation.listenToCDB(tag, value);
        }
        
        // Update register file
        for (Register reg : floatRegs) {
            if (tag.equals(reg.Qi)) {
                reg.value = value;
                reg.Qi = null;
            }
        }
        for (Register reg : intRegs) {
            if (tag.equals(reg.Qi)) {
                reg.value = value;
                reg.Qi = null;
            }
        }
        
        // Free the reservation station
        winner.busy = false;
    }

    // Update branch station dependencies from CDB (if any)
    // This should be in writeBackStage, but adding here for completeness
}

}