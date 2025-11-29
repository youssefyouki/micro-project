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
        // For Load/Store Buffers, handle address calculation and memory access.
        // Iterate through all Load Buffers
for (LoadBuffer lb : loadBuffers) {
    if (lb.busy) {
        // 1. Address Calculation (Must happen BEFORE execution starts)
        if (lb.Qj == null && !lb.addressReady) {
            // The base register value is ready, calculate the address.
            lb.calculatedAddress = (int)lb.baseRegValue + lb.offset;
            lb.addressReady = true;

            // Start Execution: Call Member 5's MemoryUnit.
            // Assumption: Member 5's load() returns the latency (cycles needed).
            MemoryUnit memory = simulatorEngine.getMemoryUnit(); // assuming a getter exists
            lb.timeLeft = memory.load(lb.calculatedAddress);
        }

        // 2. Execution Countdown
        if (lb.addressReady && lb.timeLeft > 0) {
            lb.timeLeft--;

            // If timer hits 0, the data is ready to be written back next cycle.
            if (lb.timeLeft == 0) {
                lb.valueReady = true;
                // NOTE: The actual result needs to be placed into lb.result here 
                // (Member 5 must provide the data to the buffer when the timer is set).
                // This requires confirming the MemoryUnit contract with Member 5.
            }
        }
    }
}
//store buffer logic 
// Iterate through all Store Buffers
for (StoreBuffer sb : storeBuffers) {
    if (sb.busy) {
        // 1. Check if both dependencies are resolved (address and value)
        if (sb.Qj == null && sb.Qk == null && !sb.addressReady) {
            // Both address components and the value are ready.
            sb.calculatedAddress = (int)sb.baseRegValue + sb.offset;
            sb.addressReady = true;
            sb.valueReady = true;

            // Start Execution: Call Member 5's MemoryUnit.
            MemoryUnit memory = simulatorEngine.getMemoryUnit();
            
            // Assumption: Member 5's store() returns the latency.
            sb.timeLeft = memory.store(sb.calculatedAddress, sb.V_Value);
        }

        // 2. Execution Countdown
        if (sb.addressReady && sb.valueReady && sb.timeLeft > 0) {
            sb.timeLeft--;
            
            // Store finishes when timer hits 0. It is now ready to commit (or release the buffer).
            // NOTE: Stores DO NOT use the CDB. They simply free the buffer and update memory 
            // once all dependencies and execution are finished.
        }
    }
}
        
        // 3. Write Result Stage (Member 6's logic)
        // Publish result to CDB. Update registers and waiting stations.
        
        // 4. Update Tables (Member 1)
    }
}