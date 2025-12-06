package simulation;

import components.*;
import java.util.*;

public class SimulatorEngine { 
    // Instruction unit fields
private List<Instruction> program;
private Queue<Instruction> instructionQueue;
private int pc = 0;
private boolean branchStalled = false;

/**
 * Default constructor with standard configuration:
 * - 3 ADD stations, 2 MUL stations
 * - 3 Load buffers, 3 Store buffers
 * - 4KB memory, 256B cache, 16B blocks
 * - 1 cycle hit latency, 10 cycle miss penalty
 */
public SimulatorEngine() {
    this(3, 2, 3, 3, 1024 * 4, 256, 16, 1, 10);
}

/**
 * Parameterized constructor allowing full configuration
 * @param numAddStations Number of ADD/SUB reservation stations
 * @param numMulStations Number of MUL/DIV reservation stations
 * @param numLoadBuffers Number of load buffers
 * @param numStoreBuffers Number of store buffers
 * @param memorySize Total memory size in bytes
 * @param cacheSize Cache size in bytes
 * @param blockSize Block size in bytes
 * @param hitLatency Cache hit latency in cycles
 * @param missPenalty Cache miss penalty in cycles
 */
public SimulatorEngine(int numAddStations, int numMulStations, int numLoadBuffers, 
                       int numStoreBuffers, int memorySize, int cacheSize, 
                       int blockSize, int hitLatency, int missPenalty) {
    // Initialize MemorySystem with user-specified cache parameters
    this.memory = new MemorySystem(memorySize, cacheSize, blockSize, hitLatency, missPenalty);
    
    // Initialize register files
    this.floatRegs = new Register[32];
    this.intRegs = new Register[32];
    for (int i = 0; i < 32; i++) {
        this.floatRegs[i] = new Register("F" + i);
        this.intRegs[i] = new Register("R" + i);
    }

    // Create ADD/SUB reservation stations (user-specified count)
    this.addStations = new ArrayList<>();
    for (int i = 1; i <= numAddStations; i++) {
        ReservationStation rs = new ReservationStation("ADD" + i);
        rs.busy = false;
        this.addStations.add(rs);
    }
    
    // Create MUL/DIV reservation stations (user-specified count)
    this.mulStations = new ArrayList<>();
    for (int i = 1; i <= numMulStations; i++) {
        ReservationStation rs = new ReservationStation("MUL" + i);
        rs.busy = false;
        this.mulStations.add(rs);
    }

    // Create load buffers (user-specified count)
    this.loadBuffers = new ArrayList<>();
    for (int i = 1; i <= numLoadBuffers; i++) {
        LoadBuffer lb = new LoadBuffer("LOAD" + i);
        lb.busy = false;
        this.loadBuffers.add(lb);
    }

    // Create store buffers (user-specified count)
    this.storeBuffers = new ArrayList<>();
    for (int i = 1; i <= numStoreBuffers; i++) {
        StoreBuffer sb = new StoreBuffer("STORE" + i);
        sb.busy = false;
        this.storeBuffers.add(sb);
    }

    // Create branch station (single station)
    this.branchStation = new ReservationStation("BRANCH");
    this.branchStation.busy = false;

    // Initialize default instruction latencies
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

// Initialize memory with a double value at a specific address
public void initializeMemory(int address, double value) {
    // Convert double to 8 bytes (Big-Endian to match MemoryResponse.toDouble())
    long bits = Double.doubleToLongBits(value);
    System.out.println("[INIT] Writing " + value + " to memory address " + address + 
                       " (0x" + String.format("%04X", address) + ")");
    // Store in Big-Endian order (MSB first)
    memory.storeByte(address + 0, (byte) ((bits >> 56) & 0xFF));
    memory.storeByte(address + 1, (byte) ((bits >> 48) & 0xFF));
    memory.storeByte(address + 2, (byte) ((bits >> 40) & 0xFF));
    memory.storeByte(address + 3, (byte) ((bits >> 32) & 0xFF));
    memory.storeByte(address + 4, (byte) ((bits >> 24) & 0xFF));
    memory.storeByte(address + 5, (byte) ((bits >> 16) & 0xFF));
    memory.storeByte(address + 6, (byte) ((bits >> 8) & 0xFF));
    memory.storeByte(address + 7, (byte) (bits & 0xFF));
}

public void nextCycle() {
    currentCycle++;
    System.out.println("\n>>> Starting Cycle " + currentCycle + " <<<");

    // 1. Issue Stage (Member 2's logic)
    if (!branchStalled && !instructionQueue.isEmpty()) {
        Instruction next = instructionQueue.peek();
        System.out.println("  [ISSUE] Attempting to issue: " + next.op);
        boolean issued = tryIssue(next);
        if (issued) {
            instructionQueue.poll();
            System.out.println("  [ISSUE] Successfully issued at cycle " + currentCycle);
        } else {
            System.out.println("  [ISSUE] Stalled - no free station/buffer");
        }
    } else if (branchStalled) {
        System.out.println("  [ISSUE] Branch stalled - no issue this cycle");
    } else {
        System.out.println("  [ISSUE] Instruction queue empty");
    }

    // 2. Execute Stage (Member 3 & 4's logic)
    executeStage();

    // 3. Write Result Stage (Member 6's logic)
    writeBackStage();

    System.out.println(">>> Cycle " + currentCycle + " Complete <<<\n");
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
            LoadBuffer freeLoad = findFreeLoad(loadBuffers);
            if (freeLoad == null) return false;
            allocateLoad(freeLoad, ins);
            return true;
            
        case SW:
        case SD:
        case S_S:
        case S_D:
            // Use store buffers
            StoreBuffer freeStore = findFreeStore(storeBuffers);
            if (freeStore == null) return false;
            allocateStore(freeStore, ins);
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

private LoadBuffer findFreeLoad(List<LoadBuffer> buffers) {
    if (buffers == null) return null;
    for (LoadBuffer lb : buffers) {
        if (!lb.busy) return lb;
    }
    return null;
}

private StoreBuffer findFreeStore(List<StoreBuffer> buffers) {
    if (buffers == null) return null;
    for (StoreBuffer sb : buffers) {
        if (!sb.busy) return sb;
    }
    return null;
}

private void allocateToStation(ReservationStation rs, Instruction ins) {
    rs.busy = true;
    rs.op = ins.op.name();
    rs.instruction = ins; // Link instruction to station
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

private void allocateLoad(LoadBuffer lb, Instruction ins) {
    lb.busy = true;
    lb.instruction = ins; // Link instruction to buffer
    // Determine data size based on instruction opcode
    switch (ins.op) {
        case LW:
            lb.size = LoadBuffer.DataSize.WORD; // 4 bytes (int)
            break;
        case L_S:
            lb.size = LoadBuffer.DataSize.SINGLE; // 4 bytes (float)
            break;
        case LD:
        case L_D:
        default:
            lb.size = LoadBuffer.DataSize.DOUBLE; // 8 bytes (double)
            break;
    }
    lb.timeLeft = 0; // Will be set when address is ready and memory access is initiated
    lb.addressReady = false;
    lb.valueReady = false;
    lb.destRegister = ins.dest;
    lb.offset = ins.immediate;
    
    System.out.println("  [ALLOCATE] " + lb.name + " for " + ins.op + " " + ins.dest + ", offset=" + ins.immediate + " (instruction: " + ins.toString() + ")");
    
    // Set destination register Qi
    if (ins.dest != null) {
        Register destReg = getRegisterByName(ins.dest);
        if (destReg != null) destReg.Qi = lb.name;
    }
    
    // Base register (j)
    if (ins.j != null) {
        Register baseReg = getRegisterByName(ins.j);
        if (baseReg != null) {
            if (baseReg.Qi != null) {
                lb.Qj = baseReg.Qi;
                lb.baseRegValue = 0; // Will be updated via CDB
            } else {
                lb.baseRegValue = baseReg.value;
                lb.Qj = null;
                // Compute address immediately
                lb.calculatedAddress = (int)lb.baseRegValue + lb.offset;
                lb.addressReady = true;
            }
        } else {
            // Base register not found, use offset only
            lb.baseRegValue = 0;
            lb.calculatedAddress = lb.offset;
            lb.addressReady = true;
        }
    } else {
        // No base register specified, address is just the offset
        lb.baseRegValue = 0;
        lb.calculatedAddress = lb.offset;
        lb.addressReady = true;
    }
    
    ins.issueCycle = currentCycle;
}

private void allocateStore(StoreBuffer sb, Instruction ins) {
    sb.busy = true;
    sb.instruction = ins; // Link instruction to buffer
    // Determine data size based on instruction opcode
    switch (ins.op) {
        case SW:
            sb.size = StoreBuffer.DataSize.WORD; // 4 bytes (int)
            break;
        case S_S:
            sb.size = StoreBuffer.DataSize.SINGLE; // 4 bytes (float)
            break;
        case SD:
        case S_D:
        default:
            sb.size = StoreBuffer.DataSize.DOUBLE; // 8 bytes (double)
            break;
    }
    sb.timeLeft = instructionLatencies.getOrDefault("STORE", 2);
    sb.addressReady = false;
    sb.valueReady = false;
    sb.offset = ins.immediate;
    sb.sourceRegister = ins.j;
    
    // Source register (j) - value to store
    if (ins.j != null) {
        Register srcReg = getRegisterByName(ins.j);
        if (srcReg != null) {
            if (srcReg.Qi != null) {
                sb.Qk = srcReg.Qi;
            } else {
                sb.V_Value = srcReg.value;
                sb.Qk = null;
                sb.valueReady = true;
            }
        }
    }
    
    // Base register (k)
    if (ins.k != null) {
        Register baseReg = getRegisterByName(ins.k);
        if (baseReg != null) {
            if (baseReg.Qi != null) {
                sb.Qj = baseReg.Qi;
                sb.baseRegValue = 0; // Will be updated via CDB
            } else {
                sb.baseRegValue = baseReg.value;
                sb.Qj = null;
                // Compute address immediately
                sb.calculatedAddress = (int)sb.baseRegValue + sb.offset;
                sb.addressReady = true;
            }
        } else {
            // Base register not found, use offset only
            sb.baseRegValue = 0;
            sb.calculatedAddress = sb.offset;
            sb.addressReady = true;
        }
    } else {
        // No base register specified, address is just the offset
        sb.baseRegValue = 0;
        sb.calculatedAddress = sb.offset;
        sb.addressReady = true;
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
            if (idx >= 0 && idx < intRegs.length) return intRegs[idx];
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
public List<LoadBuffer> getLoadBuffers() { return loadBuffers; }
public List<StoreBuffer> getStoreBuffers() { return storeBuffers; }

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

// ===== Configuration Methods =====

/**
 * Reconfigure memory system at runtime
 */
public void configureMemorySystem(int memSizeInBytes, int cacheSize, int blockSize, int hitLatency, int missPenalty) {
    this.memory = new MemorySystem(memSizeInBytes, cacheSize, blockSize, hitLatency, missPenalty);
    System.out.println("Memory system reconfigured");
}

public MemorySystem getMemorySystem() {
    return memory;
}

public void printCacheStatistics() {
    memory.printStatistics();
}

public void resetCacheStatistics() {
    memory.resetStatistics();
}

/**
 * Pre-load a floating-point register with a value
 * @param regName Register name (e.g., "F0", "F1")
 * @param value Value to load
 */
public void setFloatRegisterValue(String regName, double value) {
    Register reg = getRegisterByName(regName);
    if (reg != null && regName.startsWith("F")) {
        reg.value = value;
        reg.Qi = null; // Clear any dependency
        System.out.println("Loaded " + regName + " = " + value);
    } else {
        System.err.println("Invalid float register: " + regName);
    }
}

/**
 * Pre-load an integer register with a value
 * @param regName Register name (e.g., "R0", "R1")
 * @param value Value to load
 */
public void setIntRegisterValue(String regName, double value) {
    Register reg = getRegisterByName(regName);
    if (reg != null && regName.startsWith("R")) {
        reg.value = value;
        reg.Qi = null; // Clear any dependency
        System.out.println("Loaded " + regName + " = " + value);
    } else {
        System.err.println("Invalid integer register: " + regName);
    }
}

/**
 * Pre-load multiple registers from a map
 * @param registerValues Map of register names to values
 */
public void loadRegisterValues(Map<String, Double> registerValues) {
    for (Map.Entry<String, Double> entry : registerValues.entrySet()) {
        String regName = entry.getKey().toUpperCase();
        double value = entry.getValue();
        if (regName.startsWith("F")) {
            setFloatRegisterValue(regName, value);
        } else if (regName.startsWith("R")) {
            setIntRegisterValue(regName, value);
        }
    }
}

/**
 * Pre-load memory locations with values
 * @param address Memory address
 * @param value Value to store (as double)
 * @param size Data size (4 or 8 bytes)
 */
public void preloadMemory(int address, double value, int size) {
    if (size == 4) {
        memory.storeInt(address, (int) value);
    } else if (size == 8) {
        memory.storeDouble(address, value);
    }
    System.out.println("Preloaded memory[" + address + "] = " + value + " (" + size + " bytes)");
}

/**
 * Reset the entire simulator to initial state
 */
public void reset() {
    // Clear instruction queue
    instructionQueue.clear();
    program.clear();
    pc = 0;
    currentCycle = 0;
    branchStalled = false;
    
    // Clear all reservation stations
    for (ReservationStation rs : addStations) rs.clear();
    for (ReservationStation rs : mulStations) rs.clear();
    branchStation.clear();
    
    // Clear all buffers
    for (LoadBuffer lb : loadBuffers) lb.clear();
    for (StoreBuffer sb : storeBuffers) sb.clear();
    
    // Clear register dependencies (but keep values)
    for (Register reg : floatRegs) reg.Qi = null;
    for (Register reg : intRegs) reg.Qi = null;
    
    // Reset cache statistics
    memory.resetStatistics();
    
    System.out.println("Simulator reset to initial state");
}

// ===== Status Query Methods for GUI =====

/**
 * Get current instruction queue for display
 */
public List<Instruction> getInstructionQueueAsList() {
    return new ArrayList<>(instructionQueue);
}

/**
 * Get all instructions in the program
 */
public List<Instruction> getProgram() {
    return new ArrayList<>(program);
}

/**
 * Get detailed status of all reservation stations
 */
public Map<String, Object> getReservationStationStatus(ReservationStation rs) {
    Map<String, Object> status = new HashMap<>();
    status.put("name", rs.name);
    status.put("busy", rs.busy);
    status.put("op", rs.op);
    status.put("Vj", rs.Vj);
    status.put("Vk", rs.Vk);
    status.put("Qj", rs.Qj);
    status.put("Qk", rs.Qk);
    status.put("timeLeft", rs.timeLeft);
    return status;
}

/**
 * Get detailed status of a load buffer
 */
public Map<String, Object> getLoadBufferStatus(LoadBuffer lb) {
    Map<String, Object> status = new HashMap<>();
    status.put("name", lb.name);
    status.put("busy", lb.busy);
    status.put("address", lb.calculatedAddress);
    status.put("addressReady", lb.addressReady);
    status.put("valueReady", lb.valueReady);
    status.put("Qj", lb.Qj);
    status.put("timeLeft", lb.timeLeft);
    return status;
}

/**
 * Get detailed status of a store buffer
 */
public Map<String, Object> getStoreBufferStatus(StoreBuffer sb) {
    Map<String, Object> status = new HashMap<>();
    status.put("name", sb.name);
    status.put("busy", sb.busy);
    status.put("address", sb.calculatedAddress);
    status.put("addressReady", sb.addressReady);
    status.put("valueReady", sb.valueReady);
    status.put("Qj", sb.Qj);
    status.put("Qk", sb.Qk);
    status.put("timeLeft", sb.timeLeft);
    return status;
}

/**
 * Get register file status for display
 */
public Map<String, Object> getRegisterStatus(Register reg) {
    Map<String, Object> status = new HashMap<>();
    status.put("name", reg.name);
    status.put("value", reg.value);
    status.put("Qi", reg.Qi);
    return status;
}

/**
 * Get complete simulator state snapshot for GUI display
 */
public Map<String, Object> getCompleteStatus() {
    Map<String, Object> state = new HashMap<>();
    
    state.put("cycle", currentCycle);
    state.put("branchStalled", branchStalled);
    state.put("queueSize", instructionQueue.size());
    
    // Add station statuses
    List<Map<String, Object>> addStats = new ArrayList<>();
    for (ReservationStation rs : addStations) {
        addStats.add(getReservationStationStatus(rs));
    }
    state.put("addStations", addStats);
    
    List<Map<String, Object>> mulStats = new ArrayList<>();
    for (ReservationStation rs : mulStations) {
        mulStats.add(getReservationStationStatus(rs));
    }
    state.put("mulStations", mulStats);
    
    state.put("branchStation", getReservationStationStatus(branchStation));
    
    // Add buffer statuses
    List<Map<String, Object>> loadStats = new ArrayList<>();
    for (LoadBuffer lb : loadBuffers) {
        loadStats.add(getLoadBufferStatus(lb));
    }
    state.put("loadBuffers", loadStats);
    
    List<Map<String, Object>> storeStats = new ArrayList<>();
    for (StoreBuffer sb : storeBuffers) {
        storeStats.add(getStoreBufferStatus(sb));
    }
    state.put("storeBuffers", storeStats);
    
    // Add register file
    List<Map<String, Object>> floatRegStats = new ArrayList<>();
    for (Register reg : floatRegs) {
        floatRegStats.add(getRegisterStatus(reg));
    }
    state.put("floatRegisters", floatRegStats);
    
    List<Map<String, Object>> intRegStats = new ArrayList<>();
    for (Register reg : intRegs) {
        intRegStats.add(getRegisterStatus(reg));
    }
    state.put("intRegisters", intRegStats);
    
    return state;
}

// Hardware Components
private MemorySystem memory;
private Register[] floatRegs;
private Register[] intRegs;
private List<ReservationStation> addStations;
private List<ReservationStation> mulStations;
private List<LoadBuffer> loadBuffers;   // ADD THIS
private List<StoreBuffer> storeBuffers;  // ADD THIS
private ReservationStation branchStation;  // ADD THIS - single branch RS
private int currentCycle = 0;

// ADD THIS: Configurable latencies (default values)
private Map<String, Integer> instructionLatencies;

private void executeStage() {
    // Execute ALU operations (ADD/MUL stations)
    for (ReservationStation rs : addStations) {
        if (rs.busy && rs.Qj == null && rs.Qk == null && rs.timeLeft > 0) {
            // Don't execute on the same cycle as issue
            if (rs.instruction != null && rs.instruction.issueCycle == currentCycle) {
                // Skip execution on issue cycle
                continue;
            }
            
            // Mark execution start on first cycle
            if (rs.instruction != null && rs.instruction.executionStartCycle == -1) {
                rs.instruction.executionStartCycle = currentCycle;
            }
            
            rs.timeLeft--;
            if (rs.timeLeft == 0) {
                // Compute result and mark execution end
                rs.result = computeResult(rs);
                if (rs.instruction != null) {
                    rs.instruction.executionEndCycle = currentCycle;
                }
            }
        }
    }
    
    for (ReservationStation rs : mulStations) {
        if (rs.busy && rs.Qj == null && rs.Qk == null && rs.timeLeft > 0) {
            // Don't execute on the same cycle as issue
            if (rs.instruction != null && rs.instruction.issueCycle == currentCycle) {
                // Skip execution on issue cycle
                continue;
            }
            
            // Mark execution start on first cycle
            if (rs.instruction != null && rs.instruction.executionStartCycle == -1) {
                rs.instruction.executionStartCycle = currentCycle;
            }
            
            rs.timeLeft--;
            if (rs.timeLeft == 0) {
                rs.result = computeResult(rs);
                if (rs.instruction != null) {
                    rs.instruction.executionEndCycle = currentCycle;
                }
            }
        }
    }
    
    // Execute Load operations
    for (LoadBuffer lb : loadBuffers) {
        // Case 1: Address just became ready, initiate memory load (but not in the same cycle as issue)
        if (lb.busy && lb.addressReady && !lb.valueReady && lb.timeLeft == 0) {
            // Check if this is the first cycle after issue (don't start execution on issue cycle)
            if (lb.instruction != null && lb.instruction.issueCycle == currentCycle) {
                // Skip execution on issue cycle, wait for next cycle
                System.out.println("  " + lb.name + " issued this cycle, will start execution next cycle");
            } else {
                // Mark execution start
                if (lb.instruction != null && lb.instruction.executionStartCycle == -1) {
                    lb.instruction.executionStartCycle = currentCycle;
                }
                // Initiate memory load - this sets lb.result and returns the actual latency
                int actualLatency = memory.load(lb.calculatedAddress, lb.size, lb);
                lb.timeLeft = actualLatency;
                System.out.println("  " + lb.name + " initiated load from address " + lb.calculatedAddress + ", latency=" + actualLatency + " cycles");
            }
        }
        // Case 2: Memory access in progress, count down latency
        else if (lb.busy && lb.addressReady && !lb.valueReady && lb.timeLeft > 0) {
            lb.timeLeft--;
            if (lb.timeLeft == 0) {
                // Load completed, value is now ready for write-back
                lb.valueReady = true;
                if (lb.instruction != null) {
                    lb.instruction.executionEndCycle = currentCycle;
                }
                System.out.println("  " + lb.name + " completed load, result=" + lb.result + ", destReg=" + lb.destRegister);
            }
        }
        // Case 3: Address dependencies resolved, compute address
        else if (lb.busy && !lb.addressReady && lb.Qj == null) {
            lb.calculatedAddress = (int)lb.baseRegValue + lb.offset;
            lb.addressReady = true;
            System.out.println("  " + lb.name + " address ready: baseReg=" + lb.baseRegValue + " + offset=" + lb.offset + " = " + lb.calculatedAddress);
        }
    }
    
    // Execute Store operations (similar to loads)
    for (StoreBuffer sb : storeBuffers) {
        if (sb.busy && sb.addressReady && sb.valueReady && sb.timeLeft > 0) {
            // Mark execution start on first cycle
            if (sb.instruction != null && sb.instruction.executionStartCycle == -1) {
                sb.instruction.executionStartCycle = currentCycle;
            }
            
            sb.timeLeft--;
            if (sb.timeLeft == 0) {
                // Store to memory using the store buffer's size
                memory.store(sb.calculatedAddress, sb.size, sb.V_Value);
                
                // Mark execution end (no write result - stores don't use CDB)
                if (sb.instruction != null) {
                    sb.instruction.executionEndCycle = currentCycle;
                }
                
                sb.clear(); // Clear the store buffer
            }
        } else if (sb.busy && !sb.addressReady && sb.Qj == null) {
            // Address dependencies resolved, compute address
            sb.calculatedAddress = (int)sb.baseRegValue + sb.offset;
            sb.addressReady = true;
        } else if (sb.busy && !sb.valueReady && sb.Qk == null) {
            // Value dependency resolved
            sb.valueReady = true;
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
    // CDB Arbitration: Find one ready station/buffer to write back
    // Priority Order: Add Stations -> Mul Stations -> Load Buffers
    // This ensures deterministic behavior when multiple units finish in the same cycle
    String winnerTag = null;
    double winnerValue = 0.0;
    boolean foundWinner = false;
    ReservationStation winnerStation = null;
    LoadBuffer winnerLoadBuffer = null;
    
    // Check ReservationStations (ADD/MUL)
    if (!foundWinner) {
        for (ReservationStation rs : addStations) {
            if (rs.busy && rs.timeLeft == 0) {
                winnerTag = rs.name;
                winnerValue = rs.result;
                winnerStation = rs;
                foundWinner = true;
                if (rs.instruction != null) {
                    rs.instruction.writeResultCycle = currentCycle;
                }
                break;
            }
        }
    }
    
    if (!foundWinner) {
        for (ReservationStation rs : mulStations) {
            if (rs.busy && rs.timeLeft == 0) {
                winnerTag = rs.name;
                winnerValue = rs.result;
                winnerStation = rs;
                foundWinner = true;
                if (rs.instruction != null) {
                    rs.instruction.writeResultCycle = currentCycle;
                }
                break;
            }
        }
    }
    
    /** Check load buffers for completed loads */
    if (!foundWinner) {
        for (LoadBuffer lb : loadBuffers) {
            if (lb.busy && lb.valueReady) {
                winnerTag = lb.name;
                winnerValue = lb.result;
                winnerLoadBuffer = lb;
                foundWinner = true;
                if (lb.instruction != null) {
                    lb.instruction.writeResultCycle = currentCycle;
                }
                break;
            }
        }
    }
    
    // If we have a winner, broadcast on CDB
    if (foundWinner) {
        String tag = winnerTag;
        double value = winnerValue;
        
        // Update all waiting stations
        for (ReservationStation rs : addStations) {
            rs.listenToCDB(tag, value);
        }
        for (ReservationStation rs : mulStations) {
            rs.listenToCDB(tag, value);
        }
        for (LoadBuffer lb : loadBuffers) {
            lb.listenToCDB(tag, value);
        }
        for (StoreBuffer sb : storeBuffers) {
            sb.listenToCDB(tag, value);
        }

        // ADD THIS: Update branch station
        if (branchStation.busy) {
            branchStation.listenToCDB(tag, value);
        }
        
        // Update register file
        System.out.println("[CDB] Broadcasting " + tag + " = " + value);
        for (Register reg : floatRegs) {
            if (reg.Qi != null && tag.equals(reg.Qi)) {
                System.out.println("[CDB] Updating " + reg.name + ": " + reg.value + " -> " + value);
                reg.value = value;
                reg.Qi = null;
            }
        }
        for (Register reg : intRegs) {
            if (reg.Qi != null && tag.equals(reg.Qi)) {
                System.out.println("[CDB] Updating " + reg.name + ": " + reg.value + " -> " + value);
                reg.value = value;
                reg.Qi = null;
            }
        }
        
        // Clear the winning station/buffer AFTER all updates are complete
        if (winnerStation != null) {
            winnerStation.clear();
        }
        if (winnerLoadBuffer != null) {
            winnerLoadBuffer.clear();
        }
    }
}

// Helper method to get register by name (for TestTomasulo compatibility)
public Register getRegister(String name) {
    return getRegisterByName(name);
}

// Legacy method for issuing instructions (for TestTomasulo compatibility)
public boolean issueInstruction(Instruction inst) {
    return tryIssue(inst);
}

// Print status of reservation stations (for TestTomasulo compatibility)
public void printStatus() {
    System.out.println("\n=== Cycle " + currentCycle + " ===");
    System.out.println("Add Stations:");
    for (ReservationStation rs : addStations) {
        if (rs.busy) {
            System.out.printf("  RS[%s]: Busy=%s, Op=%s, Vj=%.2f, Vk=%.2f, Qj=%s, Qk=%s, Time=%d, Ready=%s%n",
                rs.name, rs.busy, rs.op, rs.Vj, rs.Vk,
                (rs.Qj == null ? "-" : rs.Qj),
                (rs.Qk == null ? "-" : rs.Qk),
                rs.timeLeft,
                (rs.timeLeft == 0));
        }
    }
    System.out.println("Mul Stations:");
    for (ReservationStation rs : mulStations) {
        if (rs.busy) {
            System.out.printf("  RS[%s]: Busy=%s, Op=%s, Vj=%.2f, Vk=%.2f, Qj=%s, Qk=%s, Time=%d, Ready=%s%n",
                rs.name, rs.busy, rs.op, rs.Vj, rs.Vk,
                (rs.Qj == null ? "-" : rs.Qj),
                (rs.Qk == null ? "-" : rs.Qk),
                rs.timeLeft,
                (rs.timeLeft == 0));
        }
    }
}

}
