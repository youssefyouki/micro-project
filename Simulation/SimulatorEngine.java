package simulation;
import components.*;
import model.*;
import java.util.List;

public class SimulatorEngine {
    // Hardware Components
    private MemoryUnit memory;
    private Register[] floatRegs;
    private Register[] intRegs;
    private List<ReservationStation> addStations;
    private List<ReservationStation> mulStations;
    private int currentCycle = 0;

    public SimulatorEngine() {
        // TODO: Initialize all arrays and stations
    }

    public void nextCycle() {
        currentCycle++;
        
        // 1. Issue Stage (Member 2's logic)
        // Check Instruction Queue -> Find free station -> Move instruction
        
        // 2. Execute Stage (Member 3 & 4's logic)
        // Tick timers on all busy stations. If timer == 0, request CDB.
        
        // 3. Write Result Stage (Member 6's logic)
        // Publish result to CDB. Update registers and waiting stations.
        
        // 4. Update Tables (Member 1)
    }
}