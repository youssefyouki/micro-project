package components;

import simulation.CommonDataBus;

class Register {
    public String name; // e.g., "F0" or "R1"
    public double value; // Can store int or float value (as double)
    public String Qi; // The name of the RS producing the result (e.g., "ADD1"). Null if valid.
    
    public Register(String name) {
        this.name = name;
        this.Qi = null;
        this.value = 0.0;
    }
}

// Manager for registers (int and float) and listener for the Common Data Bus
public class RegisterFile implements CommonDataBus.Listener {
    private final Register[] floatRegs;
    private final Register[] intRegs;

    public RegisterFile(int intCount, int floatCount) {
        this.intRegs = new Register[intCount];
        this.floatRegs = new Register[floatCount];
        for (int i = 0; i < intCount; i++) {
            this.intRegs[i] = new Register("R" + i);
        }
        for (int i = 0; i < floatCount; i++) {
            this.floatRegs[i] = new Register("F" + i);
        }
        // Register to CDB to receive broadcasts
        try {
            CommonDataBus.getInstance().registerListener(this);
        } catch (Exception e) {
            // If CDB not yet initialized or on errors, swallow to avoid startup crashes
        }
    }

    public Register getIntRegister(int idx) {
        return intRegs[idx];
    }

    public Register getFloatRegister(int idx) {
        return floatRegs[idx];
    }

    // Return a Register by its name, e.g., "R3" or "F1". Null if not found.
    public Register getRegisterByName(String name) {
        if (name == null) return null;
        if (name.startsWith("R")) {
            try {
                int idx = Integer.parseInt(name.substring(1));
                if (idx >= 0 && idx < intRegs.length) return intRegs[idx];
            } catch (NumberFormatException ignored) {}
        } else if (name.startsWith("F")) {
            try {
                int idx = Integer.parseInt(name.substring(1));
                if (idx >= 0 && idx < floatRegs.length) return floatRegs[idx];
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    // Set Qi for a register
    public void setQi(String regName, String qiTag) {
        Register r = getRegisterByName(regName);
        if (r != null) r.Qi = qiTag;
    }

    // Set value for a register (does not clear Qi)
    public void setValue(String regName, double value) {
        Register r = getRegisterByName(regName);
        if (r != null) r.value = value;
    }

    // Get value for a register (returns Double.NaN if not found)
    public double getValue(String regName) {
        Register r = getRegisterByName(regName);
        if (r != null) return r.value;
        return Double.NaN;
    }

    // Get Qi for a register (returns null if not set or register not found)
    public String getQi(String regName) {
        Register r = getRegisterByName(regName);
        if (r != null) return r.Qi;
        return null;
    }

    // Listener callback from CommonDataBus
    @Override
    public void onCDBBroadcast(String tag, double value) {
        // If any register has Qi == tag, update value and clear Qi
        for (Register r : intRegs) {
            if (r.Qi != null && r.Qi.equals(tag)) {
                r.value = value;
                r.Qi = null;
            }
        }
        for (Register r : floatRegs) {
            if (r.Qi != null && r.Qi.equals(tag)) {
                r.value = value;
                r.Qi = null;
            }
        }
    }
}