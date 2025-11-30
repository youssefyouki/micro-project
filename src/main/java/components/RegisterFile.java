package components;

import java.util.ArrayList;
import java.util.List;

public class RegisterFile {
    private String prefix;  // "F" for float, "R" for int
    private List<Register> registers;

    public RegisterFile(int count, String prefix) {
        this.prefix = prefix;
        this.registers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            registers.add(new Register(prefix + i));
        }
    }

    // Get a register by name
    public Register getRegister(String name) {
        for (Register reg : registers) {
            if (reg.getName().equals(name)) {
                return reg;
            }
        }
        return null;
    }

    // Update all registers waiting for a tag
    public void updateWaitingRegisters(String tag, double result) {
        for (Register reg : registers) {
            reg.listenToCDB(tag, result);
        }
    }

    // Get all registers (for UI)
    public List<Register> getAllRegisters() {
        return registers;
    }
}