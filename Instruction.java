package model;

import java.util.*; 
public class Instruction 
{ 
    public enum OpType { DADDI, DSUBI, ADD_D, ADD_S, SUB_D, SUB_S, MUL_D, MUL_S, DIV_D, DIV_S, LW, LD, L_S, L_D, SW, SD, S_S, S_D, BNE, BEQ }
public OpType op;
public String dest; // e.g., "F1", "R2"
public String j;    // Source 1
public String k;    // Source 2 or Immediate/Label
public int immediate; // For DADDI, offsets, branch targets etc.

// Status tracking for the GUI table
public int issueCycle = -1;
public int executionStartCycle = -1;
public int executionEndCycle = -1;
public int writeResultCycle = -1;

public Instruction(OpType op, String dest, String j, String k, int imm) {
    this.op = op;
    this.dest = dest;
    this.j = j;
    this.k = k;
    this.immediate = imm;
}

// Parse a whole program text (multiline) into a list of Instructions
public static List<Instruction> parseProgram(String asmText) {
    List<String> lines = new ArrayList<>();
    for (String rawLine : asmText.split("\\r?\\n")) {
        String line = rawLine.trim();
        if (line.isEmpty()) continue;
        // remove comments after '#', '//' or ';'
        int cidx = line.indexOf('#');
        if (cidx >= 0) line = line.substring(0, cidx).trim();
        cidx = line.indexOf("//");
        if (cidx >= 0) line = line.substring(0, cidx).trim();
        cidx = line.indexOf(";");
        if (cidx >= 0) line = line.substring(0, cidx).trim();
        if (line.isEmpty()) continue;
        lines.add(line);
    }

    // First pass: collect labels and associate to instruction index
    Map<String, Integer> labelToIdx = new HashMap<>();
    List<String> instrLines = new ArrayList<>();
    int idx = 0;
    for (String line : lines) {
        if (line.endsWith(":")) {
            String label = line.substring(0, line.length() - 1).trim();
            labelToIdx.put(label, idx);
        } else if (line.contains(":")) {
            // inline label: LABEL: INSTR ...
            int pos = line.indexOf(":");
            String label = line.substring(0, pos).trim();
            labelToIdx.put(label, idx);
            String remainder = line.substring(pos + 1).trim();
            if (!remainder.isEmpty()) {
                instrLines.add(remainder);
                idx++;
            }
        } else {
            instrLines.add(line);
            idx++;
        }
    }

    // Second pass: parse each instruction line
    List<Instruction> program = new ArrayList<>();
    for (String line : instrLines) {
        Instruction inst = parseLine(line);
        program.add(inst);
    }

    // Third pass: resolve branch labels (if any)
    for (int i = 0; i < program.size(); i++) {
        Instruction inst = program.get(i);
        if ((inst.op == OpType.BNE || inst.op == OpType.BEQ) && inst.k != null) {
            String targ = inst.k;
            // if targ is numeric, keep it; otherwise resolve label
            try {
                inst.immediate = Integer.parseInt(targ);
            } catch (NumberFormatException ex) {
                Integer targetIdx = labelToIdx.get(targ);
                if (targetIdx != null) {
                    inst.immediate = targetIdx;
                } else {
                    // unknown label -> set to -1 (caller must handle)
                    inst.immediate = -1;
                }
            }
        }
    }

    return program;
}

// Parse a single assembly instruction line into an Instruction object
private static Instruction parseLine(String line) {
    String working = line.trim();

    // separate opcode token (first token) from operands
    String[] parts = working.split("\\s+", 2);
    String opToken = parts[0].replaceAll(",", "").trim();
    String operandsPart = (parts.length > 1) ? parts[1].trim() : "";

    // Handle cases like "L. D F6, 0(R2)" -> parts[0] might be "L." and parts[1] begins with "D"
    if (opToken.endsWith(".") && operandsPart.length() > 0) {
        String maybeType = operandsPart.split("\\s+")[0];
        if (maybeType.equalsIgnoreCase("D") || maybeType.equalsIgnoreCase("S")) {
            opToken = opToken.substring(0, opToken.length() - 1) + "." + maybeType;
            // remove that type token from operandsPart
            operandsPart = operandsPart.substring(maybeType.length()).trim();
            if (operandsPart.startsWith(",")) operandsPart = operandsPart.substring(1).trim();
        }
    }

    // normalize opcode: replace '.' with '_' and uppercase
    String opKey = opToken.replace('.', '_').toUpperCase();

    // break operands by commas (allow spaces)
    List<String> ops = new ArrayList<>();
    if (!operandsPart.isEmpty()) {
        String[] rawOps = operandsPart.split(",");
        for (String o : rawOps) {
            String v = o.trim();
            if (!v.isEmpty()) ops.add(v);
        }
    }

    OpType optype = null;
    try {
        optype = OpType.valueOf(opKey);
    } catch (IllegalArgumentException ex) {
        // try some common normalization (e.g., "MULD" -> "MUL_D")
        String alt = opKey.replaceAll("([A-Z]+)([DS])$", "$1_$2");
        try {
            optype = OpType.valueOf(alt);
        } catch (IllegalArgumentException ex2) {
            // fallback: try remove dots and match
            try {
                optype = OpType.valueOf(opKey.replaceAll("\\.", "_"));
            } catch (IllegalArgumentException ex3) {
                // unknown op -> throw runtime to help debugging
                throw new RuntimeException("Unknown opcode: '" + opToken + "' (normalized: " + opKey + ") in line: " + line);
            }
        }
    }

    // Build instruction based on op type signature
    String dest = null, j = null, k = null;
    int imm = 0;

    switch (optype) {
        case DADDI:
        case DSUBI:
            // format: DADDI R1, R1, 24
            if (ops.size() >= 3) {
                dest = ops.get(0);
                j = ops.get(1);
                String immTok = ops.get(2);
                imm = parseIntSafe(immTok);
            }
            break;
        case ADD_D:
        case ADD_S:
        case SUB_D:
        case SUB_S:
        case MUL_D:
        case MUL_S:
        case DIV_D:
        case DIV_S:
            // format: MUL.D F0, F2, F4 or ADD_D F1, F2, F3
            if (ops.size() >= 3) {
                dest = ops.get(0);
                j = ops.get(1);
                k = ops.get(2);
            }
            break;
        case LW:
        case LD:
        case L_S:
        case L_D:
            // format: L.D F6, 0(R2)
            if (ops.size() >= 2) {
                dest = ops.get(0);
                String addr = ops.get(1);
                // parse immediate(base)
                AddressParseResult apr = parseMemoryAddress(addr);
                if (apr != null) {
                    imm = apr.offset;
                    j = apr.baseRegister;
                } else {
                    // maybe direct immediate
                    imm = parseIntSafe(addr);
                }
            }
            break;
        case SW:
        case SD:
        case S_S:
        case S_D:
            // format: S.D F6, 8(R2) or S.D R1, 8(R2)
            if (ops.size() >= 2) {
                j = ops.get(0); // value reg to store
                String addr2 = ops.get(1);
                AddressParseResult apr2 = parseMemoryAddress(addr2);
                if (apr2 != null) {
                    imm = apr2.offset;
                    k = apr2.baseRegister;
                } else {
                    imm = parseIntSafe(addr2);
                }
            }
            break;
        case BNE:
        case BEQ:
            // format: BNE R1, R2, LOOP
            if (ops.size() >= 3) {
                j = ops.get(0);
                k = ops.get(1);
                String tgt = ops.get(2);
                // store label initially into k or as immediate? We'll store label in k and resolve later
                // To follow fields, put label in k and immediate resolved in second pass
                // but here k used for reg2; so we set k to target label via dest of third operand
                // Use kForReg = ops.get(1); and store target label in k field? So use dest null, j reg1, k reg2 and immediate target in immediate
                // We'll set k to ops.get(1) and immediate will be resolved after parseProgram
                k = ops.get(1);
                // temporarily store label in dest place? Instead use the string in a new field: we don't have, so store immediate by parsing numeric else store label string in 'dest'?
                // Better: set dest to null, set k to second register, and use the leftover third operand stored in 'j'??? To keep structure, we'll set dest=null, j=ops.get(0), k=ops.get(1), and set immediate as label index later.
                // Store label in 'dest' temporarily:
                // Instead, reuse the instruction.k field to hold the label token by setting immediate after parseProgram pass. We'll store label text in the dest field 'dest' to preserve reg operands. But to avoid confusion, temporarily set dest = ops.get(2) and keep j/k as regs.
                dest = ops.get(2);
            }
            break;
        default:
            // default best-effort: fill tokens left-to-right
            if (ops.size() >= 1) dest = ops.get(0);
            if (ops.size() >= 2) j = ops.get(1);
            if (ops.size() >= 3) k = ops.get(2);
            break;
    }

    Instruction inst = new Instruction(optype, dest, j, k, imm);
    return inst;
}

private static int parseIntSafe(String tok) {
    tok = tok.trim();
    try {
        if (tok.startsWith("0x") || tok.startsWith("0X")) {
            return Integer.parseInt(tok.substring(2), 16);
        }
        return Integer.parseInt(tok);
    } catch (NumberFormatException ex) {
        // not an int
        return 0;
    }
}

private static class AddressParseResult {
    int offset;
    String baseRegister;
    AddressParseResult(int offset, String base) {
        this.offset = offset;
        this.baseRegister = base;
    }
}

private static AddressParseResult parseMemoryAddress(String addr) {
    // typical forms: 100(R2) or -8(R1)
    addr = addr.trim();
    int open = addr.indexOf('(');
    int close = addr.indexOf(')');
    if (open >= 0 && close > open) {
        String off = addr.substring(0, open).trim();
        String reg = addr.substring(open + 1, close).trim();
        int offv = parseIntSafe(off);
        return new AddressParseResult(offv, reg);
    }
    return null;
}
}