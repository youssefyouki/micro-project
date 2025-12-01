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
    // Tick timers on all busy stations. If timer == 0, request CDB.

    // 3. Write Result Stage (Member 6's logic)
    // Publish result to CDB. Update registers and waiting stations.

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
        case SW:
        case SD:
        case S_S:
        case S_D:
            // Load/Store buffers not implemented in skeleton; mark issued immediately (simple behavior)
            ins.issueCycle = currentCycle;
            // For loads/stores set destination reg Qi if load
            if (ins.op == Instruction.OpType.LW || ins.op == Instruction.OpType.LD || ins.op == Instruction.OpType.L_S || ins.op == Instruction.OpType.L_D) {
                if (ins.dest != null) {
                    Register destReg = getRegisterByName(ins.dest);
                    if (destReg != null) destReg.Qi = "LSBUF"; // generic tag until buffer implemented
                }
            }
            // stores would depend on source register; no buffers here
            return true;

        case BNE:
        case BEQ:
            // Stall issuing until branch resolves (no prediction)
            // Mark the branch as issued (so it is removed), but set branchStalled true to prevent issuing following instructions.
            ins.issueCycle = currentCycle;
            branchStalled = true;
            // We don't set a RS here - branch execution/resolve should clear branchStalled later in execute/write stages.
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
    // simple default timer placeholder
    rs.timeLeft = 1;
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
}
// Hardware Components private MemoryUnit memory; private Register[] floatRegs; private Register[] intRegs; private List<ReservationStation> addStations; private List<ReservationStation> mulStations; private int currentCycle = 0;
